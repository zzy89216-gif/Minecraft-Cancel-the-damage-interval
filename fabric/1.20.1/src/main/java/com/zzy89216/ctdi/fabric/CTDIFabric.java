package com.zzy89216.ctdi.fabric;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric entrypoint of Cancel The Damage Interval.
 *
 * <p>The actual behavior lives in the two mixins
 * ({@code InvulnerabilityMixin}, {@code AttackCooldownMixin}), which are
 * shared verbatim with the Forge build for the same Minecraft version because
 * both use Mojang official mappings.
 */
public class CTDIFabric implements ModInitializer {
    public static final String MOD_ID = "ctdi";
    public static final Logger LOGGER = LoggerFactory.getLogger("CTDI");

    @Override
    public void onInitialize() {
        LOGGER.info("[CTDI] loaded: damage i-frames and attack cooldown are disabled. Hand speed is the DPS.");
    }
}
