package com.zzy89216.ctdi.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels the vanilla attack cooldown on every {@link Player}.
 *
 * <p>Verified against the Minecraft 1.20.1 bytecode of
 * {@code Player#getAttackStrengthScale(float)}:
 *
 * <pre>
 * public float getAttackStrengthScale(float partialTicks) {
 *     return Mth.clamp((this.attackStrengthTicker + partialTicks) / this.getAttackSpeedModifier(), 0.0F, 1.0F);
 * }
 * </pre>
 *
 * <p>{@code Player#attack} uses that strength value {@code s} as:
 * <ul>
 *   <li>base weapon damage multiplier: {@code sBase * (0.2F + s*s*0.8F)},</li>
 *   <li>enchantment damage multiplier (e.g. sharpness): {@code ench * s},</li>
 *   <li>gate for crit, knockback and sprint bonus: {@code s > 0.9F}.</li>
 * </ul>
 *
 * <p>Always reporting {@code 1.0F} means every hit lands with full strength and
 * full crit/knockback potential no matter how fast the player swings, while the
 * rest of vanilla combat (damage values, enchant math, attribute modifiers)
 * stays exactly as-is. The client-side crosshair cooldown indicator also reads
 * this method, so it stays consistent with the actual behavior.
 */
@Mixin(Player.class)
public abstract class AttackCooldownMixin {

    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void ctdi$cancelAttackCooldown(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(1.0F);
    }
}
