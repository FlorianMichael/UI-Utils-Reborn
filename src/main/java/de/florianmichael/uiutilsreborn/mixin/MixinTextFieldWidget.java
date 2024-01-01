/*
 * This file is part of UI-Utils-Reborn - https://github.com/FlorianMichael/UI-Utils-Reborn
 * Copyright (C) 2022-2024 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and contributors
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

import de.florianmichael.uiutilsreborn.util.ITextFieldAdapter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextFieldWidget.class)
public abstract class MixinTextFieldWidget extends ClickableWidget implements ITextFieldAdapter {

    @Shadow @Final private TextRenderer textRenderer;

    @Unique
    private Text uiUtilsReborn$sideInformation;

    public MixinTextFieldWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("RETURN"))
    private void hookCustomSideInformation(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.uiUtilsReborn$sideInformation != null) {
            context.drawTextWithShadow(textRenderer, this.uiUtilsReborn$sideInformation, this.getX() - textRenderer.getWidth(this.uiUtilsReborn$sideInformation.getString()) - 10, this.getY() + this.getHeight() / 4, -1);
        }
    }

    @Override
    public void uiUtilsReborn$setSideInformation(String information) {
        this.uiUtilsReborn$sideInformation = Text.literal(information);
    }

}
