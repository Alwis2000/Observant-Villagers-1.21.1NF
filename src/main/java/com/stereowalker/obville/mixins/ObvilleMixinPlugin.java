package com.stereowalker.obville.mixins;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class ObvilleMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean hasGuard = net.neoforged.fml.loading.LoadingModList.get().getModFileById("guardvillagers") != null;
        boolean hasTalk = net.neoforged.fml.loading.LoadingModList.get().getModFileById("talk_balloons") != null;

        if (mixinClassName.contains("GuardTalkBalloonsMixin")) {
            return hasGuard && hasTalk;
        }

        if (mixinClassName.contains("mixins.guard")) {
            return hasGuard;
        }

        if (mixinClassName.contains("VillagerTalkBalloonsMixin")) {
            return hasTalk;
        }

        if (mixinClassName.contains("client.LivingEntityRendererMixin")) {
            return hasTalk;
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}