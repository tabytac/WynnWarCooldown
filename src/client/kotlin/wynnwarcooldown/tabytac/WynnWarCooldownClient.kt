package wynnwarcooldown.tabytac

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

object WynnWarCooldownClient : ClientModInitializer {
	private lateinit var toggleHudKeyBinding: KeyBinding
	private lateinit var toggleTrackingKeyBinding: KeyBinding

	override fun onInitializeClient() {
		// Load config from file
		ModConfig.load()

		// Register sound event
		SoundManager.registerSoundEvent()

		// Register client commands
		CommandManager.registerCommands()

		// Register keybind for toggling tracking (master on/off)
		toggleTrackingKeyBinding = KeyBindingHelper.registerKeyBinding(
			KeyBinding(
				"key.wynn-war-cooldown.toggle_tracking",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_PERIOD, // Default key: Period
				KeyBinding.Category.MISC
			)
		)

		// Register keybind for toggling HUD
		toggleHudKeyBinding = KeyBindingHelper.registerKeyBinding(
			KeyBinding(
				"key.wynn-war-cooldown.toggle_hud",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_COMMA, // Default key: Comma
				KeyBinding.Category.MISC
			)
		)

		// Register keybind handler
		ClientTickEvents.END_CLIENT_TICK.register { client ->
				// Update timers on tick (moved off HUD render path)
				if (ModConfig.isModEnabled) {
					CooldownTimer.updateTimers()
				}

				while (toggleHudKeyBinding.wasPressed()) {
				ModConfig.showTimerHud = !ModConfig.showTimerHud
				ModConfig.save()
				val status = if (ModConfig.showTimerHud) "visible" else "hidden"
				client.inGameHud?.chatHud?.addMessage(net.minecraft.text.Text.translatable("wynn-war-cooldown.chat.timer_hud_status", status))
			}

				while (toggleTrackingKeyBinding.wasPressed()) {
					ModConfig.isModEnabled = !ModConfig.isModEnabled
					ModConfig.save()
					val status = if (ModConfig.isModEnabled) "enabled" else "disabled"
					client.inGameHud?.chatHud?.addMessage(net.minecraft.text.Text.translatable("wynn-war-cooldown.chat.tracking_status", status))
				}
		}

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
