package de.florianmichael.uiutilsreborn.feature;

import de.florianmichael.uiutilsreborn.ExploitButton;
import de.florianmichael.uiutilsreborn.UIUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

import java.util.ArrayList;
import java.util.List;

public class GeneralFeatures {
    public static void hook(final UIUtils instance) {
        final MinecraftClient mc = MinecraftClient.getInstance();
        final List<ExploitButton> exploits = new ArrayList<>();

        exploits.add(new ExploitButton("Close without packet", parent -> mc.setScreen(null)));
        exploits.add(new ExploitButton("De-sync", parent -> mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId))));

        instance.hookFeature(HandledScreen.class, exploits);
    }
}
