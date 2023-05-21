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

package de.florianmichael.uiutilsreborn.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.List;

public class DropboxWidget extends DrawableHelper {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    private final int entryHeight;

    public final List<String> options;

    public int selected;

    private boolean open;

    public DropboxWidget(final int x, final int y, final int width, final int height, final int entryHeight, final int selected, final List<String> options) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.entryHeight = entryHeight;
        this.selected = selected;

        this.options = options;
    }

    private boolean isInBounds(final double mouseX, final double mouseY, final int x, final int y, final int width, final int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void mouseClicked(final double mouseX, final double mouseY) {
        if (isInBounds(mouseX, mouseY, this.x, this.y, this.width, this.height))
            this.toggle();

        if (this.isOpen()) {
            int bypass = this.y + this.height - 1;
            for (String option : this.options) {
                if (isInBounds(mouseX, mouseY, this.x, bypass, this.width, this.entryHeight)) {
                    this.selected = this.options.indexOf(option);
                    this.toggle();
                }
                bypass += this.entryHeight;
            }
        }
    }

    // Thank you Lucy
    private void drawRectBorder(MatrixStack matrices, int left, int top, int right, int bottom) {
        DrawableHelper.fill(matrices, left - 1, top - 1, left, bottom + 1, -1);
        DrawableHelper.fill(matrices, right, top - 1, right + 1, bottom + 1, -1);
        DrawableHelper.fill(matrices, left, top - 1, right, top, -1);
        DrawableHelper.fill(matrices, left, bottom, right, bottom + 1, -1);
    }

    public void render(final MatrixStack matrices) {
        DrawableHelper.fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, Color.BLACK.getRGB());

        final String text = this.isOpen() ? "↓" : "←";

        MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, text, (this.x + this.width) - MinecraftClient.getInstance().textRenderer.getWidth(text), this.y + (this.height / 2F) - 4, -1);
        drawRectBorder(matrices, this.x, this.y, this.x + this.width, this.y + this.height);

        drawCenteredTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, Text.literal(this.options.get(this.selected)), this.x + (this.width / 2), this.y + (this.height / 2) - 4, -1);

        if (this.isOpen()) {
            DrawableHelper.fill(matrices, this.x, this.y + this.height, this.x + this.width, this.y + this.height + (this.options.size() * this.entryHeight), Color.BLACK.getRGB());
            drawRectBorder(matrices, this.x, this.y + this.height + 1, this.x + this.width, this.y + this.height + (this.options.size() * this.entryHeight));

            int bypass = this.height;
            for (String option : this.options) {
                drawCenteredTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, Text.literal(option), this.x + this.width / 2, bypass + this.y + 1 + (this.entryHeight / 4), -1);
                bypass += this.entryHeight;
            }
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void toggle() {
        this.open = !this.open;
    }
}
