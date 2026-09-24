package com.zzy89216.ctdi.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels the vanilla attack cooldown on every {@link Player} (Minecraft 26.3).
 *
 * <p>Verified against the 26.3 bytecode:
 * {@code Player#getAttackStrengthScale(float)} still exists and still returns
 * {@code Mth.clamp((attackStrengthTicker + a) / getCurrentItemAttackStrengthDelay(), 0, 1)}.
 *
 * <p>That value is used by the attack code as:
 * <ul>
 *   <li>damage scaling: {@code 0.2F + s*s*0.8F},</li>
 *   <li>magic/enchant boost: {@code ... * s},</li>
 *   <li>full-strength gate: {@code fullStrengthAttack = s > 0.9F} (crit / knockback),</li>
 *   <li>sweep damage scaling.</li>
 * </ul>
 *
 * <p>Returning {@code 1.0F} means every swing lands at full strength no matter
 * how fast the player clicks, while everything else (damage values, enchant
 * math, attributes) stays vanilla.
 */
@Mixin(Player.class)
public abstract class AttackCooldownMixin {

    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void ctdi$cancelAttackCooldown(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(1.0F);
    }
}
