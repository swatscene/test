package com.crystalmacro.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * Triggerbot — attacks the crystal your crosshair is on.
 * Only fires while you're holding right-click (so it only breaks crystals
 * you're actively placing, not random ones you happen to look at).
 */
public class CrystalModule extends Module {

    // How many ticks to wait after a hit before hitting again on the same target.
    // 0 = every tick (fastest), 1-2 = slightly more human-like
    public static int cooldownTicks = 0;

    private int cooldown = 0;

    public CrystalModule() {
        super("CrystalMacro", "key.crystalmacro.toggle", GLFW.GLFW_KEY_R);
    }

    @Override
    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (client.interactionManager == null) return;

        if (cooldown > 0) { cooldown--; return; }

        // Only fire while user is holding right-click (placing crystals)
        if (!client.options.useKey.isPressed()) return;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof EndCrystalEntity crystal)) return;

        // Attack it
        client.interactionManager.attackEntity(client.player, crystal);
        client.player.swingHand(client.player.getActiveHand());
        cooldown = cooldownTicks;
    }
}
