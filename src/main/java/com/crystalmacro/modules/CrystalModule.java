package com.crystalmacro.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class CrystalModule extends Module {

    public static double reach           = 5.0;
    public static int    hitsPerTick     = 5;
    public static int    placementWindow = 25;
    public static double matchRadius     = 1.0;

    private int tickNum = 0;
    private boolean wasUsing = false;
    private final Set<UUID> myCrystals   = new HashSet<>();
    private final Set<UUID> seenCrystals = new HashSet<>();
    private final List<PendingPlace> pending = new ArrayList<>();

    private record PendingPlace(Vec3d pos, int tick) {}

    public CrystalModule() {
        super("CrystalMacro", "key.crystalmacro.toggle", GLFW.GLFW_KEY_R);
    }

    @Override
    protected void onDisable() {
        myCrystals.clear();
        pending.clear();
        seenCrystals.clear();
    }

    @Override
    public void tick(MinecraftClient client) {
        tickNum++;
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        boolean useDown = client.options.useKey.isPressed();
        boolean hasCrystal = client.player.getMainHandStack().isOf(Items.END_CRYSTAL)
                          || client.player.getOffHandStack().isOf(Items.END_CRYSTAL);

        // ── Spam-stall fix: if user just released right-click, wipe stale state ──
        if (wasUsing && !useDown) {
            // Don't fully clear myCrystals (we may still need to break them),
            // but clear seen so new crystals get re-evaluated next press
            pending.clear();
        }
        wasUsing = useDown;

        // ── Record placements ──
        if (useDown && hasCrystal && client.crosshairTarget instanceof BlockHitResult bhr
                && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = bhr.getBlockPos();
            // Crystals can spawn on the top of the targeted block OR the block above
            for (int dy = 1; dy <= 1; dy++) {
                Vec3d expected = new Vec3d(bp.getX() + 0.5, bp.getY() + dy, bp.getZ() + 0.5);
                boolean dup = pending.stream().anyMatch(p -> p.pos.squaredDistanceTo(expected) < 0.05);
                if (!dup) pending.add(new PendingPlace(expected, tickNum));
            }
        }
        pending.removeIf(p -> tickNum - p.tick() > placementWindow);

        // ── Discover new crystals; match to placements ──
        Set<UUID> present = new HashSet<>();
        List<EndCrystalEntity> allCrystals = new ArrayList<>();
        for (var e : client.world.getEntities()) {
            if (!(e instanceof EndCrystalEntity c)) continue;
            present.add(c.getUuid());
            allCrystals.add(c);

            if (seenCrystals.contains(c.getUuid())) continue;

            Vec3d cp = new Vec3d(c.getX(), c.getY(), c.getZ());
            Iterator<PendingPlace> it = pending.iterator();
            while (it.hasNext()) {
                PendingPlace p = it.next();
                if (p.pos().squaredDistanceTo(cp) < matchRadius * matchRadius) {
                    myCrystals.add(c.getUuid());
                    it.remove();
                    break;
                }
            }
        }
        seenCrystals.clear();
        seenCrystals.addAll(present);
        myCrystals.retainAll(present);

        if (!useDown || !hasCrystal) return;

        // ── Attack ──
        double reachSq = reach * reach;
        List<EndCrystalEntity> targets = new ArrayList<>();
        for (EndCrystalEntity c : allCrystals) {
            if (!myCrystals.contains(c.getUuid())) continue;
            if (c.squaredDistanceTo(client.player) <= reachSq) targets.add(c);
        }
        targets.sort(Comparator.comparingDouble(c -> c.squaredDistanceTo(client.player)));

        int hits = 0;
        for (EndCrystalEntity c : targets) {
            if (hits >= hitsPerTick) break;
            client.interactionManager.attackEntity(client.player, c);
            client.player.swingHand(client.player.getActiveHand());
            myCrystals.remove(c.getUuid());
            hits++;
        }
    }
}
