/*
 * This file is part of UI-Utils-Reborn - https://github.com/FlorianMichael/UI-Utils-Reborn
 * Copyright (C) 2022-2023 FlorianMichael/EnZaXD and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianmichael.uiutilsreborn.mixin;

import de.florianmichael.uiutilsreborn.UIUtilsReborn;
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

    @Shadow public abstract List<? extends Element> children();

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("RETURN"))
    public void hookFeatureButtons(MinecraftClient client, int width, int height, CallbackInfo ci) {
        if (!UIUtilsReborn.enabled) return;

        final List<ExploitButtonWidget> buttons = UIUtilsReborn.fromScreen((Screen) (Object) this);

        if (buttons.isEmpty()) return;

        int leftHeight = 0;
        int rightHeight = 0;
        for (ExploitButtonWidget next : buttons) {
            next.setX(next.getSide() == Side.LEFT ? UIUtilsReborn.BOUND : this.width - next.getWidth() - UIUtilsReborn.BOUND);
            next.setY(UIUtilsReborn.BOUND + (next.getSide() == Side.LEFT ? leftHeight : rightHeight));

            this.addDrawableChild(next);
            if (next.getSide() == Side.LEFT)
                leftHeight += UIUtilsReborn.BUTTON_DIFF;
            else
                rightHeight += UIUtilsReborn.BUTTON_DIFF;
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void hideFeatureButtons(CallbackInfo ci) {
        if (UIUtilsReborn.enabled) return;

        for (Element child : children()) {
            if (child instanceof ExploitButtonWidget buttonWidget) {
                buttonWidget.visible = false;
            }
        }
    }
}
