package com.minenash.oplock;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class OpLock implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("OpLock");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path OP_PATH = FabricLoader.getInstance().getConfigDir().resolve("oplock.json");

	public static final Map<UUID,Boolean> opStatus = new HashMap<>();
	public static MinecraftServer server;

	@Override
	public void onInitialize() {

		ServerLifecycleEvents.SERVER_STARTING.register(OpLock::load);

		ServerPlayConnectionEvents.JOIN.register(((handler, _sender, _server) -> autoLogout(handler.player.getGameProfile())));
		ServerPlayConnectionEvents.DISCONNECT.register(((handler, _server) -> autoLogout(handler.player.getGameProfile())));

		CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated) -> dispatcher.register(
				literal("oplock").requires(OpLock::canRunCommand)
						.then( literal("login").executes(OpLock::login))
						.then( literal("logout").executes(OpLock::logout))
		)));
	}

	private static boolean canRunCommand(ServerCommandSource source) {
		try {
			return opStatus.containsKey(source.getPlayer().getUuid());
		}
		catch (Exception e) {
			return false;
		}
	}

	private static int login(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		opStatus.put(player.getUuid(), true);
		server.getPlayerManager().addToOperators(player.getGameProfile());
		player.sendMessage(new LiteralText("§8[§2OpLock§8]§a Logged In, you know have op powers"), false);
		LOGGER.info("[OpLock] " + player.getGameProfile().getName() + " logged in");
		return 1;
	}

	private static int logout(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		opStatus.put(context.getSource().getPlayer().getUuid(), false);
		server.getPlayerManager().removeFromOperators(player.getGameProfile());
		player.sendMessage(new LiteralText("§8[§2OpLock§8]§a Logged Out, you know no longer have op powers"), false);
		LOGGER.info("[OpLock] " + player.getGameProfile().getName() + " logged out");
		return 1;
	}

	private static void autoLogout(GameProfile profile) {
		if (opStatus.containsKey(profile.getId()) || server.getPlayerManager().isOperator(profile)) {
			opStatus.put(profile.getId(), false);
			server.getPlayerManager().removeFromOperators(profile);
			save();
		}
	}

	private static void load(MinecraftServer server) {
		OpLock.server = server;
		if (!Files.exists(OP_PATH))
			return;

		try {
			for (JsonElement entry : GSON.fromJson(Files.newBufferedReader(OP_PATH), JsonArray.class))
				if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString())
					opStatus.put(UUID.fromString(entry.getAsString()), false);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void save() {
		if (!Files.exists(OP_PATH)) {
			try {
				Files.createFile(OP_PATH);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}

		JsonArray json = new JsonArray();
		for (UUID entry : opStatus.keySet())
			json.add(entry.toString());

		try {
			Files.write(OP_PATH, GSON.toJson(json).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
