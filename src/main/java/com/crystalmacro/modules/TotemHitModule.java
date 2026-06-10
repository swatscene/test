package com.crystalmacro.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class TotemHitModule extends Module {

    private boolean wasAttacking = false;
    private int restoreSlot  = -1;
    private int restoreTicks = 0;

    public TotemHitModule() {
        super("TotemHit", "key.crystalmacro.totemhit", GLFW.GLFW_KEY_K);
    }

    @Override
    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        // Restore phase: swap back to totem
        if (restoreTicks > 0) {
            restoreTicks--;
            if (restoreTicks == 0 && restoreSlot != -1) {
                client.player.getInventory().setSelectedSlot(restoreSlot);
                restoreSlot = -1;
            }
        }

        boolean attackDown = client.options.attackKey.isPressed();

        // Rising edge of attack key
        if (attackDown && !wasAttacking) {
            if (client.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                HitResult hit = client.crosshairTarget;
                if (hit instanceof EntityHitResult ehr) {
                    Entity target = ehr.getEntity();
                    if (target instanceof LivingEntity && target != client.player) {
                        int sword = findSword(client);
                        if (sword != -1) {
                            int original = client.player.getInventory().getSelectedSlot();

                            // 1) Swap to sword
                            client.player.getInventory().setSelectedSlot(sword);

                            // 2) Send the attack manually now that sword is held
                            //    (knockback comes from server-side based on held item)
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(client.player.getActiveHand());

                            // 3) Queue swap-back
                            restoreSlot  = original;
                            restoreTicks = 1;  // swap back next tick
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
            String name = inv.getStack(i).getItem().toString().toLowerCase();
            if (name.contains("sword")) return i;
        }
        return -1;
    }
}
