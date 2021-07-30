package com.minenash.oplock.mixin;

import com.minenash.oplock.OpLock;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.dedicated.command.OpCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(OpCommand.class)
public class OpCommandMixin {

	@Redirect(method = "op", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;addToOperators(Lcom/mojang/authlib/GameProfile;)V"))
	private static void addToOpLock(PlayerManager manager, GameProfile profile) {
		OpLock.opStatus.put(profile.getId(), false);
		OpLock.save();

		manager.sendCommandTree(manager.getPlayer(profile.getId()));
	}

}
