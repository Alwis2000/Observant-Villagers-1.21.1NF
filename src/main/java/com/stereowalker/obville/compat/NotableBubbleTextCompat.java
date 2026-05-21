package com.stereowalker.obville.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import com.mrbysco.nbt.client.BubbleHandler;
import com.mrbysco.nbt.command.BubbleText;

public class NotableBubbleTextCompat {
    public static void createBubble(Entity entity, Component cleanMessage, long gameTime) {
        String author = entity.getName().getString();
        String message = cleanMessage.getString();
        BubbleHandler.addBubble(author, new BubbleText(author, message, entity.getUUID(), gameTime));
    }
}
