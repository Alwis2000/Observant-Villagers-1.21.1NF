package com.stereowalker.obville.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

public class TalkBalloonsCompat {
    public static void createBalloon(Entity entity, Component cleanMessage) {
        if (entity instanceof com.cerbon.talk_balloons.util.mixin.ITalkBalloonsPlayer talkEntity) {
            int age = com.cerbon.talk_balloons.TalkBalloons.config.getBalloonAge() * 20;
            talkEntity.talk_balloons$createBalloonMessage(cleanMessage, age);
        }
    }
}
