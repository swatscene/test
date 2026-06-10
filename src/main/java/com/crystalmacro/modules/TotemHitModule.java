package com.crystalmacro.modules;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * When you left-click an entity while holding a totem in main hand,
 * swap to your sword, attack, then swap back.
 *
 * Requires: sword somewhere in your hotbar.
 */
public class TotemHitModule extends Module {

    private boolean wasAttacking = false;
    private int restoreSlot      = -1;
    private int restoreTicks     = 0;

    public TotemHitModule() {
        super("TotemHit", "key.crystalmacro.totemhit", GLFW.GLFW_KEY_K);
    }

    @Override
    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        // ── Restore-back logic ──
        if (restoreTicks > 0) {
            restoreTicks--;
            if (restoreTicks == 0 && restoreSlot != -1) {
                client.player.getInventory().setSelectedSlot(restoreSlot);
                restoreSlot = -1;
            }
            return;
        }

        boolean attackDown = client.options.attackKey.isPressed();

        // Rising edge: just started attacking
        if (attackDown && !wasAttacking) {
            HitResult hit = client.crosshairTarget;
            if (hit instanceof EntityHitResult ehr) {
                Entity target = ehr.getEntity();
                if (target instanceof LivingEntity && target != client.player) {
                    int selected = client.player.getInventory().getSelectedSlot();

                    // Only swap if currently holding a totem in mainhand
                    if (client.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        int sword = findSword(client);
                        if (sword != -1) {
                            client.player.getInventory().setSelectedSlot(sword);
                            // Attack the entity directly (the held left-click already triggers,
                            // but we send an extra one to make sure it lands on the new item)
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(client.player.getActiveHand());
                            restoreSlot  = selected;
                            restoreTicks = 2;   // ~100 ms before swapping back
                        }
                    }
                }
            }
        }
        wasAttacking = attackDown;
    }

    private int findSword(MinecraftClient client) {
        var inv = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            var stack = inv.getStack(i);
            String name = stack.getItem().toString().toLowerCase();
            if (name.contains("sword")) return i;
        }
        return -1;
    }
}
