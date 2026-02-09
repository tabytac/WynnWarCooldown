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

object ModConfig {
    var isModEnabled = true
    var timerOffsetSeconds = 0
    var commandExecutionOffsetSeconds = 0
    var soundPlayOffsetSeconds = 0
    var customCommand = "/gu a"
    var showTimerHud = true
    var hudXPercent = 0.5f
    var hudYPercent = 0.85f
    var soundVolume = 1.0f
    var selectedSound = SoundType.WAR_HORN
    var showBackgroundBox = true
    var textColorHex = "00FF00"
    var hudScale = 1.0f
    var activeConfigScreen: Screen? = null

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

    data class ConfigData(
        val isModEnabled: Boolean = true,
        val timerOffsetSeconds: Int = 0,
        val commandExecutionOffsetSeconds: Int = 0,
        val soundPlayOffsetSeconds: Int = 0,
        val customCommand: String = "/gu a",
        val showTimerHud: Boolean = true,
        val hudXPercent: Float = 0.5f,
        val hudYPercent: Float = 0.85f,
        val soundVolume: Float = 1.0f,
        val selectedSound: String = "WAR_HORN",
        val showBackgroundBox: Boolean = true,
        val textColorHex: String = "00FF00",
        val hudScale: Float = 1.0f
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
            commandExecutionOffsetSeconds = data.commandExecutionOffsetSeconds
            soundPlayOffsetSeconds = data.soundPlayOffsetSeconds
            customCommand = data.customCommand
            showTimerHud = data.showTimerHud
            hudXPercent = data.hudXPercent
            hudYPercent = data.hudYPercent
            soundVolume = data.soundVolume
            selectedSound = SoundType.valueOf(data.selectedSound)
            showBackgroundBox = data.showBackgroundBox
            textColorHex = data.textColorHex
            hudScale = data.hudScale
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun save() {
        try {
            val data = ConfigData(
                isModEnabled = isModEnabled,
                timerOffsetSeconds = timerOffsetSeconds,
                commandExecutionOffsetSeconds = commandExecutionOffsetSeconds,
                soundPlayOffsetSeconds = soundPlayOffsetSeconds,
                customCommand = customCommand,
                showTimerHud = showTimerHud,
                hudXPercent = hudXPercent,
                hudYPercent = hudYPercent,
                soundVolume = soundVolume,
                selectedSound = selectedSound.name,
                showBackgroundBox = showBackgroundBox,
                textColorHex = textColorHex,
                hudScale = hudScale
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
        buildActionCategory(builder, entryBuilder)

        val screen = builder.build()
        activeConfigScreen = screen
        return screen
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
                Text.translatable("wynn-war-cooldown.config.command_offset"),
                commandExecutionOffsetSeconds,
                EXEC_OFFSET_MIN,
                EXEC_OFFSET_MAX
            )
                .setDefaultValue(0)
                .setSaveConsumer { commandExecutionOffsetSeconds = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.command_offset.tooltip"))
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
            entryBuilder.startStrField(Text.translatable("wynn-war-cooldown.config.command"), customCommand)
                .setSaveConsumer { customCommand = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.command.tooltip"))
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
            entryBuilder.startFloatField(Text.translatable("wynn-war-cooldown.config.volume"), soundVolume)
                .setMin(SOUND_VOLUME_MIN)
                .setMax(SOUND_VOLUME_MAX)
                .setSaveConsumer { soundVolume = it }
                .build()
        )
    }
}
