package com.minenash.oplock.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minenash.oplock.OpLock;
import com.minenash.oplock.OpLockFiles;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.dedicated.command.OpCommand;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(OpCommand.class)
public class OpCommandMixin {
	@WrapOperation(method = "op", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;addToOperators(Lcom/mojang/authlib/GameProfile;)V"))
	private static void addToOpLock(@NotNull PlayerManager instance, @NotNull GameProfile profile, Operation<Void> original) {
		OpLock.opStatus.put(profile.getId(), false);
		OpLockFiles.save();
		instance.sendCommandTree(instance.getPlayer(profile.getId()));
	}
}