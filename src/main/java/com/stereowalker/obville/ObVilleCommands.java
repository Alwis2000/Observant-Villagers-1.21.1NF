package com.stereowalker.obville;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.stereowalker.obville.dat.OVModData;
import com.stereowalker.obville.interfaces.IModdedEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.Collections;

public class ObVilleCommands {
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		register(event.getDispatcher());
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("obville")
				.then(Commands.literal("reputation")
					.then(Commands.literal("clear")
						.executes(context -> clearReputation(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException())))
						.then(Commands.argument("players", EntityArgument.players())
							.requires(source -> source.hasPermission(2))
							.executes(context -> clearReputation(context.getSource(), EntityArgument.getPlayers(context, "players")))
						)
					)
					.then(Commands.literal("set")
						.then(Commands.argument("amount", IntegerArgumentType.integer())
							.executes(context -> setReputation(context.getSource(), IntegerArgumentType.getInteger(context, "amount"), Collections.singleton(context.getSource().getPlayerOrException())))
							.then(Commands.argument("players", EntityArgument.players())
								.requires(source -> source.hasPermission(2))
								.executes(context -> setReputation(context.getSource(), IntegerArgumentType.getInteger(context, "amount"), EntityArgument.getPlayers(context, "players")))
							)
						)
					)
					.then(Commands.literal("get")
						.executes(context -> getReputation(context.getSource(), context.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
							.requires(source -> source.hasPermission(2))
							.executes(context -> getReputation(context.getSource(), EntityArgument.getPlayer(context, "player")))
						)
					)
				)
		);
	}

	private static int clearReputation(CommandSourceStack source, Collection<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			OVModData data = ((IModdedEntity) player).getData();
			data.clearReputation();
			player.sendSystemMessage(Component.literal("Your reputation has been cleared.").withStyle(net.minecraft.ChatFormatting.GREEN));
		}
		if (players.size() > 1) {
			source.sendSuccess(() -> Component.literal("Cleared reputation for " + players.size() + " players.").withStyle(net.minecraft.ChatFormatting.GREEN), true);
		} else if (players.size() == 1 && players.iterator().next() != source.getEntity()) {
			source.sendSuccess(() -> Component.literal("Cleared reputation for " + players.iterator().next().getScoreboardName()).withStyle(net.minecraft.ChatFormatting.GREEN), true);
		}
		return players.size();
	}

	private static int setReputation(CommandSourceStack source, int amount, Collection<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			OVModData data = ((IModdedEntity) player).getData();
			int village = data.currentVillage();
			if (village >= 0) {
				data.setReputation(village, amount);
				player.sendSystemMessage(Component.literal("Your reputation in the current village (ID: " + village + ") has been set to " + amount + ".").withStyle(net.minecraft.ChatFormatting.GREEN));
			} else {
				if (ObVille.MOD_CONFIG.global_rep) {
					data.setReputation(0, amount);
					player.sendSystemMessage(Component.literal("Your reputation in the global village has been set to " + amount + ".").withStyle(net.minecraft.ChatFormatting.GREEN));
				} else {
					source.sendFailure(Component.literal("Player " + player.getScoreboardName() + " is not currently in any village. Reputation was set for the default village (ID: 0) instead.").withStyle(net.minecraft.ChatFormatting.YELLOW));
					data.setReputation(0, amount);
				}
			}
		}
		if (players.size() > 1) {
			source.sendSuccess(() -> Component.literal("Set reputation to " + amount + " for " + players.size() + " players.").withStyle(net.minecraft.ChatFormatting.GREEN), true);
		} else if (players.size() == 1 && players.iterator().next() != source.getEntity()) {
			source.sendSuccess(() -> Component.literal("Set reputation to " + amount + " for " + players.iterator().next().getScoreboardName()).withStyle(net.minecraft.ChatFormatting.GREEN), true);
		}
		return players.size();
	}

	private static int getReputation(CommandSourceStack source, ServerPlayer player) {
		OVModData data = ((IModdedEntity) player).getData();
		int village = data.currentVillage();
		int rep = data.getReputation();
		source.sendSuccess(() -> Component.literal(player.getScoreboardName() + "'s current reputation is: " + rep + " (Village ID: " + (village >= 0 ? village : "None") + ")").withStyle(net.minecraft.ChatFormatting.AQUA), false);
		return rep;
	}
}
