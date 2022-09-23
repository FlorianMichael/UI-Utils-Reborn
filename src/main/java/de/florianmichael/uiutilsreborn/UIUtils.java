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
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

import java.util.*;

public class UIUtils implements ClientModInitializer {

    public static final int BOUND = 5;
    public static final int BUTTON_DIFF = 20 + 4;

    private final static Map<Class<? extends Screen>, List<ExploitButtonWidget>> exploitTracker = new HashMap<>();
    private static final List<Packet<?>> delayedUIPackets = new ArrayList<>();

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

            UIUtils.cancelSignPackets = true;
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
        exploits.add(new ToggleableExploitButtonWidget("send-packets", Side.LEFT, button -> shouldSendUIPackets = !shouldSendUIPackets));
        exploits.add(new ToggleableExploitButtonWidget("delay-packets", Side.LEFT, button -> {
            if (!shouldDelayUIPackets && !delayedUIPackets.isEmpty()) {
                for (Packet<?> packet : delayedUIPackets)
                    Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(packet);

                delayedUIPackets.clear();
            }
        }));
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

            mc.keyboard.setClipboard(mc.player.currentScreenHandler.syncId + "");
        }));
        exploits.add(new ExploitButtonWidget("rev", Side.RIGHT, b -> {
            assert mc.player != null;

            mc.keyboard.setClipboard(mc.player.currentScreenHandler.getRevision() + "");
        }));

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

        if (shouldSendUIPackets && (packet instanceof ClickSlotC2SPacket || packet instanceof ButtonClickC2SPacket))
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
