package com.gamma.spool.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;

import com.gamma.spool.core.Spool;
import com.gamma.spool.gui.GuiHandler;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

@EventBusSubscriber
public class KeyHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final KeyBinding statsKey = new KeyBinding("Spool Stats GUI", Keyboard.KEY_HOME, "Spool");

    static {
        ClientRegistry.registerKeyBinding(statsKey);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.KeyInputEvent event) {
        if (mc.thePlayer != null && mc.theWorld != null && statsKey.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer player = mc.thePlayer;
            if (mc.isIntegratedServerRunning()) // If the client is the host.
                player.openGui(
                    Spool.instance,
                    GuiHandler.STATS_ID,
                    player.worldObj,
                    (int) (player.posX + 0.5d),
                    (int) (player.posY + 0.5d),
                    (int) (player.posZ + 0.5d));
            else player.addChatComponentMessage(
                new ChatComponentText(
                    "" + EnumChatFormatting.ITALIC
                        + EnumChatFormatting.RED
                        + "Unable to view Spool stats as a client."
                        + EnumChatFormatting.RESET));
        }
    }
}
