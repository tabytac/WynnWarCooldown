package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

data class TerritoryTimer(
    val territoryName: String,
    val endTime: Long,
    var soundPlayed: Boolean,
    var commandExecuted: Boolean
)

data class ExpiredTimer(
    val territoryName: String,
    val expiredAt: Long
)

data class CaptureEvent(
    val territoryName: String,
    var captureTime: Long,
    var announced: Boolean = false
)

enum class VisibleTimerType { ACTIVE, EXPIRED, CAPTURE }

data class VisibleTimer(
    val territoryName: String,
    val seconds: Long,
    val type: VisibleTimerType
)

object CooldownTimer {
    private val LOGGER = LoggerFactory.getLogger("WynnWarCooldown")
    private const val GUILD_ATTACK_COMMAND = "/guild attack"
    private const val DEFAULT_CAPTURE_COOLDOWN_SECONDS = 600L // assumed default for capture -> vulnerable
    private const val MAX_GUILD_ATTACK_RETRIES = 20

    // private const val CAPTURE_REMINDER_START_SECONDS = 540  // commented out (Capture HUD disabled)
    // private const val CAPTURE_REMINDER_END_SECONDS = 630  // commented out (Capture HUD disabled)

    // NOTE: capture reminder *announcement* timing is now configurable via
    // ModConfig.captureReminderBeforeSeconds (seconds before the default 600s cooldown).
    // We still keep DEFAULT_CAPTURE_COOLDOWN_SECONDS (600s) and a small removal buffer.

    private val activeTimers = mutableMapOf<String, TerritoryTimer>()
    private val expiredTimers = mutableMapOf<String, ExpiredTimer>()
    private val captureEvents = mutableMapOf<String, CaptureEvent>()
    private var pendingGuildAttackRetryAt: Long? = null
    private var pendingGuildAttackTerritory: String? = null
    private var pendingGuildAttackRetryCount: Int = 0
    private var lastPendingRetryScheduleTime: Long = 0 // Track when retry was scheduled

    fun startCooldown(durationSeconds: Long, territoryName: String) {
        require(durationSeconds > 0) { "Cooldown duration must be positive" }

        val client = MinecraftClient.getInstance()
        if (client.world == null) return

        val endTime = System.currentTimeMillis() + (durationSeconds * 1000)
        val timer = TerritoryTimer(territoryName, endTime, false, false)
        activeTimers[territoryName] = timer

        // If we have a recorded capture event, align its captureTime to the server cooldown start
        captureEvents[territoryName]?.let { event ->
            try {
                event.captureTime = endTime - (durationSeconds * 1000L)
                LOGGER.info("Synchronized capture event for {} to server cooldown start (captureTime={})", territoryName, event.captureTime)
            } catch (e: Exception) {
                LOGGER.debug("Failed to sync capture event for {}: {}", territoryName, e.message)
            }
        }

        LOGGER.info("Cooldown timer started for {}: {}s, ends at {}", territoryName, durationSeconds, endTime)
    }

    /**
     * Called when the local player captures a territory. Records the capture time and (optionally)
     * ensures an estimated cooldown exists so the HUD can display remaining time.
     */
    fun recordCapture(territoryName: String, captureTimeMillis: Long = System.currentTimeMillis()) {
        // Only record the capture event — do NOT create a regular countdown timer.
        // Capture reminders are shown as a COUNT-UP (time since capture) during the configured window.
        captureEvents[territoryName] = CaptureEvent(territoryName, captureTimeMillis, false)

        LOGGER.info("Recorded capture for {} — will announce when {}s or less remain before 10:00", territoryName, ModConfig.captureReminderBeforeSeconds)
    }

