package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient

object CooldownTimer {
    private var cooldownEndTime: Long = 0
    private var isCooldownActive = false
    private var hasPlayedSound = false

    fun startCooldown(durationSeconds: Long) {
        val client = MinecraftClient.getInstance()
        if (client.world == null) return

        cooldownEndTime = System.currentTimeMillis() + (durationSeconds * 1000)
        isCooldownActive = true
        hasPlayedSound = false
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
        if (remaining <= 0) {
            if (!hasPlayedSound) {
                hasPlayedSound = true
                onCooldownComplete()
            }
            isCooldownActive = false
            return false
        }
        return true
    }

    private fun onCooldownComplete() {
        // Play sound
        SoundManager.playWarHornSound()

        // Send command
        val client = MinecraftClient.getInstance()
        client.player?.let { player ->
            if (ModConfig.customCommand.isNotEmpty()) {
                if (ModConfig.customCommand.startsWith("/")) {
                    player.networkHandler?.sendCommand(ModConfig.customCommand.substring(1))
                } else {
                    player.networkHandler?.sendChatMessage(ModConfig.customCommand)
                }
            }
        }
    }
}
