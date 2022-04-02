package net.tagucha.plugin3DMG;

import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityFishingHook;
import net.minecraft.world.level.World;

public class EntityHook extends EntityFishingHook {
    private int tick;

    public EntityHook(EntityHuman entityhuman, World world, int i, int j) {
        super(entityhuman, world, i, j);
    }

    @Override
    public void k() {
        if (tick++ > 1200) this.ah();
    }
}