    fun getVisibleTimers(): List<VisibleTimer> {
        val now = System.currentTimeMillis()
        val nowSeconds = now / 1000L
        val memoryMs = ModConfig.expiredTimerMemorySeconds * 1000L

        // Clean up old expired timers
        expiredTimers.entries.removeIf { (_, timer) ->
            now - timer.expiredAt > memoryMs
        }

        // Active timers -> remaining seconds
        val activeList = activeTimers.map { (name, timer) ->
            val endSeconds = (timer.endTime + 999L) / 1000L
            val remaining = (endSeconds - nowSeconds).coerceAtLeast(0)
            VisibleTimer(name, remaining, VisibleTimerType.ACTIVE)
        }

        // Expired timers (memory)
        val expiredList = if (ModConfig.expiredTimerMemorySeconds > 0) {
            expiredTimers.map { (name, _) -> VisibleTimer(name, 0L, VisibleTimerType.EXPIRED) }
        } else {
            emptyList()
        }

        // Capture reminder timers (HUD display disabled by request) — keep code commented for easy re-enable.
        /*
        val captureList = if (ModConfig.enableCaptureReminder && ModConfig.captureReminderShowHud) {
            captureEvents.values.mapNotNull { event ->
                val windowStart = event.captureTime + (CAPTURE_REMINDER_START_SECONDS * 1000L)
                val windowEnd = event.captureTime + (CAPTURE_REMINDER_END_SECONDS * 1000L)
                if (now in windowStart..windowEnd) {
                    val elapsed = ((now - event.captureTime) / 1000L).coerceAtLeast(0L)
                    VisibleTimer(event.territoryName, elapsed, VisibleTimerType.CAPTURE)
                } else null
            }
        } else emptyList()
        */  // commented out (Capture HUD disabled)
        val captureList = emptyList<VisibleTimer>() // capture HUD entries are disabled

        // Merge: active + expired first, captures always at the bottom (user request)
        val merged = linkedMapOf<String, VisibleTimer>()
        (activeList + expiredList).forEach { if (!merged.containsKey(it.territoryName)) merged[it.territoryName] = it }
        // finally append captures (do not overwrite existing active/expired entries)
        captureList.forEach { if (!merged.containsKey(it.territoryName)) merged[it.territoryName] = it }

        // Return list preserving order: active/expired first (sorted), captures last (sorted by elapsed)
        val primary = merged.values.filter { it.type != VisibleTimerType.CAPTURE }
            .sortedWith(compareBy<VisibleTimer> { it.seconds == 0L }.thenBy { it.seconds })
        val captures = merged.values.filter { it.type == VisibleTimerType.CAPTURE }
            .sortedBy { it.seconds }

        return primary + captures
    }

