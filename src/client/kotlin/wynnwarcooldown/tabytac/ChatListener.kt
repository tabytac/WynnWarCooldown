package wynnwarcooldown.tabytac

import net.minecraft.text.Text
import org.slf4j.LoggerFactory

object ChatListener {
    private val LOGGER = LoggerFactory.getLogger("WWC")
    private const val COOLDOWN_PHRASE = "territory is in cooldown"
    private const val DEFENSE_PHRASE = "defense is"
    private val MINUTES_PATTERN = Regex("(\\d+)\\s*minute[s]?", RegexOption.IGNORE_CASE)
    private val SECONDS_PATTERN = Regex("(\\d+)\\s*second[s]?", RegexOption.IGNORE_CASE)

    // Pattern to extract territory name from defense status messages
    // Expected format: <Territory Name> defense is Very Low/Low/Medium/High/Very High
    private val DEFENSE_STATUS_PATTERN = Regex("^(.+?)\\s+defense is\\s+(?:Very Low|Low|Medium|High|Very High)", RegexOption.IGNORE_CASE)

    private val AMPERSAND_COLOR_CODE = Regex("(?i)&[0-9a-fk-or]")
    private val SECTION_COLOR_CODE = Regex("(?i)§[0-9a-fk-or]")
    private val CUSTOM_FORMATTING_CODE = Regex("&\\{[^}]*\\}")
    private val PRIVATE_USE_CHARS = Regex("[\\p{Co}]")
    private val NON_TEXT_CHARS = Regex("[^\\p{L}\\p{N}\\p{P}\\p{Zs}]")
    private val WHITESPACE = Regex("\\s+")

    fun onWynntilsChatMessage(message: Text) {
        if (!ModConfig.isModEnabled) return

        val messageText = normalizeChatMessage(message.string)

        // Check for territory defense status (queue detection)
        if (ModConfig.removeTimerOnQueue && messageText.contains(DEFENSE_PHRASE, ignoreCase = true)) {
            val defenseMatch = DEFENSE_STATUS_PATTERN.find(messageText)
            if (defenseMatch != null) {
                val territoryName = defenseMatch.groupValues[1].trim()
                if (CooldownTimer.removeCooldown(territoryName)) {
                    LOGGER.info("Removed timer for {} due to defense status message (territory queued)", territoryName)
                }
                return
            }
        }

        // Check for cooldown message
        if (!messageText.contains(COOLDOWN_PHRASE, ignoreCase = true)) return

        val minutes = MINUTES_PATTERN.find(messageText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val seconds = SECONDS_PATTERN.find(messageText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val totalSeconds = minutes * 60 + seconds

        if (totalSeconds > 0) {
            val adjustedSeconds = (totalSeconds + ModConfig.timerOffsetSeconds).coerceAtLeast(0).toLong()

            val territoryName = TerritoryResolver.getCurrentTerritoryName() ?: "Unknown Territory"
            LOGGER.info("War cooldown detected: {} minutes {} seconds ({}s total, adjusted to {}s) in territory: {}",
                minutes, seconds, totalSeconds, adjustedSeconds, territoryName)
            CooldownTimer.startCooldown(adjustedSeconds, territoryName)
        }
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
