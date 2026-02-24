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
    private const val LINE_SPACING = 2
    private const val SCALE_MIN = 0.1f
    private const val SCALE_MAX = 5.0f

    fun render(drawContext: DrawContext) {
        if (!ModConfig.isModEnabled || !ModConfig.showTimerHud) return

        val visibleTimers = CooldownTimer.getVisibleTimers()
        if (visibleTimers.isEmpty()) return

        val client = MinecraftClient.getInstance()
        val scale = ModConfig.hudScale.coerceIn(SCALE_MIN, SCALE_MAX)
        val currentTerritory = TerritoryResolver.getCurrentTerritoryName()

        // First pass: Build labels and calculate max width for unified box
        val timerLabels = visibleTimers.map { timer ->
            when (timer.type) {
                // VisibleTimerType.CAPTURE -> "${timer.territoryName}: ${CooldownTimer.formatTime(timer.seconds)}"  // commented out (Capture HUD disabled)
                VisibleTimerType.EXPIRED -> "${timer.territoryName}: Ready"
                else -> "${timer.territoryName}: ${CooldownTimer.formatTime(timer.seconds)}"
            }
        }

        val maxTextWidth = timerLabels.maxOfOrNull { client.textRenderer.getWidth(it) } ?: return
        val textHeight = client.textRenderer.fontHeight

        val screenWidth = client.window.scaledWidth
        val screenHeight = client.window.scaledHeight

        // Calculate unified box dimensions
        val boxWidth = ((maxTextWidth + (PADDING_X * 2)) * scale).roundToInt()
        val totalHeight = (visibleTimers.size * textHeight + (visibleTimers.size - 1) * LINE_SPACING)
        val boxHeight = ((totalHeight + (PADDING_Y * 2)) * scale).roundToInt()

        // Position the unified box - hudXPercent determines different anchor points based on alignment
        val baseX = screenWidth * ModConfig.hudXPercent
        val rawX = when (ModConfig.hudAlignment) {
            HudAlignment.LEFT -> baseX  // hudXPercent is top-left corner
            HudAlignment.CENTER -> baseX - (boxWidth / 2f)  // hudXPercent is top-center
            HudAlignment.RIGHT -> baseX - boxWidth  // hudXPercent is top-right corner
        }
        val baseY = screenHeight * ModConfig.hudYPercent
        val x = rawX.toInt().coerceIn(0, screenWidth - boxWidth)
        val y = (baseY - (boxHeight / 2f)).toInt().coerceIn(0, screenHeight - boxHeight)

        // Draw unified background box if enabled
        if (ModConfig.showBackgroundBox) {
            drawContext.fill(x, y, x + boxWidth, y + boxHeight, BACKGROUND_COLOR)
        }

        // Second pass: Render each timer line within the unified box
        visibleTimers.forEachIndexed { index, timer ->
            val label = timerLabels[index]
            val textWidth = client.textRenderer.getWidth(label)
            val yOffset = (index * (textHeight + LINE_SPACING) * scale).roundToInt()

            // Determine color
            val finalHex = when {
                currentTerritory != null && timer.territoryName.equals(currentTerritory, ignoreCase = true) -> ModConfig.currentTextColorHex
                timer.seconds == 0L && timer.type == VisibleTimerType.EXPIRED -> ModConfig.expiredTextColorHex
                else -> ModConfig.textColorHex
            }
            val textColor = parseHexColor(finalHex)

            val isCurrent = currentTerritory != null && timer.territoryName.equals(currentTerritory, ignoreCase = true)
            val textComponent: Text = if (isCurrent) {
                Text.literal(label).formatted(Formatting.BOLD)
            } else {
                Text.literal(label)
            }

            // Text position within unified box based on alignment setting
            val textX = when (ModConfig.hudAlignment) {
                HudAlignment.LEFT -> (x + (PADDING_X * scale)).toInt()
                HudAlignment.CENTER -> (x + (boxWidth / 2f) - (textWidth * scale / 2f)).toInt()
                HudAlignment.RIGHT -> (x + boxWidth - (textWidth * scale) - (PADDING_X * scale)).toInt()
            }
            val textY = (y + (PADDING_Y * scale) + yOffset).toInt()

            drawContext.drawTextWithShadow(
                client.textRenderer,
                textComponent,
                textX,
                textY,
                textColor
            )
        }
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
