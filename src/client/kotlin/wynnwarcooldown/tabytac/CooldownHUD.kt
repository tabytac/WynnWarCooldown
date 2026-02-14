package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt

object CooldownHUD {
    private const val TEXT_COLOR_ALPHA = 0xFF000000.toInt() // Alpha mask
    private const val BACKGROUND_COLOR = 0x80000000.toInt() // Semi-transparent black
    private const val PADDING_X = 10
    private const val PADDING_Y = 6
    private const val LINE_SPACING = 4
    private const val SCALE_MIN = 0.1f
    private const val SCALE_MAX = 5.0f

    fun render(drawContext: DrawContext) {
        if (!ModConfig.isModEnabled || !ModConfig.showTimerHud) return

        CooldownTimer.updateTimers()
        val visibleTimers = CooldownTimer.getVisibleTimers()
        if (visibleTimers.isEmpty()) return

        val client = MinecraftClient.getInstance()
        val scale = ModConfig.hudScale.coerceIn(SCALE_MIN, SCALE_MAX)

        visibleTimers.forEachIndexed { index, (territoryName, remaining) ->
            val formattedTime = CooldownTimer.formatTime(remaining)
            val label = "$territoryName: $formattedTime"
            val yOffset = (index * (client.textRenderer.fontHeight + LINE_SPACING) * scale).roundToInt()
            val textColorHex = if (remaining == 0L) {
                ModConfig.expiredTextColorHex
            } else {
                ModConfig.textColorHex
            }

            renderTimerLine(drawContext, client, label, yOffset, scale, textColorHex)
        }
    }

    private fun renderTimerLine(
        drawContext: DrawContext,
        client: MinecraftClient,
        label: String,
        yOffset: Int,
        scale: Float,
        textColorHex: String
    ) {
        val screenWidth = client.window.scaledWidth
        val screenHeight = client.window.scaledHeight
        val textWidth = client.textRenderer.getWidth(label)
        val textHeight = client.textRenderer.fontHeight

        val boxWidth = ((textWidth + (PADDING_X * 2)) * scale).roundToInt()
        val boxHeight = ((textHeight + (PADDING_Y * 2)) * scale).roundToInt()

        val rawX = (screenWidth * ModConfig.hudXPercent) - (boxWidth / 2f)
        val baseY = screenHeight * ModConfig.hudYPercent
        val rawY = baseY + yOffset - (boxHeight / 2f)

        val x = rawX.toInt().coerceIn(0, screenWidth - boxWidth)
        val y = rawY.toInt().coerceIn(0, screenHeight - boxHeight)

        // Draw background box if enabled
        if (ModConfig.showBackgroundBox) {
            drawContext.fill(x, y, x + boxWidth, y + boxHeight, BACKGROUND_COLOR)
        }

        // Draw text with shadow
        val textColor = parseHexColor(textColorHex)
        drawContext.matrices.push()
        drawContext.matrices.translate(
            (x + (PADDING_X * scale)).toDouble(),
            (y + (PADDING_Y * scale)).toDouble(),
            0.0
        )
        drawContext.matrices.scale(scale, scale, 1.0f)
        drawContext.drawTextWithShadow(
            client.textRenderer,
            label,
            0,
            0,
            textColor
        )
        drawContext.matrices.pop()
    }

    private fun parseHexColor(hexString: String): Int {
        return try {
            val cleanHex = hexString.replace("#", "").take(6)
            (TEXT_COLOR_ALPHA or Integer.parseInt(cleanHex, 16))
        } catch (e: Exception) {
            0xFF00FF00.toInt() // Fallback to green
        }
    }
}
