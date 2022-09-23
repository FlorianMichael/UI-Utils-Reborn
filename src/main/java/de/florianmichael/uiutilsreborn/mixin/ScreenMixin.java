package de.florianmichael.uiutilsreborn.mixin;

import de.florianmichael.uiutilsreborn.ExploitButton;
import de.florianmichael.uiutilsreborn.UIUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement);

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("RETURN"))
    public void hookFeatureButtons(MinecraftClient client, int width, int height, CallbackInfo ci) {
        final List<ExploitButton> buttons = UIUtils.fromScreen((Screen) (Object) this);

        if (buttons.isEmpty()) return;

        for (int i = 0; i < buttons.size(); i++) {
            final ExploitButton next = buttons.get(i);

            this.addDrawableChild(next.create(5, 5 + (i * UIUtils.BUTTON_DIFF), (Screen) (Object) this));
        }
    }
}
