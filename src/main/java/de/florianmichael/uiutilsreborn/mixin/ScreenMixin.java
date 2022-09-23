package de.florianmichael.uiutilsreborn.mixin;

import de.florianmichael.uiutilsreborn.UIUtils;
import de.florianmichael.uiutilsreborn.util.Side;
import de.florianmichael.uiutilsreborn.widget.ExploitButtonWidget;
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

    @Shadow public int width;

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("RETURN"))
    public void hookFeatureButtons(MinecraftClient client, int width, int height, CallbackInfo ci) {
        final List<ExploitButtonWidget> buttons = UIUtils.fromScreen((Screen) (Object) this);

        if (buttons.isEmpty()) return;

        int leftHeight = 0;
        int rightHeight = 0;
        for (ExploitButtonWidget next : buttons) {
            next.x = next.getSide() == Side.LEFT ? UIUtils.BOUND : this.width - next.getWidth() - UIUtils.BOUND;
            next.y = UIUtils.BOUND + (next.getSide() == Side.LEFT ? leftHeight : rightHeight);

            this.addDrawableChild(next);
            if (next.getSide() == Side.LEFT)
                leftHeight += UIUtils.BUTTON_DIFF;
            else
                rightHeight += UIUtils.BUTTON_DIFF;
        }
    }
}
