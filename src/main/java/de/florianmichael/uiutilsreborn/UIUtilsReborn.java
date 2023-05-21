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
import de.florianmichael.uiutilsreborn.util.Side;
import de.florianmichael.uiutilsreborn.widget.ExploitButtonWidget;
import de.florianmichael.uiutilsreborn.widget.ToggleableExploitButtonWidget;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.LecternScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

import java.util.*;

public class UIUtilsReborn implements ClientModInitializer {
    public static boolean enabled = true;

    public final static int BOUND = 5;
    public final static int BUTTON_DIFF = ExploitButtonWidget.DEFAULT_HEIGHT + 3;

    private final static Map<Class<? extends Screen>, List<ExploitButtonWidget>> exploitTracker = new HashMap<>();
    private final static List<Packet<?>> delayedUIPackets = new ArrayList<>();

    private static boolean cancelSignPackets;

    private static boolean shouldSendUIPackets = true;
    private static boolean shouldDelayUIPackets = true;

    private static Screen storedScreen = null;
    private static ScreenHandler storedScreenHandler = null;

    @Override
    public void onInitializeClient() {
        final MinecraftClient mc = MinecraftClient.getInstance();

        // Some Random GUIs
        hookFeature(SleepingChatScreen.class, new ExploitButtonWidget("client-wake-up", Side.LEFT, (b) -> {
            assert mc.player != null;

            mc.player.wakeUp();
            mc.setScreen(null);
        }));
        hookFeature(SignEditScreen.class, new ExploitButtonWidget("client-side-close", Side.LEFT, (b) -> {
            mc.setScreen(null);

            UIUtilsReborn.cancelSignPackets = true;
        }));
        hookFeature(DeathScreen.class, new ExploitButtonWidget("force-respawn", Side.LEFT, b -> {
            assert mc.player != null;

            mc.player.requestRespawn();
            mc.setScreen(null);
        }));

        // Handled Screen Hooks
        final List<ExploitButtonWidget> exploits = new ArrayList<>();

        exploits.add(new ExploitButtonWidget("client-side-close", Side.LEFT, button -> mc.setScreen(null)));
        exploits.add(new ExploitButtonWidget("server-side-close", Side.LEFT, button -> {
            assert mc.player != null;

            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        }));
        exploits.add(new ToggleableExploitButtonWidget("send-packets", Side.LEFT, button -> shouldSendUIPackets = !shouldSendUIPackets, shouldSendUIPackets));
        exploits.add(new ToggleableExploitButtonWidget("delay-packets", Side.LEFT, button -> {
            if (!shouldDelayUIPackets && !delayedUIPackets.isEmpty()) {
                for (Packet<?> packet : delayedUIPackets)
                    Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);

                delayedUIPackets.clear();
            }
        }, shouldDelayUIPackets));
        exploits.add(new ExploitButtonWidget("disconnect", Side.LEFT, button -> {
            if (!delayedUIPackets.isEmpty()) {
                shouldDelayUIPackets = false;

                for (Packet<?> packet : delayedUIPackets)
                    Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);

                mc.getNetworkHandler().getConnection().disconnect(Text.literal("Connection closed (UI Utils Reborn)"));
                delayedUIPackets.clear();
            }
        }));

        // Save and Load GUI
        final ExploitButtonWidget load = new ExploitButtonWidget("load", Side.LEFT, button -> {
            if (storedScreen != null && storedScreenHandler != null) {
                mc.setScreen(storedScreen);
                mc.player.currentScreenHandler = storedScreenHandler;
            }
            button.active = false;
        });
        load.active = false;
        exploits.add(new ExploitButtonWidget("save", Side.LEFT, button -> {
            storedScreen = mc.currentScreen;

            assert mc.player != null;
            storedScreenHandler = mc.player.currentScreenHandler;
            load.active = true;
        }));
        exploits.add(load);

        // Information Copy Buttons
        exploits.add(new ExploitButtonWidget("sid", Side.RIGHT, b -> {
            assert mc.player != null;

            mc.keyboard.setClipboard(String.valueOf(mc.player.currentScreenHandler.syncId));
        }));
        exploits.add(new ExploitButtonWidget("rev", Side.RIGHT, b -> {
            assert mc.player != null;

            mc.keyboard.setClipboard(String.valueOf(mc.player.currentScreenHandler.getRevision()));
        }));
        exploits.add(new ExploitButtonWidget("title", Side.RIGHT, b -> MinecraftClient.getInstance().keyboard.setClipboard(Text.Serializer.toJson(mc.currentScreen.getTitle()))));

        // Packet Fabrication
        exploits.add(new ExploitButtonWidget("fabricate", Side.RIGHT, b -> mc.setScreen(new FabricateScreen(mc.currentScreen))));

        for (Class<? extends Screen> aClass : Arrays.asList(
                HandledScreen.class,
                LecternScreen.class
        ))
            hookFeature(aClass, exploits);
    }

    public static boolean shouldCancel(final Packet<?> packet) {
        if (cancelSignPackets && packet instanceof UpdateSignC2SPacket) {
            cancelSignPackets = false;
            return true;
        }

        if (!shouldSendUIPackets && (packet instanceof ClickSlotC2SPacket || packet instanceof ButtonClickC2SPacket))
            return true;

        if (shouldDelayUIPackets && (packet instanceof ClickSlotC2SPacket || packet instanceof ButtonClickC2SPacket)) {
            delayedUIPackets.add(packet);
            return true;
        }

        return false;
    }

    public static List<ExploitButtonWidget> fromScreen(final Screen original) {
        final List<ExploitButtonWidget> buttons = new ArrayList<>();

        for (Map.Entry<Class<? extends Screen>, List<ExploitButtonWidget>> entry : exploitTracker.entrySet())
            if (entry.getKey().isAssignableFrom(original.getClass()))
                buttons.addAll(entry.getValue());

        return buttons;
    }

    public void hookFeature(final Class<? extends Screen> screenClass, final ExploitButtonWidget exploitButton) {
        hookFeature(screenClass, Collections.singletonList(exploitButton));
    }

    public void hookFeature(final Class<? extends Screen> screenClass, final List<ExploitButtonWidget> buttons) {
        if (exploitTracker.containsKey(screenClass)) {
            exploitTracker.get(screenClass).addAll(buttons);
            return;
        }
        exploitTracker.put(screenClass, buttons);
    }
}
