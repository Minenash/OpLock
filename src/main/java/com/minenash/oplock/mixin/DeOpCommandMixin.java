package com.minenash.oplock.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minenash.oplock.OpLock;
import com.minenash.oplock.OpLockFiles;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.dedicated.command.DeOpCommand;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DeOpCommand.class)
public class DeOpCommandMixin {
	@WrapOperation(method = "deop", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;removeFromOperators(Lcom/mojang/authlib/GameProfile;)V"))
	private static void addToOpLock(PlayerManager instance, @NotNull GameProfile profile, @NotNull Operation<Void> original) {
		OpLock.opStatus.remove(profile.getId());
		OpLockFiles.save();
		original.call(instance, profile);
	}

	@WrapOperation(method = "deop", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;isOperator(Lcom/mojang/authlib/GameProfile;)Z"))
	private static boolean checkOpStatus(PlayerManager instance, GameProfile profile, @NotNull Operation<Boolean> original) {
		return original.call(instance, profile) || OpLock.opStatus.containsKey(profile.getId());
	}
}