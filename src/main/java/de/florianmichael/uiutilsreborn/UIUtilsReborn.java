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

import de.florianmichael.uiutilsreborn.gui.FabricateScreen;
import de.florianmichael.uiutilsreborn.widget.ExploitButtonWidget;
import de.florianmichael.uiutilsreborn.widget.ToggleableExploitButtonWidget;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import java.util.*;

public class UIUtilsReborn implements ClientModInitializer {
    private static boolean enabled = true;

    public final static int BOUND = 5;
    public final static int BUTTON_DIFF = ExploitButtonWidget.DEFAULT_HEIGHT + 3;

    private final static List<Pair<ExploitButtonWidget, Class<? extends  Screen>>> exploitTracker = new ArrayList<>();
    private final static List<Packet<?>> delayedUIPackets = new ArrayList<>();

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
        hookFeature(SleepingChatScreen.class, new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.client-wake-up"), (b) -> {
            assert mc.player != null;

            mc.player.wakeUp();
            mc.setScreen(null);
        }));
        hookFeature(SignEditScreen.class, new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.client-side-close"), (b) -> {
            mc.setScreen(null);

            UIUtilsReborn.cancelSignPackets = true;
        }));
        hookFeature(DeathScreen.class, new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.force-respawn"), b -> {
            assert mc.player != null;

            mc.player.requestRespawn();
            mc.setScreen(null);
        }));
        hookFeature(DeathScreen.class, new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.fake-respawn"), b -> {
            mc.player.init();
            mc.setScreen(null);
        }));

        // Handled Screen Hooks
        final List<ExploitButtonWidget> exploits = new ArrayList<>();

        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.client-side-close"), button -> mc.setScreen(null)));
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.server-side-close"), button -> {
            assert mc.player != null;

            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        }));
        exploits.add(shouldCancelUIPackets = new ToggleableExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.cancel-packets"), button -> {}, false));
        exploits.add(shouldDelayUIPackets = new ToggleableExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.delay-packets"), button -> {
            if (!shouldDelayUIPackets.isToggled() && !delayedUIPackets.isEmpty()) {
                for (Packet<?> packet : delayedUIPackets)
                    Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);

                delayedUIPackets.clear();
            }
        }, false));
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.disconnect"), button -> {
            if (!delayedUIPackets.isEmpty()) {
                if (shouldCancelUIPackets.isToggled()) shouldDelayUIPackets.toggle();

                for (Packet<?> packet : delayedUIPackets)
                    Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);

                delayedUIPackets.clear();
            }

            mc.getNetworkHandler().getConnection().disconnect(Text.literal("Connection closed (UI Utils Reborn)"));
        }));

        // Save and Load GUI
        final ExploitButtonWidget load = new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.load"), button -> {
            if (storedScreen != null && storedScreenHandler != null) {
                mc.setScreen(storedScreen);
                mc.player.currentScreenHandler = storedScreenHandler;
            }
            button.active = false;
        });
        load.active = false;
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.save"), button -> {
            storedScreen = mc.currentScreen;

            assert mc.player != null;
            storedScreenHandler = mc.player.currentScreenHandler;
            load.active = true;
        }));
        exploits.add(load);

        // Information Copy Buttons
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.copy"), b -> {
            assert mc.player != null;

            mc.keyboard.setClipboard("SyncID: " + mc.player.currentScreenHandler.syncId + ", Revision: " + mc.player.currentScreenHandler.getRevision() + ", Title: " + Text.Serialization.toJsonString(mc.currentScreen.getTitle()));
        }));

        // Packet Fabrication
        exploits.add(new ExploitButtonWidget(Text.translatable("gui.ui-utils-reborn.fabricate"), b -> mc.setScreen(new FabricateScreen(mc.currentScreen))));

        for (Class<? extends Screen> aClass : Arrays.asList(
                HandledScreen.class,
                LecternScreen.class
        ))
            hookFeature(aClass, exploits);
    }

    public static boolean shouldCancel(final Packet<?> packet) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;

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
