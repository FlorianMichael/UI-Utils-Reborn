package de.florianmichael.uiutilsreborn;

import de.florianmichael.uiutilsreborn.feature.GeneralFeatures;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.network.Packet;

import java.util.*;

public class UIUtils implements ClientModInitializer {

    public static final int BUTTON_DIFF = 20 + 4;

    private final static Map<Class<? extends Screen>, List<ExploitButton>> exploitTracker = new HashMap<>();
    private final static List<ExploitButton> EMPTY_LIST = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        final MinecraftClient mc = MinecraftClient.getInstance();

        hookFeature(SleepingChatScreen.class, new ExploitButton("Client wake up", (parent) -> {
            assert mc.player != null;

            mc.player.wakeUp();
            mc.setScreen(null);
        }));

        hookFeature(SignEditScreen.class, new ExploitButton("Close without packet", (parent) -> mc.setScreen(null)));
        GeneralFeatures.hook(this);
    }

    public static boolean shouldCancel(final Packet<?> packet) {

        return false;
    }

    public static List<ExploitButton> fromScreen(final Screen original) {
        for (Map.Entry<Class<? extends Screen>, List<ExploitButton>> entry : exploitTracker.entrySet()) {
            if (entry.getKey().isAssignableFrom(original.getClass()))
                return entry.getValue();
        }

        return EMPTY_LIST;
    }

    public void hookFeature(final Class<? extends Screen> screenClass, final ExploitButton exploitButton) {
        hookFeature(screenClass, Collections.singletonList(exploitButton));
    }

    public void hookFeature(final Class<? extends Screen> screenClass, final List<ExploitButton> buttons) {
        exploitTracker.put(screenClass, buttons);
    }
}
