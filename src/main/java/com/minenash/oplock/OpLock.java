package com.minenash.oplock;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class OpLock implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("OpLock");
	public static final Set<UUID> autoSignIn = new HashSet<>();
	public static final Map<UUID, Boolean> opStatus = new HashMap<>();

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> OpLockFiles.load());
		CommandRegistrationCallback.EVENT.register(((dispatcher, commandRegistryAccess, environment) -> dispatcher.register(CommandManager.literal("oplock").requires(OpLock::canRunCommand)
				.then(CommandManager.literal("login").executes(OpLock::login))
				.then(CommandManager.literal("logout").executes(OpLock::logout))
				.then(CommandManager.literal("refresh").executes(OpLock::refresh))
		)));
		ServerPlayConnectionEvents.JOIN.register(((handler, sender, server) -> autoLogout(server, handler.player.getGameProfile())));
		ServerPlayConnectionEvents.DISCONNECT.register(((handler, server) -> autoLogout(server, handler.player.getGameProfile())));
	}

	private static boolean canRunCommand(@NotNull ServerCommandSource source) {
		if (source.isExecutedByPlayer()) return opStatus.containsKey(Objects.requireNonNull(source.getPlayer()).getUuid());
		return false;
	}

	private static int login(@NotNull CommandContext<ServerCommandSource> context) {
		var player = context.getSource().getPlayer();
		if (player == null) return 0;
		opStatus.put(player.getUuid(), true);
		context.getSource().getServer().getPlayerManager().addToOperators(player.getGameProfile());
		player.sendMessage(Text.translatable("oplock.login"), false);
		LOGGER.info("[OpLock] {} logged in", player.getGameProfile().getName());
		return 1;
	}

	private static int logout(@NotNull CommandContext<ServerCommandSource> context) {
		var player = context.getSource().getPlayer();
		if (player == null) return 0;
		opStatus.put(player.getUuid(), false);
		context.getSource().getServer().getPlayerManager().removeFromOperators(player.getGameProfile());
		player.sendMessage(Text.translatable("oplock.logout"), false);
		LOGGER.info("[OpLock] {} logged out", player.getGameProfile().getName());
		return 1;
	}

	private static int refresh(@NotNull CommandContext<ServerCommandSource> context) {
		var player = context.getSource().getPlayer();
		if (player == null) return 0;
		OpLockFiles.loadAutoSignIn();
		player.sendMessage(Text.translatable("oplock.refresh"), false);
		LOGGER.info("[OpLock] Config was refreshed by {}", player.getGameProfile().getName());
		return 1;
	}

	private static void autoLogout(MinecraftServer server, @NotNull GameProfile profile) {
		if (opStatus.containsKey(profile.getId()) || server.getPlayerManager().isOperator(profile)) {
			if (autoSignIn.contains(profile.getId())) {
				opStatus.put(profile.getId(), true);
				server.getPlayerManager().addToOperators(profile);
			} else {
				opStatus.put(profile.getId(), false);
				server.getPlayerManager().removeFromOperators(profile);
			}
			OpLockFiles.save();
		}
	}
}