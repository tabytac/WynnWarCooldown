package wynnwarcooldown.tabytac

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

object CommandManager {
    private val LOGGER = LoggerFactory.getLogger("WynnWarCooldown")

    fun registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            val rootCommand = ClientCommandManager.literal("wynnwarcooldown")
                .then(buildTestSoundCommand())
                .then(buildRemoveCommand())
                .then(buildAddCommand())
                .then(buildCooldownListCommand())
                .then(buildClearExpiredCommand())
                .then(buildToggleCommand())
                .then(buildTriggerCaptureCommand())

            // Main command: /wynnwarcooldown
            dispatcher.register(rootCommand)

            // Alias: /wwc
            dispatcher.register(
                ClientCommandManager.literal("wwc")
                    .then(buildTestSoundCommand())
                    .then(buildRemoveCommand())
                    .then(buildAddCommand())
                    .then(buildCooldownListCommand())
                    .then(buildClearExpiredCommand())
                    .then(buildToggleCommand())
                    .then(buildTriggerCaptureCommand())
            )
        }
    }

    private fun buildTestSoundCommand() =
        ClientCommandManager.literal("test-sound")
            .then(
                ClientCommandManager.argument("soundType", StringArgumentType.word())
                    .suggests(getSoundTypeAsSuggestions())
                    .executes { context ->
                        val soundTypeName = StringArgumentType.getString(context, "soundType")
                        val soundType = try {
                            SoundType.valueOf(soundTypeName.uppercase())
                        } catch (e: IllegalArgumentException) {
                            sendChat("wynn-war-cooldown.chat.unknown_sound_type", soundTypeName)
                            return@executes 0
                        }
                        SoundManager.playSound(soundType)
                        sendChat("wynn-war-cooldown.chat.playing_sound", soundTypeName)
                        1
                    }
            )
            .executes {
                SoundManager.playCooldownSound()
                sendChat("wynn-war-cooldown.chat.playing_default_sound")
                1
            }

    private fun buildRemoveCommand() =
        ClientCommandManager.literal("remove")
            .then(
                ClientCommandManager.literal("all")
                    .executes {
                        CooldownTimer.clearAllTimers()
                        sendChat("wynn-war-cooldown.chat.cleared_all")
                        1
                    }
            )
            .then(
                ClientCommandManager.argument("territory", StringArgumentType.greedyString())
                    .suggests(getTerritoryAsSuggestions())
                    .executes { context ->
                        var input = StringArgumentType.getString(context, "territory").trim()

                        // allow users to include accidental trailing numbers (strip them)
                        val trailingSecondsMatch = Regex("(.*)\\s+(\\d+)").find(input)
                        if (trailingSecondsMatch != null) input = trailingSecondsMatch.groupValues[1].trim()

                        val territoryName = findTerritoryName(input) ?: input

                        if (CooldownTimer.removeCooldown(territoryName)) {
                            sendChat("wynn-war-cooldown.chat.removed_cooldown", territoryName)
                        } else {
                            sendChat("wynn-war-cooldown.chat.no_cooldown_found", territoryName)
                        }
                        1
                    }
            )
            .executes {
                sendChat("wynn-war-cooldown.chat.usage_remove")
                0
            }

    private fun buildAddCommand() =
        ClientCommandManager.literal("add")
            .then(
                ClientCommandManager.argument("territory", StringArgumentType.greedyString())
                    .suggests(getTerritoriesOnCooldownAsSuggestions())
                    .executes { context ->
                        val inputName = StringArgumentType.getString(context, "territory")
                        val territoryName = findTerritoryName(inputName)

                        if (territoryName == null) {
                            sendChat("wynn-war-cooldown.chat.territory_not_found", inputName)
                            return@executes 0
                        }

                        val profile = TerritoryResolver.getTerritoryProfile(territoryName)

                        if (profile == null) {
                            sendChat("wynn-war-cooldown.chat.territory_not_found", inputName)
                            return@executes 0
                        }

                        if (!profile.isOnCooldown) {
                            sendChat("wynn-war-cooldown.chat.not_on_cooldown", territoryName)
                            return@executes 0
                        }

                        val remainingSeconds = (600L - (profile.timeHeldMillis / 1000L)).coerceAtLeast(0L)
                        CooldownTimer.startCooldown(remainingSeconds, territoryName)

                        val timeStr = CooldownTimer.formatTime(remainingSeconds)
                        sendChat("wynn-war-cooldown.chat.added_cooldown", territoryName, timeStr)
                        1
                    }
            )
            .executes {
                sendChat("wynn-war-cooldown.chat.usage_add")
                0
            }

    private fun buildCooldownListCommand() =
        ClientCommandManager.literal("list")
            .executes {
                displayCooldownList()
                1
            }

    private fun buildClearExpiredCommand() =
        ClientCommandManager.literal("clear-expired")
            .executes {
                CooldownTimer.clearExpiredTimers()
                sendChat("wynn-war-cooldown.chat.cleared_expired")
                1
            }

    private fun buildToggleCommand() =
        ClientCommandManager.literal("toggle")
            .executes {
                ModConfig.showTimerHud = !ModConfig.showTimerHud
                ModConfig.save()
                val status = if (ModConfig.showTimerHud) "visible" else "hidden"
                sendChat("wynn-war-cooldown.chat.timer_hud_status", status)
                1
            }

    // TEMP: trigger a capture reminder for testing without actually warring
    private fun buildTriggerCaptureCommand() =
        ClientCommandManager.literal("trigger-capture")
            .then(
                ClientCommandManager.argument("territory", StringArgumentType.greedyString())
                    .suggests(getAllTerritoriesAsSuggestions())
                    .executes { context ->
                        var input = StringArgumentType.getString(context, "territory").trim()

                        // support trailing seconds (e.g. "Temple Island 535") by extracting trailing integer
                        val trailingSecondsMatch = Regex("(.*)\\s+(\\d+)").find(input)
                        val secondsAgo: Long? = if (trailingSecondsMatch != null) {
                            input = trailingSecondsMatch.groupValues[1].trim()
                            trailingSecondsMatch.groupValues[2].toLongOrNull()
                        } else null

                        val territoryName = findTerritoryName(input)
                        if (territoryName == null) {
                            sendChat("wynn-war-cooldown.chat.territory_not_found", input)
                            return@executes 0
                        }

                        if (secondsAgo != null) {
                            val captureTime = System.currentTimeMillis() - (secondsAgo * 1000L)
                            CooldownTimer.recordCapture(territoryName, captureTime)
                        } else {
                            CooldownTimer.recordCapture(territoryName)
                        }

                        sendChat("wynn-war-cooldown.chat.triggered_capture", territoryName)
                        1
                    }
            )
            .executes {
                sendChat("wynn-war-cooldown.chat.usage_trigger_capture")
                0
            }

    private fun getTerritoryAsSuggestions(): SuggestionProvider<FabricClientCommandSource> {
        return SuggestionProvider { context, builder ->
            val remaining = builder.remainingLowerCase
            val territories = CooldownTimer.getActiveTerritories()
            territories
                .filter { it.lowercase().startsWith(remaining) }
                .forEach { territory ->
                    builder.suggest(territory)
                }
            builder.buildFuture()
        }
    }

    private fun findTerritoryName(input: String): String? {
        val inputLower = input.lowercase()
        val allTerritories = TerritoryResolver.getAllTerritoryNames()
        return allTerritories.firstOrNull { it.lowercase() == inputLower }
    }

    private fun getTerritoriesOnCooldownAsSuggestions(): SuggestionProvider<FabricClientCommandSource> {
        return SuggestionProvider { context, builder ->
            val remaining = builder.remainingLowerCase
            val territories = TerritoryResolver.getAllTerritoryNames()
            territories
                .filter { territoryName ->
                    val profile = TerritoryResolver.getTerritoryProfile(territoryName)
                    profile?.isOnCooldown == true && territoryName.lowercase().startsWith(remaining)
                }
                .forEach { territory ->
                    builder.suggest(territory)
                }
            builder.buildFuture()
        }
    }

    private fun getAllTerritoriesAsSuggestions(): SuggestionProvider<FabricClientCommandSource> {
        return SuggestionProvider { context, builder ->
            val remaining = builder.remainingLowerCase
            val territories = TerritoryResolver.getAllTerritoryNames()
            territories
                .filter { it.lowercase().startsWith(remaining) }
                .forEach { territory ->
                    builder.suggest(territory)
                }
            builder.buildFuture()
        }
    }

    private fun getSoundTypeAsSuggestions(): SuggestionProvider<FabricClientCommandSource> {
        return SuggestionProvider { context, builder ->
            SoundType.values().forEach { soundType ->
                builder.suggest(soundType.name.lowercase())
            }
            builder.buildFuture()
        }
    }

    private fun sendChat(key: String, vararg args: Any) {
        val client = MinecraftClient.getInstance()
        client.inGameHud?.chatHud?.addMessage(Text.translatable(key, *args))
    }

    private fun displayCooldownList() {
        val timers = CooldownTimer.getVisibleTimers()

        if (timers.isEmpty()) {
            sendChat("wynn-war-cooldown.chat.no_active_cooldowns")
            return
        }

        sendChat("wynn-war-cooldown.chat.active_cooldowns_header")
        timers.forEach { timer ->
            val timeStr = when (timer.type) {
                VisibleTimerType.CAPTURE -> "CAPTURED ${CooldownTimer.formatTime(timer.seconds)}"
                VisibleTimerType.EXPIRED -> "EXPIRED"
                else -> CooldownTimer.formatTime(timer.seconds)
            }
            sendChat("wynn-war-cooldown.chat.active_cooldown_line", timer.territoryName, timeStr)
        }
        sendChat("wynn-war-cooldown.chat.active_cooldowns_footer")
    }

    // Legacy helpers kept for compatibility (prefer `sendChat(key, ...)`)
    private fun sendMessage(message: String) {
        val client = MinecraftClient.getInstance()
        client.inGameHud?.chatHud?.addMessage(Text.literal(message))
    }

    private fun sendErrorMessage(message: String) {
        val client = MinecraftClient.getInstance()
        client.inGameHud?.chatHud?.addMessage(Text.literal("§c$message"))
    }
}
