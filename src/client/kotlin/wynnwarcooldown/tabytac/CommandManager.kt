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
                            sendErrorMessage("Unknown sound type: $soundTypeName")
                            return@executes 0
                        }
                        SoundManager.playSound(soundType)
                        sendMessage("Playing sound: $soundTypeName")
                        1
                    }
            )
            .executes {
                SoundManager.playCooldownSound()
                sendMessage("Playing default cooldown sound")
                1
            }

    private fun buildRemoveCommand() =
        ClientCommandManager.literal("remove")
            .then(
                ClientCommandManager.literal("all")
                    .executes {
                        CooldownTimer.clearAllTimers()
                        sendMessage("§aCleared all cooldown timers")
                        1
                    }
            )
            .then(
                ClientCommandManager.argument("territory", StringArgumentType.greedyString())
                    .suggests(getTerritoryAsSuggestions())
                    .executes { context ->
                        val territoryName = StringArgumentType.getString(context, "territory")
                        if (CooldownTimer.removeCooldown(territoryName)) {
                            sendMessage("§aRemoved cooldown for: $territoryName")
                        } else {
                            sendErrorMessage("No cooldown found for: $territoryName")
                        }
                        1
                    }
            )
            .executes {
                sendErrorMessage("§cUsage: /wwc remove <territory|all>")
                0
            }

    private fun buildAddCommand() =
        ClientCommandManager.literal("add")
            .then(
                ClientCommandManager.argument("territory", StringArgumentType.greedyString())
                    .suggests(getAllTerritoriesAsSuggestions())
                    .executes { context ->
                        val territoryName = StringArgumentType.getString(context, "territory")
                        val profile = TerritoryResolver.getTerritoryProfile(territoryName)

                        if (profile == null) {
                            sendErrorMessage("Territory not found: $territoryName")
                            return@executes 0
                        }

                        if (!profile.isOnCooldown) {
                            sendErrorMessage("Territory is not on cooldown: $territoryName")
                            return@executes 0
                        }

                        val remainingSeconds = (600L - (profile.timeHeldMillis / 1000L)).coerceAtLeast(0L)
                        CooldownTimer.startCooldown(remainingSeconds, territoryName)

                        val timeStr = CooldownTimer.formatTime(remainingSeconds)
                        sendMessage("§aAdded cooldown for $territoryName: §f$timeStr")
                        1
                    }
            )
            .executes {
                sendErrorMessage("§cUsage: /wwc add <territory>")
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
                sendMessage("§aCleared expired timers from memory")
                1
            }

    private fun buildToggleCommand() =
        ClientCommandManager.literal("toggle")
            .executes {
                ModConfig.showTimerHud = !ModConfig.showTimerHud
                ModConfig.save()
                val status = if (ModConfig.showTimerHud) "visible" else "hidden"
                sendMessage("§aTimer HUD is now $status")
                1
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

    private fun displayCooldownList() {
        val timers = CooldownTimer.getVisibleTimers()

        if (timers.isEmpty()) {
            sendMessage("§6No active cooldowns")
            return
        }

        sendMessage("§6=== Active Cooldowns ===")
        timers.forEach { (territory, remaining) ->
            val timeStr = if (remaining > 0) {
                CooldownTimer.formatTime(remaining)
            } else {
                "EXPIRED"
            }
            sendMessage("§e$territory §7→ §f$timeStr")
        }
        sendMessage("§6======================")
    }

    private fun sendMessage(message: String) {
        val client = MinecraftClient.getInstance()
        client.inGameHud?.chatHud?.addMessage(Text.literal(message))
    }

    private fun sendErrorMessage(message: String) {
        val client = MinecraftClient.getInstance()
        client.inGameHud?.chatHud?.addMessage(Text.literal("§c$message"))
    }
}
