package com.zzy89216.ctdi;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cancel The Damage Interval (CTDI).
 *
 * <p>Remove the limits. Keep the combat.
 *
 * <p>This mod removes the two vanilla mechanics that throttle attack rate:
 * <ul>
 *   <li>Damage i-frames: {@code LivingEntity#hurt} refuses damage while
 *       {@code invulnerableTime > 10} (and applies a damage-diff rule while
 *       {@code invulnerableTime > 0}). CTDI zeroes the timer before the vanilla
 *       check runs, so every hit enters full damage resolution.</li>
 *   <li>Attack cooldown: {@code Player#getAttackStrengthScale} returns the
 *       cooldown progress in {@code [0,1]}, which vanilla uses to scale base
 *       weapon damage ({@code 0.2 + s*s*0.8}), scale sharpness/sharpness-like
 *       enchantment damage, and gate crits, knockback and the sprint bonus
 *       ({@code s > 0.9}). CTDI always reports full strength.</li>
 * </ul>
 *
 * <p>Everything else (enchantments, potions, crits, attributes, knockback,
 * shields, armor, vanilla damage calculation) is left untouched.
 *
 * <p>Final damage resolution is server-authoritative: the mixins run on both
 * client and server, so a CTDI server accepts rapid full-strength hits.
 */
@Mod(CTDIMod.MOD_ID)
public class CTDIMod {
    public static final String MOD_ID = "ctdi";
    public static final Logger LOGGER = LoggerFactory.getLogger("CTDI");

    public CTDIMod() {
        LOGGER.info("[CTDI] loaded: damage i-frames and attack cooldown are disabled. Hand speed is the DPS.");
    }
}
