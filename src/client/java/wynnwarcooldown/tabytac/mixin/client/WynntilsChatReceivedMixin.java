package wynnwarcooldown.tabytac.mixin.client;

import com.wynntils.mc.event.SystemMessageEvent;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wynnwarcooldown.tabytac.ChatListener;

@Mixin(SystemMessageEvent.ChatReceivedEvent.class)
public class WynntilsChatReceivedMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void wynnwarcooldown$onChatReceived(Text message, CallbackInfo ci) {
        ChatListener.INSTANCE.onWynntilsChatMessage(message);
    }
}
