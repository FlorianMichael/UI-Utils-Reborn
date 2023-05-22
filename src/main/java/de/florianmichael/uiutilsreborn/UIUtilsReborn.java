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

package de.florianmichael.uiutilsreborn;

import com.google.common.collect.Lists;
import de.florianmichael.uiutilsreborn.gui.FabricateScreen;
import de.florianmichael.uiutilsreborn.util.Side;
import de.florianmichael.uiutilsreborn.widget.ExploitButtonWidget;
import de.florianmichael.uiutilsreborn.widget.ToggleableExploitButtonWidget;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;

import java.util.*;

public class UIUtilsReborn implements ClientModInitializer {
    private static boolean enabled = true;

    public final static int BOUND = 5;
    public final static int BUTTON_DIFF = ExploitButtonWidget.DEFAULT_HEIGHT + 3;

    private final static List<Pair<ExploitButtonWidget, Class<? extends  Screen>>> exploitTracker = new ArrayList<>();
    private final static List<Packet<?>> delayedUIPackets = new ArrayList<>();

    public static ToggleableExploitButtonWidget shulkerDupe = null;
    public static ToggleableExploitButtonWidget shulkerDupeMulti = null;

    private static boolean cancelSignPackets;

    private static ToggleableExploitButtonWidget shouldCancelUIPackets = null;
    private static ToggleableExploitButtonWidget shouldDelayUIPackets = null;

    private static Screen storedScreen = null;
    private static ScreenHandler storedScreenHandler = null;

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean handleChatMessages(final String chatText) {
        if (chatText.equals("$ui-utils-reborn")) {
            UIUtilsReborn.enabled = !UIUtilsReborn.enabled;

            assert MinecraftClient.getInstance().player != null;
            MinecraftClient.getInstance().player.sendMessage(Text.of((UIUtilsReborn.enabled ? Formatting.GREEN : Formatting.RED) + "UI-Utils-Reborn is now " + (UIUtilsReborn.enabled ? "enabled" : "disabled")));
            return true;
        }
        return false;
    }

