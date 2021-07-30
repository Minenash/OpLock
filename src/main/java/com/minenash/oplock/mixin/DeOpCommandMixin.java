package com.minenash.oplock.mixin;

import com.minenash.oplock.OpLock;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.dedicated.command.DeOpCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DeOpCommand.class)
public class DeOpCommandMixin {

	@Redirect(method = "deop", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;removeFromOperators(Lcom/mojang/authlib/GameProfile;)V"))
	private static void addToOpLock(PlayerManager manager, GameProfile profile) {
		OpLock.opStatus.remove(profile.getId());
		OpLock.save();

		manager.removeFromOperators(profile);
	}

	@Redirect(method = "deop", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;isOperator(Lcom/mojang/authlib/GameProfile;)Z"))
	private static boolean checkOpStatus(PlayerManager manager, GameProfile profile) {
		return OpLock.opStatus.containsKey(profile.getId()) || manager.isOperator(profile);
	}

}
