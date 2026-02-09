package wynnwarcooldown.tabytac

import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

object ModConfig {
    var isModEnabled = true
    var timerOffsetSeconds = 0
    var customCommand = "/gu a"
    var showTimerHud = true
    var soundVolume = 1.0f

    fun buildConfigScreen(parent: Screen?): Screen {
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("Wynn War Cooldown Configuration"))

        val entryBuilder = builder.entryBuilder()

        // General Settings
        val general = builder.getOrCreateCategory(Text.literal("General"))

        general.addEntry(
            entryBuilder.startBooleanToggle(Text.literal("Mod Enabled"), isModEnabled)
                .setSaveConsumer { isModEnabled = it }
                .build()
        )

        general.addEntry(
            entryBuilder.startBooleanToggle(Text.literal("Show Timer HUD"), showTimerHud)
                .setSaveConsumer { showTimerHud = it }
                .build()
        )

        // Timer Settings
        val timer = builder.getOrCreateCategory(Text.literal("Timer"))

        timer.addEntry(
            entryBuilder.startIntSlider(Text.literal("Timer Offset (seconds)"), timerOffsetSeconds, -60, 60)
                .setDefaultValue(0)
                .setSaveConsumer { timerOffsetSeconds = it }
                .setTooltip(
                    Text.literal("Adjust when the timer ends relative to the cooldown.\nPositive = earlier, Negative = later")
                )
                .build()
        )

        // Action Settings
        val action = builder.getOrCreateCategory(Text.literal("Action"))

        action.addEntry(
            entryBuilder.startStrField(Text.literal("Command to Execute"), customCommand)
                .setSaveConsumer { customCommand = it }
                .setTooltip(Text.literal("Command/message sent when timer ends.\nUse /say for chat or /request for commands"))
                .build()
        )

        action.addEntry(
            entryBuilder.startFloatField(Text.literal("Sound Volume"), soundVolume)
                .setMin(0.0f)
                .setMax(1.0f)
                .setSaveConsumer { soundVolume = it }
                .build()
        )

        return builder.build()
    }
}