    @Override
    public void onInitializeClient() {
        final MinecraftClient mc = MinecraftClient.getInstance();

        // Some Random GUIs
        hookFeature(SleepingChatScreen.class, new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.client-wake-up"), Side.LEFT, (b) -> {
            assert mc.player != null;

            mc.player.wakeUp();
            mc.setScreen(null);
        }));
        hookFeature(SignEditScreen.class, new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.client-side-close"), Side.LEFT, (b) -> {
            mc.setScreen(null);

            UIUtilsReborn.cancelSignPackets = true;
        }));
        hookFeature(DeathScreen.class, new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.force-respawn"), Side.LEFT, b -> {
            assert mc.player != null;

            mc.player.requestRespawn();
            mc.setScreen(null);
        }));

        // Handled Screen Hooks
        final List<ExploitButtonWidget> exploits = new ArrayList<>();

        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.client-side-close"), Side.LEFT, button -> mc.setScreen(null)));
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.server-side-close"), Side.LEFT, button -> {
            assert mc.player != null;

            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        }));
        exploits.add(shouldCancelUIPackets = new ToggleableExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.cancel-packets"), Side.LEFT, button -> {}, false));
        exploits.add(shouldDelayUIPackets = new ToggleableExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.delay-packets"), Side.LEFT, button -> {
            if (!shouldDelayUIPackets.isToggled() && !delayedUIPackets.isEmpty()) {
                for (Packet<?> packet : delayedUIPackets)
                    Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);

                delayedUIPackets.clear();
            }
        }, false));
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.disconnect"), Side.LEFT, button -> {
            if (!delayedUIPackets.isEmpty()) {
                if (shouldCancelUIPackets.isToggled()) shouldDelayUIPackets.toggle();

                for (Packet<?> packet : delayedUIPackets)
                    Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);

                mc.getNetworkHandler().getConnection().disconnect(Text.literal("Connection closed (UI Utils Reborn)"));
                delayedUIPackets.clear();
            }
        }));

        // Save and Load GUI
        final ExploitButtonWidget load = new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.load"), Side.LEFT, button -> {
            if (storedScreen != null && storedScreenHandler != null) {
                mc.setScreen(storedScreen);
                mc.player.currentScreenHandler = storedScreenHandler;
            }
            button.active = false;
        });
        load.active = false;
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.save"), Side.LEFT, button -> {
            storedScreen = mc.currentScreen;

            assert mc.player != null;
            storedScreenHandler = mc.player.currentScreenHandler;
            load.active = true;
        }));
        exploits.add(load);

        // Information Copy Buttons
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.sid"), Side.RIGHT, b -> {
            assert mc.player != null;

            mc.keyboard.setClipboard(String.valueOf(mc.player.currentScreenHandler.syncId));
        }));
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.rev"), Side.RIGHT, b -> {
            assert mc.player != null;

            mc.keyboard.setClipboard(String.valueOf(mc.player.currentScreenHandler.getRevision()));
        }));
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.title"), Side.RIGHT, b -> MinecraftClient.getInstance().keyboard.setClipboard(Text.Serializer.toJson(mc.currentScreen.getTitle()))));

        // Packet Fabrication
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.fabricate"), Side.RIGHT, b -> mc.setScreen(new FabricateScreen(mc.currentScreen))));

        for (Class<? extends Screen> aClass : Arrays.asList(
                HandledScreen.class,
                LecternScreen.class
        ))
            hookFeature(aClass, exploits);

        hookFeature(LecternScreen.class, new ExploitButtonWidget(Text.of("Lectern Crash"), Side.RIGHT, b -> {
            final ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            final ScreenHandler screenHandler = player.currentScreenHandler;

            DefaultedList<Slot> defaultedList = screenHandler.slots;
            int i = defaultedList.size();
            List<ItemStack> list = Lists.newArrayListWithCapacity(i);

            for (Slot slot : defaultedList) {
                list.add(slot.getStack().copy());
            }

            Int2ObjectMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();

            for(int slot = 0; slot < i; ++slot) {
                ItemStack itemStack = list.get(slot);
                ItemStack itemStack2 = (defaultedList.get(slot)).getStack();
                if (!ItemStack.areEqual(itemStack, itemStack2)) {
                    int2ObjectMap.put(slot, itemStack2.copy());
                }
            }

            final ClientConnection connection = MinecraftClient.getInstance().getNetworkHandler().getConnection();
            if (connection == null) return;

            connection.send(new ClickSlotC2SPacket(player.currentScreenHandler.syncId, player.currentScreenHandler.getRevision(), 0, 0, SlotActionType.QUICK_MOVE, player.currentScreenHandler.getCursorStack().copy(), int2ObjectMap));
        }));

        hookFeature(ShulkerBoxScreen.class, shulkerDupe = new ToggleableExploitButtonWidget(Text.of("Shulker Dupe"), Side.RIGHT, b -> {}, false));
        hookFeature(ShulkerBoxScreen.class, shulkerDupeMulti = new ToggleableExploitButtonWidget(Text.of("Shulker Dupe (Multi)"), Side.RIGHT, b -> {}, false));
    }

    public static void tickShulkerDupe() {
        if (!(MinecraftClient.getInstance().player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) return;

        if (shulkerDupe.isToggled() || shulkerDupeMulti.isToggled()) {
            if (MinecraftClient.getInstance().crosshairTarget instanceof BlockHitResult blockHitResult) {
                if (MinecraftClient.getInstance().world.getBlockState(blockHitResult.getBlockPos()).getBlock() instanceof ShulkerBoxBlock) {
                    MinecraftClient.getInstance().interactionManager.updateBlockBreakingProgress(blockHitResult.getBlockPos(), Direction.DOWN);
                } else {
                    MinecraftClient.getInstance().player.sendMessage(Text.of("You need to have a shulker box screen open and look at a shulker box."));
                    MinecraftClient.getInstance().player.closeHandledScreen();
                    shulkerDupe.disable();
                    shulkerDupeMulti.disable();
                }
            }
        }
    }

    private static void quickMoveAllItems() {
        for (int i = 0; i < 27; i++) quickMoveItem(i);
    }

    private static void quickMoveItem(final int slot) {
        assert MinecraftClient.getInstance().player != null;
        final ScreenHandler screenHandler = MinecraftClient.getInstance().player.currentScreenHandler;
        Int2ObjectMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(slot, screenHandler.getSlot(slot).getStack());
        final ClientConnection connection = Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getConnection();
        if (connection == null) return;

        connection.send(new ClickSlotC2SPacket(screenHandler.syncId, 0, slot, 0, SlotActionType.QUICK_MOVE, screenHandler.getSlot(0).getStack(), stack));
    }

    public static boolean shouldCancel(final Packet<?> packet) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;

        if (player.currentScreenHandler instanceof ShulkerBoxScreenHandler && packet instanceof PlayerActionC2SPacket) {
            if (((PlayerActionC2SPacket) packet).getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                if (shulkerDupe.isToggled()) {
                    quickMoveItem(0);
                    shulkerDupe.disable();
                } else if (shulkerDupeMulti.isToggled()) {
                    quickMoveAllItems();
                    shulkerDupeMulti.disable();
                }
            }
        }

        if (cancelSignPackets && packet instanceof UpdateSignC2SPacket) {
            cancelSignPackets = false;
            return true;
        }

        if (shouldCancelUIPackets.isToggled() && (packet instanceof ClickSlotC2SPacket || packet instanceof ButtonClickC2SPacket))
            return true;

        if (shouldDelayUIPackets.isToggled() && (packet instanceof ClickSlotC2SPacket || packet instanceof ButtonClickC2SPacket)) {
            delayedUIPackets.add(packet);
            return true;
        }
        return false;
    }

    public static List<ExploitButtonWidget> fromScreen(final Screen original) {
        return exploitTracker.stream().filter(pair -> pair.getRight().isAssignableFrom(original.getClass())).map(Pair::getLeft).toList();
    }

    public void hookFeature(final Class<? extends Screen> screenClass, final ExploitButtonWidget exploitButton) {
        hookFeature(screenClass, Collections.singletonList(exploitButton));
    }

    public void hookFeature(final Class<? extends Screen> screenClass, final List<ExploitButtonWidget> buttons) {
        buttons.forEach(buttonWidget -> exploitTracker.add(new Pair<>(buttonWidget, screenClass)));
    }
}
