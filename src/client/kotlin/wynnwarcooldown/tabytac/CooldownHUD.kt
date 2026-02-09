package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

object CooldownHUD {
    private val textColor = 0xFF00FF00.toInt() // Green
    private val backgroundColor = 0x80000000.toInt() // Semi-transparent black

    fun render(drawContext: DrawContext) {
        if (!ModConfig.isModEnabled || !ModConfig.showTimerHud) return
        if (!CooldownTimer.isActive()) return

        val client = MinecraftClient.getInstance()
        val remaining = CooldownTimer.getRemainingSeconds()
        val formattedTime = CooldownTimer.formatTime(remaining)

        val screenWidth = client.window.scaledWidth
        val screenHeight = client.window.scaledHeight

        // Position: center-bottom of screen (can be made configurable)
        val x = (screenWidth / 2) - 40
        val y = screenHeight - 60

        // Draw background box
        drawContext.fill(x - 10, y - 10, x + 90, y + 20, backgroundColor)

        // Draw text
        drawContext.drawTextWithShadow(
            client.textRenderer,
            "Cooldown: $formattedTime",
            x,
            y,
            textColor
        )
    }
}
