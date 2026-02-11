package wynnwarcooldown.tabytac

import net.minecraft.text.Text
import org.slf4j.LoggerFactory

object TerritoryLossListener {
    private val LOGGER = LoggerFactory.getLogger("WWC")
    private const val TERRITORY_LOSS_PHRASE = "has taken control of"
    private const val COOLDOWN_DURATION_SECONDS = 600L // 10 minutes

    private val AMPERSAND_COLOR_CODE = Regex("(?i)&[0-9a-fk-or]")
    private val SECTION_COLOR_CODE = Regex("(?i)§[0-9a-fk-or]")
    private val CUSTOM_FORMATTING_CODE = Regex("&\\{[^}]*\\}")
    private val PRIVATE_USE_CHARS = Regex("[\\p{Co}]")
    private val NON_TEXT_CHARS = Regex("[^\\p{L}\\p{N}\\p{P}\\p{Zs}]")
    private val WHITESPACE = Regex("\\s+")

    // Pattern to extract territory name and guild tag
    // Expected format: [GUILD_TAG] has taken control of Territory Name!
    private val TERRITORY_LOSS_PATTERN = Regex("\\[([A-Za-z]{3,4})\\]\\s+has taken control of\\s+([^!]+)!", RegexOption.IGNORE_CASE)

    fun onWynntilsChatMessage(message: Text) {
        if (!ModConfig.isModEnabled) return

        val messageText = normalizeChatMessage(message.string)
        if (!messageText.contains(TERRITORY_LOSS_PHRASE, ignoreCase = true)) return

        val match = TERRITORY_LOSS_PATTERN.find(messageText) ?: return
        val guildTag = match.groupValues[1]
        val territoryName = match.groupValues[2].trim()

        LOGGER.info("Territory loss detected: {} was taken by guild [{}]. Starting 10-minute cooldown.", territoryName, guildTag)
        CooldownTimer.startCooldown(COOLDOWN_DURATION_SECONDS, territoryName)
    }

    private fun normalizeChatMessage(rawMessage: String): String {
        var messageText = rawMessage

        // Strip standard and custom formatting codes (Wynntils/Wynncraft often inject these).
        messageText = messageText.replace(AMPERSAND_COLOR_CODE, "")
        messageText = messageText.replace(SECTION_COLOR_CODE, "")
        messageText = messageText.replace(CUSTOM_FORMATTING_CODE, "")

        // Remove private-use glyphs used for custom fonts/icons.
        messageText = messageText.replace(PRIVATE_USE_CHARS, "")

        // Remove any remaining non-text symbols.
        messageText = messageText.replace(NON_TEXT_CHARS, "")

        // Normalize whitespace (newlines, tabs, multiple spaces, etc.).
        return messageText.replace(WHITESPACE, " ").trim()
    }
}
