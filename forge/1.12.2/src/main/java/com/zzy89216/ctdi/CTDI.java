package com.zzy89216.ctdi;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Cancel The Damage Interval (CTDI) — Minecraft 1.12.2 / Forge.
 *
 * <p>Remove the limits. Keep the combat.
 *
 * <p>Unlike the newer targets this build needs no Mixin: Forge 1.12.2 fires
 * {@code LivingAttackEvent} as the very first line of
 * {@code EntityLivingBase#attackEntityFrom} and {@code AttackEntityEvent} before
 * the attack-cooldown value is read, which is enough to remove both mechanics.
 * See {@link CTDIHandler}.
 */
@Mod(modid = CTDI.MODID, name = CTDI.NAME, version = CTDI.VERSION,
        acceptedMinecraftVersions = "[1.12.2]", acceptableRemoteVersions = "*")
public class CTDI {
    public static final String MODID = "ctdi";
    public static final String NAME = "Cancel The Damage Interval";
    public static final String VERSION = "0.2.0";
    public static final Logger LOGGER = LogManager.getLogger("CTDI");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new CTDIHandler());
        LOGGER.info("[CTDI] loaded: damage i-frames and attack cooldown are disabled. Hand speed is the DPS.");
    }
}
