package wynnwarcooldown.tabytac

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient

object WynnWarCooldownClient : ClientModInitializer {
	override fun onInitializeClient() {
		// Load config from file
		ModConfig.load()

		// Register sound event
		SoundManager.registerSoundEvent()

		// Register client commands
		CommandManager.registerCommands()

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
