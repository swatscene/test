package com.crystalmacro.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;

public class TotemHitModule extends Module {

    private boolean wasAttacking = false;

    public TotemHitModule() {
        super("TotemHit", "key.crystalmacro.totemhit", GLFW.GLFW_KEY_K);
    }

    @Override
    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (client.interactionManager == null) return;
        ClientPlayNetworkHandler net = client.getNetworkHandler();
        if (net == null) return;

        boolean attackDown = client.options.attackKey.isPressed();

        if (attackDown && !wasAttacking) {
            if (client.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                HitResult hit = client.crosshairTarget;
                if (hit instanceof EntityHitResult ehr) {
                    Entity target = ehr.getEntity();
                    if (target instanceof LivingEntity && target != client.player) {
                        int swordSlot = findSwordInHotbar(client);
                        if (swordSlot != -1) {
                            int currentSlot = client.player.getInventory().getSelectedSlot();

                            // Use inventory swap packets (server sees inventory edit, not hotbar swap).
                            // Slot indices in PlayerInventory: hotbar 0-8 maps to slot 36-44 in screen handler
                            int totemScreenSlot = 36 + currentSlot;
                            int swordScreenSlot = 36 + swordSlot;
                            int syncId = client.player.playerScreenHandler.syncId;

                            // 1) Swap totem and sword positions via slot-click packets
                            sendSwap(net, syncId, totemScreenSlot, swordScreenSlot, client);

                            // 2) Attack — server now sees sword in our hand slot
                            client.interactionManager.attackEntity(client.player, target);

                            // 3) Swap back
                            sendSwap(net, syncId, totemScreenSlot, swordScreenSlot, client);

                            // 4) Swing
                            client.player.swingHand(client.player.getActiveHand());
                        }
                    }
                }
            }
        }
        wasAttacking = attackDown;
    }

    private void sendSwap(ClientPlayNetworkHandler net, int syncId,
                          int slotA, int slotB, MinecraftClient client) {
        // SWAP action: pick up A, drop on B, pick up B, drop on A
        // Using SlotActionType.SWAP with button as the source-hotbar-slot would be easier
        // but cross-server compat varies. Use PICKUP sequence for max compatibility.
        var inv = client.player.playerScreenHandler.slots;
        var stackA = inv.get(slotA).getStack();
        var stackB = inv.get(slotB).getStack();

        net.sendPacket(new ClickSlotC2SPacket(
                syncId, 0, (short) slotA, (byte) 0,
                SlotActionType.PICKUP, stackA.copy(), new HashMap<>()));
        net.sendPacket(new ClickSlotC2SPacket(
                syncId, 0, (short) slotB, (byte) 0,
                SlotActionType.PICKUP, stackB.copy(), new HashMap<>()));
        net.sendPacket(new ClickSlotC2SPacket(
                syncId, 0, (short) slotA, (byte) 0,
                SlotActionType.PICKUP, stackA.copy(), new HashMap<>()));
    }

    private int findSwordInHotbar(MinecraftClient client) {
        var inv = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            String name = inv.getStack(i).getItem().toString().toLowerCase();
            if (name.contains("sword")) return i;
        }
        return -1;
    }
}
