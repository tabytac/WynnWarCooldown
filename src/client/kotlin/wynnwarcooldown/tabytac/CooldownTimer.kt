package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient
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

object CooldownTimer {
    private val LOGGER = LoggerFactory.getLogger("WynnWarCooldown")
    private const val GUILD_ATTACK_COMMAND = "guild attack"
    private val activeTimers = mutableMapOf<String, TerritoryTimer>()
    private val expiredTimers = mutableMapOf<String, ExpiredTimer>()

    fun startCooldown(durationSeconds: Long, territoryName: String) {
        require(durationSeconds > 0) { "Cooldown duration must be positive" }

        val client = MinecraftClient.getInstance()
        if (client.world == null) return

        val endTime = System.currentTimeMillis() + (durationSeconds * 1000)
        activeTimers[territoryName] = TerritoryTimer(territoryName, endTime, false, false)

        LOGGER.info("Cooldown timer started for {}: {}s, ends at {}", territoryName, durationSeconds, endTime)
    }

    fun getVisibleTimers(): List<Pair<String, Long>> {
        val now = System.currentTimeMillis()
        val nowSeconds = now / 1000L
        val memoryMs = ModConfig.expiredTimerMemorySeconds * 1000L

        // Clean up old expired timers
        expiredTimers.entries.removeIf { (_, timer) ->
            now - timer.expiredAt > memoryMs
        }

        // Return active timers with remaining seconds
        val activeList = activeTimers.map { (name, timer) ->
            val endSeconds = (timer.endTime + 999L) / 1000L
            val remaining = (endSeconds - nowSeconds).coerceAtLeast(0)
            name to remaining
        }

        // Add expired timers if memory is enabled
        val expiredList = if (ModConfig.expiredTimerMemorySeconds > 0) {
            expiredTimers.map { (name, _) -> name to 0L }
        } else {
            emptyList()
        }

        // Sort by remaining time ascending (shortest on top), with active timers before expired
        return (activeList + expiredList).sortedWith(compareBy<Pair<String, Long>> { it.second == 0L }.thenBy { it.second })
    }

    fun updateTimers() {
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
                timersToRemove.add(territoryName)
                if (ModConfig.expiredTimerMemorySeconds > 0) {
                    expiredTimers[territoryName] = ExpiredTimer(territoryName, now)
                }
            }
        }

        timersToRemove.forEach { activeTimers.remove(it) }
    }

    fun hasActiveTimers(): Boolean = activeTimers.isNotEmpty()

    fun getActiveTerritories(): List<String> {
        return activeTimers.keys.sorted()
    }

    fun removeCooldown(territoryName: String): Boolean {
        val removedFromActive = activeTimers.remove(territoryName) != null
        val removedFromExpired = expiredTimers.remove(territoryName) != null
        val removed = removedFromActive || removedFromExpired
        
        if (removed) {
            LOGGER.info("Removed cooldown for: {}", territoryName)
        }
        return removed
    }

    fun clearAllTimers() {
        activeTimers.clear()
        expiredTimers.clear()
        LOGGER.info("Cleared all timers")
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

        if (currentTerritory != null && currentTerritory != territoryName) {
            LOGGER.info("Skipping command for {} - player not in territory (currently in: {})",
                territoryName, currentTerritory)
            return
        }

        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val actualCommand = GUILD_ATTACK_COMMAND

        LOGGER.info("Executing command for {}: {}", territoryName, actualCommand)
        player.networkHandler?.sendCommand(actualCommand)
    }
}
