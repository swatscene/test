package com.crystalmacro.modules;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * TotemRestockModule
 *  - Watches a hotbar slot (configurable via /totemslot 1-9, default = 9)
 *  - Also watches the offhand
 *  - When either is empty, pulls a totem from inventory into it
 *  - InvOpen mode (default ON): only restocks while inventory screen is open
 *  - CheckHotbar (default ON): also pulls totems from other hotbar slots
 *  - Offhand restocking: default ON
 */
public class TotemRestockModule extends Module {

    // Settings
    public static int  hotbarSlot   = 9;    // 1-9 (NOT 0-8 from user's view); converted internally
    public static boolean invOpen     = true;
    public static boolean checkHotbar = true;
    public static boolean restockOffhand = true;

    // Slot indices in the player screen handler:
    //   9..35 = main inventory
    //   36..44 = hotbar (slot 1..9)
    //   45 = offhand
    private static final int OFFHAND_SLOT = 45;

    private int actionCooldown = 0;   // ticks between restock actions
    private boolean commandRegistered = false;

    public TotemRestockModule() {
        super("TotemRestock", "key.crystalmacro.totemrestock", GLFW.GLFW_KEY_T);
        registerCommand();
    }

    private void registerCommand() {
        if (commandRegistered) return;
        commandRegistered = true;

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("totemslot")
                    .then(ClientCommandManager.argument("slot", IntegerArgumentType.integer(1, 9))
                        .executes(ctx -> {
                            int v = IntegerArgumentType.getInteger(ctx, "slot");
                            hotbarSlot = v;
                            ctx.getSource().sendFeedback(Text.literal(
                                "§7[TotemRestock] §fHotbar slot set to §a" + v));
                            return 1;
                        })
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(Text.literal(
                            "§7[TotemRestock] §fCurrent slot: §a" + hotbarSlot
                            + " §7| §f/totemslot <1-9>"));
                        return 1;
                    })
            );

            dispatcher.register(
                ClientCommandManager.literal("totemrestock")
                    .then(ClientCommandManager.literal("invopen")
                        .executes(ctx -> {
                            invOpen = !invOpen;
                            ctx.getSource().sendFeedback(Text.literal(
                                "§7[TotemRestock] §fInvOpen: " + (invOpen ? "§aON" : "§cOFF")));
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("checkhotbar")
                        .executes(ctx -> {
                            checkHotbar = !checkHotbar;
                            ctx.getSource().sendFeedback(Text.literal(
                                "§7[TotemRestock] §fCheckHotbar: " + (checkHotbar ? "§aON" : "§cOFF")));
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("offhand")
                        .executes(ctx -> {
                            restockOffhand = !restockOffhand;
                            ctx.getSource().sendFeedback(Text.literal(
                                "§7[TotemRestock] §fOffhand: " + (restockOffhand ? "§aON" : "§cOFF")));
                            return 1;
                        }))
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(Text.literal(
                            "§7[TotemRestock] §fSlot=§a" + hotbarSlot
                            + " §fInvOpen=§a" + invOpen
                            + " §fCheckHotbar=§a" + checkHotbar
                            + " §fOffhand=§a" + restockOffhand));
                        return 1;
                    })
            );
        });
    }

    @Override
    public void tick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        if (actionCooldown > 0) { actionCooldown--; return; }

        // InvOpen check
        if (invOpen && !(client.currentScreen instanceof InventoryScreen)) return;

        PlayerInventory inv = client.player.getInventory();

        // ── Offhand restock ──
        if (restockOffhand && inv.offHand.get(0).isEmpty()) {
            int from = findTotemSourceSlot(client);
            if (from != -1) {
                swap(client, from, OFFHAND_SLOT);
                actionCooldown = 4;   // brief gap
                return;
            }
        }

        // ── Hotbar slot restock ──
        // hotbarSlot (1..9) → screen index 36 + (hotbarSlot-1)
        int targetScreenSlot = 36 + (hotbarSlot - 1);
        int hotbarIdx = hotbarSlot - 1;   // 0..8 in inventory.main

        if (inv.main.get(hotbarIdx).isEmpty()) {
            int from = findTotemSourceSlot(client);
            if (from != -1 && from != targetScreenSlot) {
                swap(client, from, targetScreenSlot);
                actionCooldown = 4;
            }
        }
    }

    /** Find a screen-handler slot that holds a totem, excluding the target slots. */
    private int findTotemSourceSlot(MinecraftClient client) {
        var handler = client.player.currentScreenHandler;
        int targetHotbarScreenSlot = 36 + (hotbarSlot - 1);

        // Main inventory slots (9-35)
        for (int i = 9; i <= 35; i++) {
            if (i >= handler.slots.size()) break;
            ItemStack s = handler.slots.get(i).getStack();
            if (s.isOf(Items.TOTEM_OF_UNDYING)) return i;
        }

        // Hotbar (36-44) if enabled
        if (checkHotbar) {
            for (int i = 36; i <= 44; i++) {
                if (i >= handler.slots.size()) break;
                if (i == targetHotbarScreenSlot) continue;   // skip target itself
                ItemStack s = handler.slots.get(i).getStack();
                if (s.isOf(Items.TOTEM_OF_UNDYING)) return i;
            }
        }

        return -1;
    }

    /** Perform pickup → place → (drop carried) using slot click packets. */
    private void swap(MinecraftClient client, int from, int to) {
        int syncId = client.player.currentScreenHandler.syncId;

        // 1) pick up totem
        client.interactionManager.clickSlot(syncId, from, 0, SlotActionType.PICKUP, client.player);
        // 2) place into target
        client.interactionManager.clickSlot(syncId, to, 0, SlotActionType.PICKUP, client.player);
        // 3) if there's anything on the cursor still (because target slot had something),
        //    put it back where the totem came from
        if (!client.player.currentScreenHandler.getCursorStack().isEmpty()) {
            client.interactionManager.clickSlot(syncId, from, 0, SlotActionType.PICKUP, client.player);
        }
    }
}
