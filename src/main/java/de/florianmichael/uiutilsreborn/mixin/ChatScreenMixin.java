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

package de.florianmichael.uiutilsreborn.mixin;

import de.florianmichael.uiutilsreborn.UIUtilsReborn;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(at = @At("HEAD"), method = "sendMessage", cancellable = true)
    public void hookToggleCommand(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (chatText.equals("$ui-utils-reborn")) {
            UIUtilsReborn.enabled = !UIUtilsReborn.enabled;

            assert MinecraftClient.getInstance().player != null;
            MinecraftClient.getInstance().player.sendMessage(Text.of((UIUtilsReborn.enabled ? Formatting.GREEN : Formatting.RED) + "UI-Utils-Reborn is now " + (UIUtilsReborn.enabled ? "enabled" : "disabled")));
            MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(chatText);

            cir.setReturnValue(true);
        }
    }
}
