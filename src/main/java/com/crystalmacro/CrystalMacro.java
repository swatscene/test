package com.crystalmacro;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class CrystalMacro implements ClientModInitializer {

    private static final double REACH = 5.0;
    private static final double REACH_SQ = REACH * REACH;
    private static final int COOLDOWN_T = 2;
    private static final int PLACEMENT_WINDOW_TICKS = 12;
    private static final double MATCH_RADIUS = 0.7;

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.register(Identifier.of("crystalmacro", "main"));

    private static boolean enabled = false;
    private static int cooldown = 0;
    private static int tickNum = 0;

    private static final Set<UUID> myCrystals = new HashSet<>();
    private static final Set<UUID> seenCrystals = new HashSet<>();
    private static final List<PendingPlace> pending = new ArrayList<>();

    private static KeyBinding toggleKey;

    private record PendingPlace(Vec3d pos, int tick) {}

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.crystalmacro.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickNum++;

            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("§7[CrystalMacro] §f"
                                    + (enabled ? "§aENABLED" : "§cDISABLED")),
                            true);
                }
                if (!enabled) {
                    myCrystals.clear();
                    pending.clear();
                }
            }

            if (!enabled) return;
            if (client.player == null || client.world == null) return;
            if (client.interactionManager == null) return;

            boolean useDown = client.options.useKey.isPressed();
            boolean hasCrystal =
                    client.player.getMainHandStack().isOf(Items.END_CRYSTAL) ||
                    client.player.getOffHandStack().isOf(Items.END_CRYSTAL);

            if (useDown && hasCrystal && client.crosshairTarget instanceof BlockHitResult bhr
                    && bhr.getType() == HitResult.Type.BLOCK) {
                BlockPos bp = bhr.getBlockPos();
                Vec3d expected = new Vec3d(bp.getX() + 0.5, bp.getY() + 1.0, bp.getZ() + 0.5);
                boolean dup = pending.stream()
                        .anyMatch(p -> p.pos.squaredDistanceTo(expected) < 0.1);
                if (!dup) pending.add(new PendingPlace(expected, tickNum));
            }
            pending.removeIf(p -> tickNum - p.tick() > PLACEMENT_WINDOW_TICKS);

            Set<UUID> present = new HashSet<>();
            for (var e : client.world.getEntities()) {
                if (!(e instanceof EndCrystalEntity c)) continue;
                UUID id = c.getUuid();
                present.add(id);
                if (seenCrystals.contains(id)) continue;

                Vec3d cp = new Vec3d(c.getX(), c.getY(), c.getZ());
                Iterator<PendingPlace> it = pending.iterator();
                while (it.hasNext()) {
                    PendingPlace p = it.next();
                    if (p.pos().squaredDistanceTo(cp) < MATCH_RADIUS * MATCH_RADIUS) {
                        myCrystals.add(id);
                        it.remove();
                        break;
                    }
                }
            }
            seenCrystals.clear();
            seenCrystals.addAll(present);
            myCrystals.retainAll(present);

            if (cooldown > 0) { cooldown--; return; }
            if (!useDown || !hasCrystal) return;

            EndCrystalEntity nearest = null;
            double best = REACH_SQ;
            for (var e : client.world.getEntities()) {
                if (!(e instanceof EndCrystalEntity c)) continue;
                if (!myCrystals.contains(c.getUuid())) continue;
                double d = c.squaredDistanceTo(client.player);
                if (d < best) { best = d; nearest = c; }
            }
            if (nearest != null) {
                client.interactionManager.attackEntity(client.player, nearest);
                client.player.swingHand(client.player.getActiveHand());
                cooldown = COOLDOWN_T;
            }
        });
    }
}
