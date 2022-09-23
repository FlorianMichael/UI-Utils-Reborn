package de.florianmichael.uiutilsreborn.mixin;

import de.florianmichael.uiutilsreborn.UIUtils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketCallbacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "sendImmediately", at = @At("HEAD"), cancellable = true)
    public void hookExploitCancels(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo ci) {
        if (UIUtils.shouldCancel(packet)) ci.cancel();
    }
}
