package de.florianmichael.uiutilsreborn.gui;

import de.florianmichael.uiutilsreborn.UIUtils;
import de.florianmichael.uiutilsreborn.util.ITextFieldAdapter;
import de.florianmichael.uiutilsreborn.widget.DropboxWidget;
import de.florianmichael.uiutilsreborn.widget.ExploitButtonWidget;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

public class FabricateScreen extends Screen {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 15;

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

        this.addDrawableChild(new ButtonWidget(UIUtils.BOUND, this.height - ExploitButtonWidget.DEFAULT_HEIGHT - UIUtils.BOUND, 98, ExploitButtonWidget.DEFAULT_HEIGHT, Text.literal(
                this.currentPacket.getDisplay()
        ), button -> {
            if (this.currentPacket == CurrentPacket.CLICK_SLOT)
                this.currentPacket = CurrentPacket.BUTTON_CLICK;
            else
                this.currentPacket = CurrentPacket.CLICK_SLOT;

            this.clearChildren();
            this.init();

            button.setMessage(Text.literal(this.currentPacket.getDisplay()));
        }));

        this.initButtons(startX);

        this.addDrawableChild(new ButtonWidget(this.width / 2 - (DEFAULT_WIDTH / 4), this.height - ExploitButtonWidget.DEFAULT_HEIGHT - UIUtils.BOUND, DEFAULT_WIDTH / 2, ExploitButtonWidget.DEFAULT_HEIGHT, Text.translatable("fabricate.ui-utils-reborn.send"), button1 -> {
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
                this.status = Formatting.GREEN + Text.translatable("fabricate.ui-utils-reborn.success").getString();
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
                this.status = Formatting.GREEN + Text.translatable("fabricate.ui-utils-reborn.success").getString();
            }
        }));
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
        y += UIUtils.BUTTON_DIFF;
        ((ITextFieldAdapter) this.syncID).setSideInformation("Sync ID");
        this.addDrawableChild(this.syncID);

        if (this.currentPacket == CurrentPacket.CLICK_SLOT) {
            this.revision = new TextFieldWidget(textRenderer, startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Text.empty());
            y += UIUtils.BUTTON_DIFF;
            ((ITextFieldAdapter) this.revision).setSideInformation("Revision");
            this.addDrawableChild(this.revision);

            this.slot = new TextFieldWidget(textRenderer, startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Text.empty());
            y += UIUtils.BUTTON_DIFF;
            ((ITextFieldAdapter) this.slot).setSideInformation("Slot");
            this.addDrawableChild(this.slot);

            this.button = new TextFieldWidget(textRenderer, startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Text.empty());
            y += UIUtils.BUTTON_DIFF;
            ((ITextFieldAdapter) this.button).setSideInformation("Button");
            this.addDrawableChild(this.button);

            this.action = new DropboxWidget(startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_HEIGHT, 0, Arrays.stream(SlotActionType.values()).map(Enum::name).toList());
        } else {
            this.buttonID = new TextFieldWidget(textRenderer, startX, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Text.empty());
            ((ITextFieldAdapter) this.buttonID).setSideInformation("Button ID");
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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        matrices.push();
        matrices.translate(0, 0, -100);
        this.parent.render(matrices, -1, -1, delta);
        matrices.pop();

        matrices.push();
        matrices.translate(0, 0, 900);
        this.renderBackground(matrices);

        matrices.push();
        matrices.scale(2F, 2F, 2F);
        drawCenteredTextWithShadow(matrices, textRenderer, Text.literal(this.currentPacket.getDisplay()).asOrderedText(), this.width / 4, 2, -1);
        matrices.pop();

        if (this.action != null)
            this.action.render(matrices);

        if (this.status != null)
            this.textRenderer.drawWithShadow(matrices, this.status, 0, 0, -1);

        super.render(matrices, mouseX, mouseY, delta);
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
