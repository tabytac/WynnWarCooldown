package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

object SoundManager {
    private var warHornEvent: SoundEvent? = null

    // Register the sound event
    fun registerSoundEvent() {
        val soundId = Identifier.of("wynn-war-cooldown", "war_horn")
        val soundEvent = SoundEvent.of(soundId)
        Registry.register(Registries.SOUND_EVENT, soundId, soundEvent)
        warHornEvent = soundEvent
    }

    fun playWarHornSound() {
        val client = MinecraftClient.getInstance()
        client.world?.let { world ->
            client.player?.let { player ->
                val soundEvent = warHornEvent ?: return
                world.playSound(
                    player.x,
                    player.y,
                    player.z,
                    soundEvent,
                    SoundCategory.PLAYERS,
                    ModConfig.soundVolume,
                    1.0f,
                    false
                )
            }
        }
    }
}
