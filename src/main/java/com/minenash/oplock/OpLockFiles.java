package com.minenash.oplock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public interface OpLockFiles {
    Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    Path OP_PATH = FabricLoader.getInstance().getConfigDir().resolve("oplock.json");
    Path ASI_PATH = FabricLoader.getInstance().getConfigDir().resolve("oplock_auto_sign_in.json");

    static void load() {
        loadAutoSignIn();
        if (!Files.exists(OP_PATH)) {
            save();
            return;
        }
        try {
            var element = GSON.fromJson(Files.newBufferedReader(OP_PATH), JsonElement.class);
            if (!element.isJsonArray()) return;
            for (var entry : element.getAsJsonArray()) {
                if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) continue;
                OpLock.opStatus.put(UUID.fromString(entry.getAsString()), false);
            }
        } catch (Exception e) {
            OpLock.LOGGER.error("[OpLock] Failed to load op list");
            OpLock.LOGGER.catching(e);
        }
    }

    static void loadAutoSignIn() {
        if (!Files.exists(ASI_PATH)) {
            try {
                Files.createFile(ASI_PATH);
            } catch (Exception e) {
                OpLock.LOGGER.error("[OpLock] Failed to create oplock_auto_sign_in.json");
                OpLock.LOGGER.catching(e);
            }
            return;
        }
        try {
            var element = GSON.fromJson(Files.newBufferedReader(ASI_PATH), JsonElement.class);
            if (!element.isJsonArray()) return;
            OpLock.autoSignIn.clear();
            for (var entry : element.getAsJsonArray()) {
                if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) continue;
                OpLock.autoSignIn.add(UUID.fromString(entry.getAsString()));
            }
        } catch (Exception e) {
            OpLock.LOGGER.error("[OpLock] Failed to load oplock_auto_sign_in.json");
            OpLock.LOGGER.catching(e);
        }
    }

    static void save() {
        if (!Files.exists(OP_PATH)) {
            try {
                Files.createFile(OP_PATH);
            } catch (IOException e) {
                OpLock.LOGGER.error("[OpLock] Failed to save op list");
                OpLock.LOGGER.catching(e);
                return;
            }
        }
        var ops = new JsonArray();
        for (var entry : OpLock.opStatus.keySet()) ops.add(entry.toString());
        try {
            Files.write(OP_PATH, GSON.toJson(ops).getBytes());
        } catch (IOException e) {
            OpLock.LOGGER.error("[OpLock] Failed to save op list");
            OpLock.LOGGER.catching(e);
        }
    }
}