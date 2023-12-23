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

package de.florianmichael.uiutilsreborn.gui;

import de.florianmichael.uiutilsreborn.UIUtilsReborn;
import de.florianmichael.uiutilsreborn.util.ITextFieldAdapter;
import de.florianmichael.uiutilsreborn.widget.DropboxWidget;
import de.florianmichael.uiutilsreborn.widget.ExploitButtonWidget;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

public class FabricateScreen extends Screen {

    private final static int DEFAULT_WIDTH = 300;
    private final static int DEFAULT_HEIGHT = 20;

    private final Screen parent;
    private CurrentPacket currentPacket = CurrentPacket.CLICK_SLOT;

    // General
    private TextFieldWidget syncID;

    // Click Slot
    private TextFieldWidget revision;
    private TextFieldWidget slot;
    private TextFieldWidget button;
    private DropboxWidget action;

    // Button Click
    private TextFieldWidget buttonID;

    private String status;

    public FabricateScreen(final Screen parent) {
        super(Text.literal("Fabricate"));

        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        final int startX = this.width / 2 - (DEFAULT_WIDTH / 2);

        this.addDrawableChild(ButtonWidget.builder(Text.literal(
                this.currentPacket.getDisplay()
        ), button -> {
            if (this.currentPacket == CurrentPacket.CLICK_SLOT)
                this.currentPacket = CurrentPacket.BUTTON_CLICK;
            else
                this.currentPacket = CurrentPacket.CLICK_SLOT;

            this.clearChildren();
            this.init();

            button.setMessage(Text.literal(this.currentPacket.getDisplay()));
        }).dimensions(UIUtilsReborn.BOUND, this.height - ExploitButtonWidget.DEFAULT_HEIGHT - UIUtilsReborn.BOUND, 98, ExploitButtonWidget.DEFAULT_HEIGHT).build());

        this.initButtons(startX);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("fabricate.ui-utils-reborn.send"), button1 -> {
            if (this.currentPacket == CurrentPacket.CLICK_SLOT) {
                if (this.syncID.getText().trim().isEmpty() || this.revision.getText().trim().isEmpty() || this.slot.getText().trim().isEmpty() || this.button.getText().trim().isEmpty()) {
                    this.status = Formatting.RED + Text.translatable("fabricate.ui-utils-reborn.invalid").getString();
                    return;
                }

                final Integer syncID = this.isInt(this.syncID.getText().trim());
                final Integer revision = this.isInt(this.revision.getText().trim());
                final Integer slot = this.isInt(this.slot.getText().trim());
                final Integer button = this.isInt(this.button.getText().trim());

                if (syncID == null || revision == null || slot == null || button == null) {
                    this.status = Formatting.RED + Text.translatable("fabricate.ui-utils-reborn.invalid").getString();
                    return;
                }

                client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(syncID, revision, slot, button, SlotActionType.values()[this.action.selected], ItemStack.EMPTY, new Int2ObjectArrayMap<>()));
            } else {
                if (this.syncID.getText().trim().isEmpty() || this.buttonID.getText().trim().isEmpty()) {
                    this.status = Formatting.RED + Text.translatable("fabricate.ui-utils-reborn.invalid").getString();
                    return;
                }

                final Integer syncID = this.isInt(this.syncID.getText().trim());
                final Integer buttonID = this.isInt(this.button.getText().trim());

                if (syncID == null || buttonID == null) {
                    this.status = Formatting.RED + Text.translatable("fabricate.ui-utils-reborn.invalid").getString();
                    return;
                }

                client.getNetworkHandler().sendPacket(new ButtonClickC2SPacket(syncID, buttonID));
            }
            this.status = Formatting.GREEN + Text.translatable("fabricate.ui-utils-reborn.success").getString();
        }).dimensions(this.width / 2 - (DEFAULT_WIDTH / 4), this.height - ExploitButtonWidget.DEFAULT_HEIGHT - UIUtilsReborn.BOUND, DEFAULT_WIDTH / 2, ExploitButtonWidget.DEFAULT_HEIGHT).build());
    }

    private Integer isInt(final String input) {
        try {
            return Integer.parseInt(input);
        } catch (Exception e) {
            return null;
        }
    }

    private void initButtons(final int startX) {
        int y = 25;

        this.syncID = new TextFieldWidget(textRenderer, startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Text.empty());
        y += UIUtilsReborn.BUTTON_DIFF;
        ((ITextFieldAdapter) this.syncID).uiUtilsReborn$setSideInformation("Sync ID");
        this.addDrawableChild(this.syncID);

        if (this.currentPacket == CurrentPacket.CLICK_SLOT) {
            this.revision = new TextFieldWidget(textRenderer, startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Text.empty());
            y += UIUtilsReborn.BUTTON_DIFF;
            ((ITextFieldAdapter) this.revision).uiUtilsReborn$setSideInformation("Revision");
            this.addDrawableChild(this.revision);

            this.slot = new TextFieldWidget(textRenderer, startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Text.empty());
            y += UIUtilsReborn.BUTTON_DIFF;
            ((ITextFieldAdapter) this.slot).uiUtilsReborn$setSideInformation("Slot");
            this.addDrawableChild(this.slot);

            this.button = new TextFieldWidget(textRenderer, startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Text.empty());
            y += UIUtilsReborn.BUTTON_DIFF;
            ((ITextFieldAdapter) this.button).uiUtilsReborn$setSideInformation("Button");
            this.addDrawableChild(this.button);

            this.action = new DropboxWidget(startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_HEIGHT, 0, Arrays.stream(SlotActionType.values()).map(Enum::name).toList());
        } else {
            this.buttonID = new TextFieldWidget(textRenderer, startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Text.empty());
            ((ITextFieldAdapter) this.buttonID).uiUtilsReborn$setSideInformation("Button ID");
            this.addDrawableChild(this.buttonID);

            this.action = null;
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);

        this.parent.resize(client, width, height);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.action != null)
            this.action.mouseClicked(mouseX, mouseY);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        final var matrices = drawContext.getMatrices();

        matrices.push();
        matrices.translate(0, 0, -100);
        this.parent.render(drawContext, -1, -1, delta);
        matrices.pop();

        matrices.push();
        matrices.translate(0, 0, 900);
        super.render(drawContext, mouseX, mouseY, delta);

        matrices.push();
        matrices.scale(2F, 2F, 2F);
        drawContext.drawCenteredTextWithShadow(textRenderer, Text.literal(this.currentPacket.getDisplay()).asOrderedText(), this.width / 4, 2, -1);
        matrices.pop();

        if (this.action != null)
            this.action.render(drawContext);

        if (this.status != null)
            drawContext.drawTextWithShadow(textRenderer, this.status, 0, 0, -1);
        matrices.pop();
    }

    public enum CurrentPacket {
        CLICK_SLOT("Click Slot"),
        BUTTON_CLICK("Button Click");

        private final String display;

        CurrentPacket(final String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
}
