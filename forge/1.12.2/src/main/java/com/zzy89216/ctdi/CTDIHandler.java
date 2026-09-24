package com.zzy89216.ctdi;

import java.lang.reflect.Field;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Removes vanilla damage i-frames and the attack cooldown on Minecraft 1.12.2.
 *
 * <h2>1. i-frames (无敌帧) — {@link LivingAttackEvent}</h2>
 *
 * <p>Verified against the Forge 1.12.2 patch for
 * {@code EntityLivingBase#attackEntityFrom(DamageSource, float)}: Forge inserts
 * {@code ForgeHooks.onLivingAttack(this, source, amount)} as the <b>first line</b>
 * of the method, i.e. before vanilla's own checks — including the i-frame check:
 *
 * <pre>
 * if ((float)this.hurtResistantTime &gt; (float)this.maxHurtResistantTime / 2.0F) {
 *     if (damage &lt;= this.lastDamage) return false;             // hit dropped
 *     this.damageEntity(source, damage - this.lastDamage);     // only the difference
 *     ...
 * } else {
 *     this.lastDamage = damage;
 *     this.hurtResistantTime = this.maxHurtResistantTime;
 *     this.damageEntity(source, damage);                       // full damage
 *     ...
 * }
 * </pre>
 *
 * <p>{@code Entity.hurtResistantTime} is <b>public</b> in 1.12.2, so zeroing it
 * inside the event handler makes vanilla take the full-damage branch for every
 * call. This also covers several hits processed in the same server tick, because
 * the event runs before the field is read on each call.
 *
 * <h2>2. Attack cooldown (攻击冷却) — {@link AttackEntityEvent} + {@link TickEvent.PlayerTickEvent}</h2>
 *
 * <p>1.12.2 {@code EntityPlayer}:
 *
 * <pre>
 * public float getCooledAttackStrength(float adjust) {
 *     return MathHelper.clamp((ticksSinceLastSwing + adjust) / getCooldownPeriod(), 0.0F, 1.0F);
 * }
 * </pre>
 *
 * <p>and inside {@code attackTargetEntityWithCurrentItem} the value {@code s} is read as
 * {@code 0.2F + s*s*0.8F} (weapon damage), {@code enchant * s}, and as the
 * {@code s > 0.9F} gate for crits/knockback. {@code AttackEntityEvent} is fired by
 * Forge as the first line of that method — <b>before</b> {@code s} is read — so
 * pushing {@code ticksSinceLastSwing} up there guarantees full strength for every
 * single attack, including multiple attacks within one tick. The player-tick
 * handler additionally keeps the client's attack indicator bar consistent.
 *
 * <p>{@code ticksSinceLastSwing} is {@code protected} (declared on
 * {@code EntityLivingBase}), so it is written through Forge's own
 * {@link ObfuscationReflectionHelper} with its SRG name — the officially
 * supported way to touch a vanilla member without an access transformer.
 */
public class CTDIHandler {
    /** {@code EntityLivingBase.ticksSinceLastSwing} — SRG name as used at runtime in 1.12.2. */
    private static final String SRG_TICKS_SINCE_LAST_SWING = "field_184617_aD";

    /**
     * Large enough that {@code getCooledAttackStrength()} clamps to 1.0 for any
     * ATTACK_SPEED value (the cooldown period is 20/attackSpeed ticks).
     */
    private static final int FULL_ATTACK_STRENGTH = 1000;

    private static final Field TICKS_SINCE_LAST_SWING = findTicksField();

    private static Field findTicksField() {
        try {
            return ObfuscationReflectionHelper.findField(EntityLivingBase.class, SRG_TICKS_SINCE_LAST_SWING);
        } catch (Throwable t) {
            CTDI.LOGGER.error("[CTDI] could not resolve EntityLivingBase#ticksSinceLastSwing ({}); "
                    + "the attack cooldown will stay vanilla", SRG_TICKS_SINCE_LAST_SWING, t);
            return null;
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity == null) {
            return;
        }
        // No i-frames: every incoming hit is resolved with its full damage.
        entity.hurtResistantTime = 0;
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        forceFullAttackStrength(event.getEntityPlayer());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            forceFullAttackStrength(event.player);
        }
    }

    private static void forceFullAttackStrength(EntityPlayer player) {
        if (player == null || TICKS_SINCE_LAST_SWING == null) {
            return;
        }
        try {
            TICKS_SINCE_LAST_SWING.setInt(player, FULL_ATTACK_STRENGTH);
        } catch (IllegalAccessException e) {
            // findField() already made the field accessible; nothing sensible left to do.
            CTDI.LOGGER.error("[CTDI] could not write ticksSinceLastSwing", e);
        }
    }
}
