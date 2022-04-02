package net.tagucha.plugin3DMG;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class PluginMain extends JavaPlugin {
    private final ManeuverGear GEAR_RIGHT =
            new ManeuverGear(
                    this,
                    event -> (event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK)) && event.getHand() == EquipmentSlot.HAND,
                    player -> player.getInventory().getItemInMainHand().isSimilar(ManeuverGear.MANEUVER_GEAR)
                    );
    private final ManeuverGear GEAR_LEFT =
            new ManeuverGear(
                    this,
                    event -> (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && event.getHand() == EquipmentSlot.OFF_HAND,
                    player -> player.getInventory().getItemInOffHand().isSimilar(ManeuverGear.MANEUVER_GEAR)
            );

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
        PluginManager manager = this.getServer().getPluginManager();
        manager.registerEvents(GEAR_RIGHT, this);
        manager.registerEvents(GEAR_LEFT, this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            player.getInventory().addItem(ManeuverGear.MANEUVER_GEAR);
        }
        return true;
    }
}