    private fun sendGuildAttack(territoryName: String) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: run {
            LOGGER.debug("Cannot send /guild attack: no player client")
            return
        }
        val currentTerritory = TerritoryResolver.getCurrentTerritoryName()
        if (currentTerritory == territoryName) {
            player.networkHandler?.sendChatMessage(GUILD_ATTACK_COMMAND)
            LOGGER.info("Sent /guild attack retry for {}", territoryName)
        } else {
            LOGGER.debug("Skipped /guild attack retry for {}: currently in {}", territoryName, currentTerritory)
        }
    }

    fun scheduleGuildAttackRetry(secondsRemaining: Int, territoryName: String) {
        // Pure millisecond-based scheduler for rapid retries
        val now = System.currentTimeMillis()

        // Dynamic delay based on server's reported remaining seconds
        // Aim to retry just as the cooldown is about to expire
        val delayMs = when {
            secondsRemaining >= 3 -> (secondsRemaining * 1000 - 500).toLong()
            secondsRemaining == 2 -> 1000L // Wait 1s for 2s remaining
            secondsRemaining == 1 -> 100L // Wait 100ms for 1s remaining
            secondsRemaining <= 0 -> 50L // For sub-second or already expired
            else -> return
        }

        // If we're retrying the same territory, just reschedule (don't increment counter)
        // Counter only increments if this is a fresh retry sequence
        if (pendingGuildAttackTerritory != territoryName) {
            pendingGuildAttackRetryCount = 0
            lastPendingRetryScheduleTime = now
        }

        // Check if we've exceeded max retries for this territory
        if (pendingGuildAttackRetryCount >= MAX_GUILD_ATTACK_RETRIES) {
            LOGGER.error(
                "Aborting /guild attack retries for {} after {} attempts; cooldown message persisted",
                territoryName,
                pendingGuildAttackRetryCount
            )
            resetPendingRetry()
            return
        }

        pendingGuildAttackRetryCount += 1
        pendingGuildAttackRetryAt = now + delayMs
        pendingGuildAttackTerritory = territoryName
        LOGGER.info("Scheduling /guild attack retry #{} in {}ms for {} ({}s remaining)", pendingGuildAttackRetryCount, delayMs, territoryName, secondsRemaining)
    }

    private fun resetPendingRetry() {
        pendingGuildAttackRetryAt = null
        pendingGuildAttackTerritory = null
        pendingGuildAttackRetryCount = 0
        lastPendingRetryScheduleTime = 0
    }

    fun updateTimers() {
        if (!ModConfig.isModEnabled) return

        val now = System.currentTimeMillis()
        val timersToRemove = mutableListOf<String>()

        activeTimers.forEach { (territoryName, timer) ->
            val remaining = ((timer.endTime - now) / 1000).coerceAtLeast(0)
            val soundTriggerTime = timer.endTime + (ModConfig.soundPlayOffsetSeconds * 1000L)
            val commandTriggerTime = timer.endTime
            val removalTime = maxOf(timer.endTime, soundTriggerTime, commandTriggerTime)

            // Check sound play offset
            if (!timer.soundPlayed && now >= soundTriggerTime) {
                timer.soundPlayed = true
                SoundManager.playCooldownSound()
            }

            // Check command execution offset
            if (!timer.commandExecuted && now >= commandTriggerTime) {
                timer.commandExecuted = true
                executeCommand(territoryName)
            }

            // Move to expired if finished
            if (now >= removalTime) {
                // Notify player that the territory is off cooldown
                if (ModConfig.announceTimerOffCooldown) {
                    try {
                        val client = MinecraftClient.getInstance()
                        client.inGameHud?.chatHud?.addMessage(
                            Text.translatable("wynn-war-cooldown.chat.timer_off_cooldown", territoryName)
                        )
                    } catch (e: Exception) {
                        LOGGER.debug("Failed to send off-cooldown chat for {}: {}", territoryName, e.message)
                    }
                }

                timersToRemove.add(territoryName)
                if (ModConfig.expiredTimerMemorySeconds > 0) {
                    expiredTimers[territoryName] = ExpiredTimer(territoryName, now)
                }
            }
        }

        timersToRemove.forEach { activeTimers.remove(it) }

        pendingGuildAttackRetryAt?.let { retryAt ->
            if (now >= retryAt) {
                LOGGER.info("Executing pending /guild attack retry #{} for {}", pendingGuildAttackRetryCount, pendingGuildAttackTerritory)
                pendingGuildAttackTerritory?.let { sendGuildAttack(it) }

                // Only keep retrying if we haven't exceeded the timeout
                // If 5 seconds have passed since original schedule with no new cooldown message,
                // assume the attack succeeded
                val timeSinceSchedule = now - lastPendingRetryScheduleTime
                if (timeSinceSchedule < 5000L && pendingGuildAttackTerritory != null) {
                    // Schedule aggressive 50ms retry in case first attempt failed
                    pendingGuildAttackRetryAt = now + 50L
                    LOGGER.debug("Rescheduling aggressive retry in 50ms ({}ms since initial schedule)", timeSinceSchedule)
                } else {
                    // Either 5 seconds passed or no territory set; clear the retry
                    if (timeSinceSchedule >= 5000L) {
                        LOGGER.info("Clearing /guild attack retry for {} (5s timeout, assuming success)", pendingGuildAttackTerritory)
                    }
                    resetPendingRetry()
                }
            }
        }

        // --- Capture reminder processing (announcement only; HUD disabled) ---
        if (ModConfig.enableCaptureReminder) {
            val captureToRemove = mutableListOf<String>()

            captureEvents.values.forEach { event ->
                val elapsed = (now - event.captureTime) / 1000L

                // Remaining seconds until the assumed default vulnerable time (600s)
                val remaining = (DEFAULT_CAPTURE_COOLDOWN_SECONDS - elapsed).coerceAtLeast(0L)

                // Trigger one-time announcement when remaining <= configured lead time
                if (!event.announced && remaining <= ModConfig.captureReminderBeforeSeconds) {
                    event.announced = true

                    if (ModConfig.captureReminderPlaySound) SoundManager.playCooldownSound()

                    if (ModConfig.captureReminderAnnounceChat) {
                        val client = MinecraftClient.getInstance()
                        client.inGameHud?.chatHud?.addMessage(
                            Text.translatable("wynn-war-cooldown.chat.capture_reminder", event.territoryName, formatTime(remaining))
                        )
                    }
                }

                // remove capture event after a small buffer past the default cooldown (legacy behavior: ~10m30s)
                if (elapsed > DEFAULT_CAPTURE_COOLDOWN_SECONDS + 30L) {
                    captureToRemove.add(event.territoryName)
                }
            }

            captureToRemove.forEach { captureEvents.remove(it) }
        }
    }

    fun hasActiveTimers(): Boolean = activeTimers.isNotEmpty()

    fun getActiveTerritories(): List<String> {
        return activeTimers.keys.sorted()
    }

    fun removeCooldown(territoryName: String): Boolean {
        val removedFromActive = activeTimers.remove(territoryName) != null
        val removedFromExpired = expiredTimers.remove(territoryName) != null
        val removedFromCapture = captureEvents.remove(territoryName) != null
        val removed = removedFromActive || removedFromExpired || removedFromCapture

        if (removed) {
            LOGGER.info("Removed cooldown/capture for: {}", territoryName)
        }
        return removed
    }

    fun clearAllTimers() {
        activeTimers.clear()
        expiredTimers.clear()
        captureEvents.clear()
        LOGGER.info("Cleared all timers and capture events")
    }

    fun clearExpiredTimers() {
        expiredTimers.clear()
        LOGGER.info("Cleared expired timers from memory")
    }

    fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    private fun executeCommand(territoryName: String) {
        if (!ModConfig.sendGuildAttackAtEnd) return

        val currentTerritory = TerritoryResolver.getCurrentTerritoryName()

        if (currentTerritory == null) {
            LOGGER.info("Player is not currently in any territory, skipping command execution for {}", territoryName)
            return
        }

        if (currentTerritory != territoryName) {
            LOGGER.info("Skipping command for {} - player not in territory (currently in: {})",
                territoryName, currentTerritory)
            return
        }

        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val actualCommand = GUILD_ATTACK_COMMAND

        LOGGER.info("Player is currently in territory: {}", currentTerritory)
        LOGGER.info("Executing command for {}: {}", territoryName, actualCommand)
        player.networkHandler?.sendChatMessage(actualCommand)
    }
}
