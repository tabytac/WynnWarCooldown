package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Formatting
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

        val visibleTimers = CooldownTimer.getVisibleTimers()
        if (visibleTimers.isEmpty()) return

        val client = MinecraftClient.getInstance()
        val scale = ModConfig.hudScale.coerceIn(SCALE_MIN, SCALE_MAX)

        val currentTerritory = TerritoryResolver.getCurrentTerritoryName()
        visibleTimers.forEachIndexed { index, timer ->
        val label = when (timer.type) {
                // VisibleTimerType.CAPTURE -> "${timer.territoryName}: ${CooldownTimer.formatTime(timer.seconds)}"  // commented out (Capture HUD disabled)
                VisibleTimerType.EXPIRED -> "${timer.territoryName}: Ready"
                else -> "${timer.territoryName}: ${CooldownTimer.formatTime(timer.seconds)}"
            }
            val yOffset = (index * (client.textRenderer.fontHeight + LINE_SPACING) * scale).roundToInt()
            renderTimerLine(drawContext, client, label, yOffset, scale, timer, currentTerritory)
        }
    }

    private fun renderTimerLine(
        drawContext: DrawContext,
        client: MinecraftClient,
        label: String,
        yOffset: Int,
        scale: Float,
        timer: VisibleTimer,
        currentTerritory: String?
    ) {
        val screenWidth = client.window.scaledWidth
        val screenHeight = client.window.scaledHeight
        val textWidth = client.textRenderer.getWidth(label)
        val textHeight = client.textRenderer.fontHeight

        val boxWidth = ((textWidth + (PADDING_X * 2)) * scale).roundToInt()
        val boxHeight = ((textHeight + (PADDING_Y * 2)) * scale).roundToInt()

        val rawX = when (ModConfig.hudAlignment) {
            HudAlignment.CENTER -> (screenWidth / 2f) - (boxWidth / 2f)
            HudAlignment.LEFT -> (screenWidth * ModConfig.hudXPercent)
            HudAlignment.RIGHT -> (screenWidth * (1f - ModConfig.hudXPercent)) - boxWidth
        }
        val baseY = screenHeight * ModConfig.hudYPercent
        val rawY = baseY + yOffset - (boxHeight / 2f)

        val x = rawX.toInt().coerceIn(0, screenWidth - boxWidth)
        val y = rawY.toInt().coerceIn(0, screenHeight - boxHeight)

        // Draw background box if enabled
        if (ModConfig.showBackgroundBox) {
            drawContext.fill(x, y, x + boxWidth, y + boxHeight, BACKGROUND_COLOR)
        }

        // Determine color: capture override, current override (only for non-capture), expired, or default
        val finalHex = when {
            // timer.type == VisibleTimerType.CAPTURE -> ModConfig.captureTextColorHex  // commented out (Capture HUD disabled)
            currentTerritory != null && timer.territoryName.equals(currentTerritory, ignoreCase = true) -> ModConfig.currentTextColorHex
            timer.seconds == 0L && timer.type == VisibleTimerType.EXPIRED -> ModConfig.expiredTextColorHex
            else -> ModConfig.textColorHex
        }

        val textColor = parseHexColor(finalHex)
        drawContext.matrices.push()
        drawContext.matrices.translate(
            (x + (PADDING_X * scale)).toDouble(),
            (y + (PADDING_Y * scale)).toDouble(),
            0.0
        )
        drawContext.matrices.scale(scale, scale, 1.0f)

        // val isCurrent = (timer.type != VisibleTimerType.CAPTURE) && (currentTerritory != null && timer.territoryName.equals(currentTerritory, ignoreCase = true))  // commented out (Capture HUD disabled)
        val isCurrent = (currentTerritory != null && timer.territoryName.equals(currentTerritory, ignoreCase = true))
        val textComponent: Text = if (isCurrent) {
            Text.literal(label).formatted(Formatting.BOLD)
        } else {
            Text.literal(label)
        }

        drawContext.drawTextWithShadow(
            client.textRenderer,
            textComponent,
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
