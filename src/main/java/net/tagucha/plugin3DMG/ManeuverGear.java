package net.tagucha.plugin3DMG;

import net.minecraft.core.BlockPosition;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class ManeuverGear implements Listener {
    public static final ItemStack MANEUVER_GEAR;
    private static final double SPEED = 2.5;
    private static final double PULL_FORCE = 0.5;
    private static final double MAX_FULL_FORCE = 2.0;
    private static final double CANCEL_DISTANCE = 2.0;

    static {
        MANEUVER_GEAR = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = MANEUVER_GEAR.getItemMeta();
        meta.setDisplayName(String.format("%s%s立体機動装置", ChatColor.GOLD, ChatColor.BOLD));
        meta.setCustomModelData(1002);
        meta.setUnbreakable(true);
        MANEUVER_GEAR.setItemMeta(meta);
    }

    private final PluginMain plugin;
    private final Predicate<PlayerInteractEvent> which;
    private final Predicate<Player> has_gear;

    private final Map<Player, EntityHook> hooks = new HashMap<>();
    private final Map<Player, Arrow> arrows = new HashMap<>();
    private final Map<Player, BukkitRunnable> flying = new HashMap<>();

    public ManeuverGear(PluginMain plugin, Predicate<PlayerInteractEvent> which, Predicate<Player> has_gear) {
        this.plugin = plugin;
        this.which = which;
        this.has_gear = has_gear;
    }

    private void finishPull(Player player) {
        Optional.ofNullable(flying.remove(player)).filter(runnable -> !runnable.isCancelled()).ifPresent(BukkitRunnable::cancel);
        Optional.ofNullable(arrows.remove(player)).filter(arrow -> !arrow.isDead()).ifPresent(Entity::remove);
        EntityHook hook = this.hooks.remove(player);
        hook.ah();
    }

    @EventHandler
    public void onClickGear(PlayerInteractEvent event) {
        if (!this.which.test(event)) return;
        event.setCancelled(true);
        Player user = event.getPlayer();
        if (!this.has_gear.test(user)) return;
        if (this.flying.containsKey(user)) {
            this.finishPull(user);
        } else if (arrows.containsKey(user)) {
            if (!this.arrows.get(user).isOnGround()) return;
            FlyScheduler scheduler = new FlyScheduler(this, user);
            this.flying.put(user,scheduler);
            scheduler.runTaskLater(this.plugin, 1);
        } else {
            Vector vector = user.getLocation().getDirection().multiply(SPEED);

            Arrow arrow = user.launchProjectile(Arrow.class, vector);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);

            EntityHook hooky = spawnHook(user);
            FishHook hook = (FishHook) hooky.getBukkitEntity();
            hook.setApplyLure(false);
            hook.setInvulnerable(true);
            hook.setHookedEntity(arrow);

            hooks.put(user, hooky);
            arrows.put(user, arrow);
        }
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        this.hooks.values().forEach(net.minecraft.world.entity.Entity::ah);
        this.arrows.values().forEach(Entity::remove);
    }

    public static EntityHook spawnHook(Player player) {
        Location loc = player.getLocation();
        EntityHook hook = new EntityHook(((CraftPlayer) player).getHandle(), ((CraftWorld) player.getWorld()).getHandle(), 0, 0);
        hook.a(new BlockPosition(loc.getX(), loc.getY(), loc.getZ()), loc.getPitch(), loc.getYaw());
        ((CraftPlayer)player).getHandle().t.addFreshEntity(hook, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return hook;
    }

    private static class FlyScheduler extends BukkitRunnable {
        private final ManeuverGear maneuverGear;
        private final Player user;

        public FlyScheduler(ManeuverGear maneuverGear, Player user) {
            this.maneuverGear = maneuverGear;
            this.user = user;
        }

        @Override
        public void run() {
            Arrow arrow = this.maneuverGear.arrows.get(user);
            if (arrow.isDead() || arrow.getLocation().distance(user.getLocation()) < CANCEL_DISTANCE) {
                this.maneuverGear.finishPull(user);
                this.cancel();
                return;
            }
            Vector base = user.getVelocity();
            double vecX = arrow.getLocation().getX() - user.getLocation().getX();
            double vecY = arrow.getLocation().getY() - user.getLocation().getY();
            double vecZ = arrow.getLocation().getZ() - user.getLocation().getZ();
            Vector vec = new Vector(vecX, vecY, vecZ);
            vec = vec.add(new Vector(0, vec.length() / 10, 0)).multiply(PULL_FORCE / vec.length());
            Vector next = base.add(vec);
            if (next.length() > MAX_FULL_FORCE) next = next.multiply(MAX_FULL_FORCE / next.length());
            user.setVelocity(next);
            FlyScheduler scheduler = new FlyScheduler(this.maneuverGear, user);
            this.maneuverGear.flying.put(user,scheduler);
            scheduler.runTaskLater(this.maneuverGear.plugin, 1);
        }
    }
}
