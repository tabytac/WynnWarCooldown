package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient

object CooldownTimer {
    private var cooldownEndTime: Long = 0
    private var isCooldownActive = false
    private var hasSoundPlayed = false
    private var hasCommandExecuted = false

    fun startCooldown(durationSeconds: Long) {
        require(durationSeconds > 0) { "Cooldown duration must be positive" }

        val client = MinecraftClient.getInstance()
        if (client.world == null) return

        cooldownEndTime = System.currentTimeMillis() + (durationSeconds * 1000)
        isCooldownActive = true
        hasSoundPlayed = false
        hasCommandExecuted = false
    }

    fun getRemainingSeconds(): Long {
        if (!isCooldownActive) return 0

        val remaining = (cooldownEndTime - System.currentTimeMillis()) / 1000
        return remaining.coerceAtLeast(0)
    }

    fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    fun isActive(): Boolean {
        if (!isCooldownActive) return false

        val remaining = getRemainingSeconds()

        // Check sound play offset
        if (!hasSoundPlayed && remaining <= ModConfig.soundPlayOffsetSeconds) {
            hasSoundPlayed = true
            SoundManager.playCooldownSound()
        }

        // Check command execution offset
        if (!hasCommandExecuted && remaining <= ModConfig.commandExecutionOffsetSeconds) {
            hasCommandExecuted = true
            executeCommand()
        }

        if (remaining <= 0) {
            isCooldownActive = false
            return false
        }
        return true
    }

    private fun executeCommand() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val command = ModConfig.customCommand.takeIf { it.isNotEmpty() } ?: return

        val actualCommand = if (command.startsWith("/")) {
            command.substring(1)
        } else {
            command
        }

        player.networkHandler?.sendCommand(actualCommand)
    }
}
