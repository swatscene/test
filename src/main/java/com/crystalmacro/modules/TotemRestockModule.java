package com.crystalmacro.modules;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class TotemRestockModule extends Module {

    public static int hotbarSlot = 9;
    public static boolean invOpen = true;
    public static boolean checkHotbar = true;
    public static boolean restockOffhand = true;

    private static final int OFFHAND_SLOT = 45;

    private int actionCooldown = 0;
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

        if (invOpen && !(client.currentScreen instanceof InventoryScreen)) return;

        PlayerInventory inv = client.player.getInventory();

        // Offhand restock
        if (restockOffhand && client.player.getOffHandStack().isEmpty()) {
            int from = findTotemSourceSlot(client);
            if (from != -1) {
                swap(client, from, OFFHAND_SLOT);
                actionCooldown = 4;
                return;
            }
        }

        // Hotbar slot restock
        int targetScreenSlot = 36 + (hotbarSlot - 1);
        int hotbarIdx = hotbarSlot - 1;

        if (inv.getStack(hotbarIdx).isEmpty()) {
            int from = findTotemSourceSlot(client);
            if (from != -1 && from != targetScreenSlot) {
                swap(client, from, targetScreenSlot);
                actionCooldown = 4;
            }
        }
    }

    private int findTotemSourceSlot(MinecraftClient client) {
        var handler = client.player.currentScreenHandler;
        int targetHotbarScreenSlot = 36 + (hotbarSlot - 1);

        for (int i = 9; i <= 35; i++) {
            if (i >= handler.slots.size()) break;
            ItemStack s = handler.slots.get(i).getStack();
            if (s.isOf(Items.TOTEM_OF_UNDYING)) return i;
        }

        if (checkHotbar) {
            for (int i = 36; i <= 44; i++) {
                if (i >= handler.slots.size()) break;
                if (i == targetHotbarScreenSlot) continue;
                ItemStack s = handler.slots.get(i).getStack();
                if (s.isOf(Items.TOTEM_OF_UNDYING)) return i;
            }
        }

        return -1;
    }

    private void swap(MinecraftClient client, int from, int to) {
        int syncId = client.player.currentScreenHandler.syncId;
        client.interactionManager.clickSlot(syncId, from, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, to, 0, SlotActionType.PICKUP, client.player);
        if (!client.player.currentScreenHandler.getCursorStack().isEmpty()) {
            client.interactionManager.clickSlot(syncId, from, 0, SlotActionType.PICKUP, client.player);
        }
    }
}
