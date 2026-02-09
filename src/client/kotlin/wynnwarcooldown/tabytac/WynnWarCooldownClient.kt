package wynnwarcooldown.tabytac

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient

object WynnWarCooldownClient : ClientModInitializer {
	override fun onInitializeClient() {
		// Register sound event
		SoundManager.registerSoundEvent()

		// Register HUD render callback (deprecated API, suppressed)
		@Suppress("DEPRECATION")
		HudRenderCallback.EVENT.register { drawContext, _ ->
			val client = MinecraftClient.getInstance()
			if (client.player != null && !client.isPaused) {
				CooldownHUD.render(drawContext)
			}
		}
	}
}
