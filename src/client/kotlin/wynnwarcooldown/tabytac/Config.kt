package wynnwarcooldown.tabytac

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import java.io.File

enum class SoundType(val displayName: String, val soundId: String) {
    WAR_HORN("War Horn", "wynn-war-cooldown:war_horn"),
    EXPERIENCE_ORB("Experience Orb", "entity.experience_orb.pickup"),
    BELL("Bell", "block.bell.use"),
    LEVEL_UP("Level Up", "entity.player.levelup");

    override fun toString(): String = displayName
}

enum class HudAlignment {
    LEFT,
    CENTER,
    RIGHT
}

object ModConfig {
    var isModEnabled = true
    var timerOffsetSeconds = 0
    var soundPlayOffsetSeconds = 0
    var sendGuildAttackAtEnd = true
    var showTimerHud = true
    var hudXPercent = 0.8f
    var hudYPercent = 0.25f
    var hudAlignment: HudAlignment = HudAlignment.RIGHT
    var soundVolume = 1.0f
    var selectedSound = SoundType.WAR_HORN
    var showBackgroundBox = false
    var textColorHex = "FF5522"
    var expiredTextColorHex = "22FF55"
    var currentTextColorHex = "FF9900"
    // var captureTextColorHex = "AA00FF" // capture HUD color commented out (HUD disabled)
    var hudScale = 1.0f
    var expiredTimerMemorySeconds = 30
    var removeTimerOnQueue = true

    // announce when a regular (non-capture) timer finishes
    var announceTimerOffCooldown = false

    // Capture reminder (when *we* take a territory)
    var enableCaptureReminder = true

    // var captureReminderShowHud = false // HUD toggle commented out (capture HUD removed)
    var captureReminderAnnounceChat = true
    var captureReminderPlaySound = false

    // Example: 30 = send reminder when 1:00 remains (i.e. at 9:30 elapsed)
    var captureReminderBeforeSeconds = 30

