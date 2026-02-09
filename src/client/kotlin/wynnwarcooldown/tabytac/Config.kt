package wynnwarcooldown.tabytac

import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

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

    fun buildConfigScreen(parent: Screen?): Screen {
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("wynn-war-cooldown.config.title"))

        val entryBuilder = builder.entryBuilder()

        // General Settings
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
            entryBuilder.startFloatField(Text.literal("HUD X Position (0-1)"), hudXPercent)
                .setMin(HUD_POS_X_MIN)
                .setMax(HUD_POS_X_MAX)
                .setSaveConsumer { hudXPercent = it }
                .setTooltip(Text.literal("0 = left, 0.5 = center, 1 = right"))
                .build()
        )

        general.addEntry(
            entryBuilder.startFloatField(Text.literal("HUD Y Position (0-1)"), hudYPercent)
                .setMin(HUD_POS_Y_MIN)
                .setMax(HUD_POS_Y_MAX)
                .setSaveConsumer { hudYPercent = it }
                .setTooltip(Text.literal("0 = top, 0.85 = just above hotbar, 1 = bottom"))
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

        // Timer Settings
        val timer = builder.getOrCreateCategory(Text.translatable("wynn-war-cooldown.config.timer"))

        timer.addEntry(
            entryBuilder.startIntSlider(Text.translatable("wynn-war-cooldown.config.offset"), timerOffsetSeconds, TIMER_OFFSET_MIN, TIMER_OFFSET_MAX)
                .setDefaultValue(0)
                .setSaveConsumer { timerOffsetSeconds = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.offset.tooltip"))
                .build()
        )

        timer.addEntry(
            entryBuilder.startIntSlider(Text.translatable("wynn-war-cooldown.config.command_offset"), commandExecutionOffsetSeconds, EXEC_OFFSET_MIN, EXEC_OFFSET_MAX)
                .setDefaultValue(0)
                .setSaveConsumer { commandExecutionOffsetSeconds = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.command_offset.tooltip"))
                .build()
        )

        timer.addEntry(
            entryBuilder.startIntSlider(Text.translatable("wynn-war-cooldown.config.sound_offset"), soundPlayOffsetSeconds, EXEC_OFFSET_MIN, EXEC_OFFSET_MAX)
                .setDefaultValue(0)
                .setSaveConsumer { soundPlayOffsetSeconds = it }
                .setTooltip(Text.translatable("wynn-war-cooldown.config.sound_offset.tooltip"))
                .build()
        )

        // Action Settings
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
            entryBuilder.startStrField(Text.literal("Text Color (Hex)"), textColorHex)
                .setSaveConsumer { textColorHex = it.replace("#", "").take(6).uppercase() }
                .setTooltip(Text.literal("Enter hex color without #. Example: FF0000 for red"))
                .build()
        )

        action.addEntry(
            entryBuilder.startFloatField(Text.translatable("wynn-war-cooldown.config.volume"), soundVolume)
                .setMin(SOUND_VOLUME_MIN)
                .setMax(SOUND_VOLUME_MAX)
                .setSaveConsumer { soundVolume = it }
                .build()
        )

        return builder.build()
    }
}
