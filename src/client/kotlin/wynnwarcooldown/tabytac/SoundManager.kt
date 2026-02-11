package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

object SoundManager {
    private var warHornEvent: SoundEvent? = null

    fun registerSoundEvent() {
        val soundId = Identifier.of("wynn-war-cooldown", "war_horn")
        val soundEvent = SoundEvent.of(soundId)
        Registry.register(Registries.SOUND_EVENT, soundId, soundEvent)
        warHornEvent = soundEvent
    }

    fun playCooldownSound() {
        playSound(ModConfig.selectedSound)
    }

    fun playSound(soundType: SoundType) {
        val client = MinecraftClient.getInstance()
        val world = client.world ?: return
        val player = client.player ?: return

        val soundId = getSoundEventForType(soundType) ?: return

        world.playSound(
            player.x,
            player.y,
            player.z,
            soundId,
            SoundCategory.PLAYERS,
            ModConfig.soundVolume,
            1.0f,
            false
        )
    }

    private fun getSoundEventForType(soundType: SoundType): SoundEvent? {
        return when (soundType) {
            SoundType.WAR_HORN -> warHornEvent
            SoundType.EXPERIENCE_ORB -> Registries.SOUND_EVENT.get(Identifier.tryParse("minecraft:entity.experience_orb.pickup"))
            SoundType.BELL -> Registries.SOUND_EVENT.get(Identifier.tryParse("minecraft:block.bell.use"))
            SoundType.LEVEL_UP -> Registries.SOUND_EVENT.get(Identifier.tryParse("minecraft:entity.player.levelup"))
        }
    }
}