    private val configFile: File by lazy {
        val configDir = FabricLoader.getInstance().configDir.toFile()
        File(configDir, "wynn-war-cooldown.json")
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // HUD constants
    private const val HUD_POS_X_MIN = 0.0f
    private const val HUD_POS_X_MAX = 1.0f
    private const val HUD_POS_Y_MIN = 0.0f
    private const val HUD_POS_Y_MAX = 1.0f
    private const val SOUND_VOLUME_MIN = 0.0f
    private const val SOUND_VOLUME_MAX = 1.0f
    private const val HUD_SCALE_MIN = 0.1f
    private const val HUD_SCALE_MAX = 5.0f
    private const val TIMER_OFFSET_MIN = -20
    private const val TIMER_OFFSET_MAX = 20
    private const val EXEC_OFFSET_MIN = -20
    private const val EXEC_OFFSET_MAX = 20
    private const val EXPIRED_MEMORY_MIN = 0
    private const val EXPIRED_MEMORY_MAX = 60



    data class ConfigData(
        val isModEnabled: Boolean = true,
        val timerOffsetSeconds: Int = 0,
        val soundPlayOffsetSeconds: Int = 0,
        val sendGuildAttackAtEnd: Boolean = true,
        val showTimerHud: Boolean = true,
        val hudXPercent: Float = 0.8f,
        val hudYPercent: Float = 0.25f,
        val hudAlignment: String = "RIGHT",
        val soundVolume: Float = 1.0f,
        val selectedSound: String = "WAR_HORN",
        val showBackgroundBox: Boolean = false,
        val textColorHex: String = "FF5522",
        val expiredTextColorHex: String = "22FF55",
        val currentTextColorHex: String = "FF9900",
        // val captureTextColorHex: String = "AA00FF", // removed (HUD disabled)
        val hudScale: Float = 1.0f,
        val enableCaptureReminder: Boolean = true,
        // val captureReminderShowHud: Boolean = false, // removed (HUD disabled)
        val captureReminderAnnounceChat: Boolean = true,
        val captureReminderPlaySound: Boolean = false,
        val captureReminderBeforeSeconds: Int = 30,
        val announceTimerOffCooldown: Boolean = false,
        val expiredTimerMemorySeconds: Int = 30,
        val removeTimerOnQueue: Boolean = true
    )

    fun load() {
        try {
            if (!configFile.exists()) {
                save()
                return
            }

            val data = gson.fromJson(configFile.readText(), ConfigData::class.java)
            isModEnabled = data.isModEnabled
            timerOffsetSeconds = data.timerOffsetSeconds
            soundPlayOffsetSeconds = data.soundPlayOffsetSeconds
            sendGuildAttackAtEnd = data.sendGuildAttackAtEnd
            showTimerHud = data.showTimerHud
            hudXPercent = data.hudXPercent
            try { hudAlignment = HudAlignment.valueOf(data.hudAlignment) } catch (_: Exception) { hudAlignment = HudAlignment.RIGHT }
            hudYPercent = data.hudYPercent
            soundVolume = data.soundVolume
            selectedSound = SoundType.valueOf(data.selectedSound)
            showBackgroundBox = data.showBackgroundBox
            textColorHex = data.textColorHex
            expiredTextColorHex = data.expiredTextColorHex
            currentTextColorHex = data.currentTextColorHex
            // captureTextColorHex = data.captureTextColorHex // commented out (HUD disabled)
            hudScale = data.hudScale
            enableCaptureReminder = data.enableCaptureReminder
            // captureReminderShowHud = data.captureReminderShowHud // commented out (HUD disabled)
            captureReminderAnnounceChat = data.captureReminderAnnounceChat
            captureReminderPlaySound = data.captureReminderPlaySound
            captureReminderBeforeSeconds = data.captureReminderBeforeSeconds
            announceTimerOffCooldown = data.announceTimerOffCooldown
            expiredTimerMemorySeconds = data.expiredTimerMemorySeconds
            removeTimerOnQueue = data.removeTimerOnQueue
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun save() {
        try {
            val data = ConfigData(
                isModEnabled = isModEnabled,
                timerOffsetSeconds = timerOffsetSeconds,
                soundPlayOffsetSeconds = soundPlayOffsetSeconds,
                sendGuildAttackAtEnd = sendGuildAttackAtEnd,
                showTimerHud = showTimerHud,
                hudXPercent = hudXPercent,
                hudYPercent = hudYPercent,
                soundVolume = soundVolume,
                selectedSound = selectedSound.name,
                showBackgroundBox = showBackgroundBox,
                textColorHex = textColorHex,
                expiredTextColorHex = expiredTextColorHex,
                currentTextColorHex = currentTextColorHex,
                // captureTextColorHex = captureTextColorHex, // commented out (HUD disabled)
                hudScale = hudScale,
                enableCaptureReminder = enableCaptureReminder,
                // captureReminderShowHud = captureReminderShowHud, // commented out (HUD disabled)
                captureReminderAnnounceChat = captureReminderAnnounceChat,
                captureReminderPlaySound = captureReminderPlaySound,
                captureReminderBeforeSeconds = captureReminderBeforeSeconds,
                announceTimerOffCooldown = announceTimerOffCooldown,
                expiredTimerMemorySeconds = expiredTimerMemorySeconds,
                removeTimerOnQueue = removeTimerOnQueue
                , hudAlignment = hudAlignment.name
            )
            configFile.writeText(gson.toJson(data))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseHexToColor(hex: String): Int {
        return try {
            val cleanHex = hex.replace("#", "").take(6)
            Integer.parseInt(cleanHex, 16)
        } catch (e: Exception) {
            0x00FF00 // Green default
        }
    }

    private fun colorToHex(color: Int): String {
        return String.format("%06X", color and 0xFFFFFF)
    }

    fun buildConfigScreen(parent: Screen?): Screen {
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("wynn-war-cooldown.config.title"))
            .setSavingRunnable { save() }

        val entryBuilder = builder.entryBuilder()

        buildGeneralCategory(builder, entryBuilder)
        buildTimerCategory(builder, entryBuilder)
        buildCaptureCategory(builder, entryBuilder)
        buildActionCategory(builder, entryBuilder)

        return builder.build()
    }

    private fun buildGeneralCategory(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val general = builder.getOrCreateCategory(Text.translatable("wynn-war-cooldown.config.general"))

        general.addEntry(
            entryBuilder.startBooleanToggle(Text.translatable("wynn-war-cooldown.config.enabled"), isModEnabled)
                .setSaveConsumer { isModEnabled = it }
                .build()
        )

        general.addEntry(
            entryBuilder.startBooleanToggle(Text.translatable("wynn-war-cooldown.config.show_timer"), showTimerHud)
                .setSaveConsumer { showTimerHud = it }
                .build()
        )

        general.addEntry(
            entryBuilder.startBooleanToggle(Text.translatable("wynn-war-cooldown.config.show_background"), showBackgroundBox)
                .setSaveConsumer { showBackgroundBox = it }
                .build()
        )

        general.addEntry(
            entryBuilder.startFloatField(Text.translatable("wynn-war-cooldown.config.hud_x"), hudXPercent)
                .setMin(HUD_POS_X_MIN)
                .setMax(HUD_POS_X_MAX)
                .setSaveConsumer { hudXPercent = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.hud_x.tooltip"))
                .build()
        )

        general.addEntry(
            entryBuilder.startFloatField(Text.translatable("wynn-war-cooldown.config.hud_y"), hudYPercent)
                .setMin(HUD_POS_Y_MIN)
                .setMax(HUD_POS_Y_MAX)
                .setSaveConsumer { hudYPercent = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.hud_y.tooltip"))
                .build()
        )

        general.addEntry(
            entryBuilder.startFloatField(Text.translatable("wynn-war-cooldown.config.hud_scale"), hudScale)
                .setMin(HUD_SCALE_MIN)
                .setMax(HUD_SCALE_MAX)
                .setSaveConsumer { hudScale = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.hud_scale.tooltip"))
                .build()
        )

        general.addEntry(
            entryBuilder.startSelector(
                Text.translatable("wynn-war-cooldown.config.hud_alignment"),
                HudAlignment.values(),
                hudAlignment
            )
                .setDefaultValue(HudAlignment.RIGHT)
                .setSaveConsumer { hudAlignment = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.hud_alignment.tooltip"))
                .build()
        )
    }

    private fun buildTimerCategory(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val timer = builder.getOrCreateCategory(Text.translatable("wynn-war-cooldown.config.timer"))

        timer.addEntry(
            entryBuilder.startIntSlider(
                Text.translatable("wynn-war-cooldown.config.offset"),
                timerOffsetSeconds,
                TIMER_OFFSET_MIN,
                TIMER_OFFSET_MAX
            )
                .setDefaultValue(0)
                .setSaveConsumer { timerOffsetSeconds = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.offset.tooltip"))
                .build()
        )

        timer.addEntry(
            entryBuilder.startIntSlider(
                Text.translatable("wynn-war-cooldown.config.sound_offset"),
                soundPlayOffsetSeconds,
                EXEC_OFFSET_MIN,
                EXEC_OFFSET_MAX
            )
                .setDefaultValue(0)
                .setSaveConsumer { soundPlayOffsetSeconds = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.sound_offset.tooltip"))
                .build()
        )

        timer.addEntry(
            entryBuilder.startIntSlider(
                Text.translatable("wynn-war-cooldown.config.expired_memory"),
                expiredTimerMemorySeconds,
                EXPIRED_MEMORY_MIN,
                EXPIRED_MEMORY_MAX
            )
                .setDefaultValue(0)
                .setSaveConsumer { expiredTimerMemorySeconds = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.expired_memory.tooltip"))
                .build()
        )

        timer.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("wynn-war-cooldown.config.remove_on_queue"),
                removeTimerOnQueue
            )
                .setDefaultValue(true)
                .setSaveConsumer { removeTimerOnQueue = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.remove_on_queue.tooltip"))
                .build()
        )

        timer.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("wynn-war-cooldown.config.announce_off_cooldown"),
                announceTimerOffCooldown
            )
                .setDefaultValue(false)
                .setSaveConsumer { announceTimerOffCooldown = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.announce_off_cooldown.tooltip"))
                .build()
        )
    }

    private fun buildActionCategory(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val action = builder.getOrCreateCategory(Text.translatable("wynn-war-cooldown.config.action"))

        action.addEntry(
            entryBuilder.startSelector(
                Text.translatable("wynn-war-cooldown.config.sound_type"),
                SoundType.values(),
                selectedSound
            )
                .setDefaultValue(SoundType.WAR_HORN)
                .setSaveConsumer { selectedSound = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.sound_type.tooltip"))
                .build()
        )

        action.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("wynn-war-cooldown.config.send_guild_attack"),
                sendGuildAttackAtEnd
            )
                .setSaveConsumer { sendGuildAttackAtEnd = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.send_guild_attack.tooltip"))
                .build()
        )

        action.addEntry(
            entryBuilder.startColorField(
                Text.translatable("wynn-war-cooldown.config.text_color"),
                parseHexToColor(textColorHex)
            )
                .setSaveConsumer {
                    textColorHex = colorToHex(it)
                }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.text_color.tooltip"))
                .build()
        )

        action.addEntry(
            entryBuilder.startColorField(
                Text.translatable("wynn-war-cooldown.config.expired_text_color"),
                parseHexToColor(expiredTextColorHex)
            )
                .setSaveConsumer {
                    expiredTextColorHex = colorToHex(it)
                }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.expired_text_color.tooltip"))
                .build()
        )

        action.addEntry(
            entryBuilder.startColorField(
                Text.translatable("wynn-war-cooldown.config.current_text_color"),
                parseHexToColor(currentTextColorHex)
            )
                .setSaveConsumer {
                    currentTextColorHex = colorToHex(it)
                }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.current_text_color.tooltip"))
                .build()
        )

        action.addEntry(
            entryBuilder.startFloatField(Text.translatable("wynn-war-cooldown.config.volume"), soundVolume)
                .setMin(SOUND_VOLUME_MIN)
                .setMax(SOUND_VOLUME_MAX)
                .setSaveConsumer { soundVolume = it }
                .build()
        )
    }

    private fun buildCaptureCategory(builder: ConfigBuilder, entryBuilder: ConfigEntryBuilder) {
        val capture = builder.getOrCreateCategory(Text.translatable("wynn-war-cooldown.config.capture"))

        capture.addEntry(
            entryBuilder.startBooleanToggle(Text.translatable("wynn-war-cooldown.config.capture_enable"), enableCaptureReminder)
                .setSaveConsumer { enableCaptureReminder = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.capture_enable.tooltip"))
                .build()
        )

        /*capture.addEntry(
            entryBuilder.startBooleanToggle(Text.translatable("wynn-war-cooldown.config.capture_hud"), captureReminderShowHud)
                .setSaveConsumer { captureReminderShowHud = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.capture_hud.tooltip"))
                .build()
        )*/  // commented out (HUD disabled)

        capture.addEntry(
            entryBuilder.startBooleanToggle(Text.translatable("wynn-war-cooldown.config.capture_chat"), captureReminderAnnounceChat)
                .setSaveConsumer { captureReminderAnnounceChat = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.capture_chat.tooltip"))
                .build()
        )

        capture.addEntry(
            entryBuilder.startBooleanToggle(Text.translatable("wynn-war-cooldown.config.capture_sound"), captureReminderPlaySound)
                .setSaveConsumer { captureReminderPlaySound = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.capture_sound.tooltip"))
                .build()
        )

        capture.addEntry(
            entryBuilder.startIntField(
                Text.translatable("wynn-war-cooldown.config.capture_before_vulnerable"),
                captureReminderBeforeSeconds
            )
                .setMin(0)
                .setMax(600)
                .setDefaultValue(30)
                .setSaveConsumer { captureReminderBeforeSeconds = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.capture_before_vulnerable.tooltip"))
                .build()
        )

        /*capture.addEntry(
            entryBuilder.startColorField(
                Text.translatable("wynn-war-cooldown.config.capture_color"),
                parseHexToColor(captureTextColorHex)
            )
                .setSaveConsumer {
                    captureTextColorHex = colorToHex(it)
                }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.capture_color.tooltip"))
                .build()
        )*/
    }
}
