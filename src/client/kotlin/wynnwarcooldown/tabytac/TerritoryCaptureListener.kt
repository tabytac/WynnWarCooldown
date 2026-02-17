package wynnwarcooldown.tabytac

import net.minecraft.text.Text
import org.slf4j.LoggerFactory

object TerritoryCaptureListener {
    private val LOGGER = LoggerFactory.getLogger("WWC")
    private const val CAPTURE_PHRASE = "you have taken control of"

    private val AMPERSAND_COLOR_CODE = Regex("(?i)&[0-9a-fk-or]")
    private val SECTION_COLOR_CODE = Regex("(?i)§[0-9a-fk-or]")
    private val CUSTOM_FORMATTING_CODE = Regex("&\\{[^}]*\\}")
    private val PRIVATE_USE_CHARS = Regex("[\\p{Co}]")
    private val NON_TEXT_CHARS = Regex("[^\\p{L}\\p{N}\\p{P}\\p{Zs}]")
    private val WHITESPACE = Regex("\\s+")

    // Matches: "You have taken control of Iboju Village from [Sqm]!" or "You have taken control of Iboju Village!"
    private val CAPTURE_PATTERN = Regex("you have taken control of\\s+([^!\\n\\r]+?)(?:\\s+from\\s+\\[[^\\]]+\\])?!", RegexOption.IGNORE_CASE)

    fun onWynntilsChatMessage(message: Text) {
        if (!ModConfig.isModEnabled) return
        if (!ModConfig.enableCaptureReminder) return

        val messageText = normalizeChatMessage(message.string)
        if (!messageText.contains(CAPTURE_PHRASE, ignoreCase = true)) return

        val match = CAPTURE_PATTERN.find(messageText) ?: return
        val territoryName = match.groupValues[1].trim()

        LOGGER.info("Territory capture detected: {} — recording capture event.", territoryName)
        // Record capture reminder and start a default cooldown so HUD can report remaining time immediately
        CooldownTimer.recordCapture(territoryName)
    }

    private fun normalizeChatMessage(rawMessage: String): String {
        var messageText = rawMessage

        messageText = messageText.replace(AMPERSAND_COLOR_CODE, "")
        messageText = messageText.replace(SECTION_COLOR_CODE, "")
        messageText = messageText.replace(CUSTOM_FORMATTING_CODE, "")
        messageText = messageText.replace(PRIVATE_USE_CHARS, "")
        messageText = messageText.replace(NON_TEXT_CHARS, "")

        return messageText.replace(WHITESPACE, " ").trim()
    }
}
