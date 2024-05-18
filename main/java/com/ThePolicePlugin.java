package com.example.thepoliceplugin;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class ThePolicePlugin extends JavaPlugin implements Listener {

    private final Set<String> mutedPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        getLogger().info("ThePolicePlugin has been enabled");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("ThePolicePlugin has been disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Please specify a player and time.");
            return false;
        }

        String targetName = args[0];
        String time = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage("Player not found.");
            return false;
        }

        switch (command.getName().toLowerCase()) {
            case "ban":
                openConfirmationInventory((Player) sender, target, "ban", time);
                break;

            case "mute":
                openConfirmationInventory((Player) sender, target, "mute", time);
                break;

            case "kick":
                openConfirmationInventory((Player) sender, target, "kick", time);
                break;
        }

        return true;
    }

    private void openConfirmationInventory(Player sender, Player target, String action, String time) {
        Inventory confirmInventory = Bukkit.createInventory(null, 9, "Confirm " + action + " " + target.getName());

        ItemStack confirm = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("Confirm");
        confirm.setItemMeta(confirmMeta);

        ItemStack deny = new ItemStack(Material.RED_WOOL);
        ItemMeta denyMeta = deny.getItemMeta();
        denyMeta.setDisplayName("Deny");
        deny.setItemMeta(denyMeta);

        confirmInventory.setItem(3, confirm);
        confirmInventory.setItem(5, deny);

        sender.openInventory(confirmInventory);

        // Store the action and time in the player's metadata
        sender.setMetadata("action", new FixedMetadataValue(this, action));
        sender.setMetadata("target", new FixedMetadataValue(this, target.getName()));
        sender.setMetadata("time", new FixedMetadataValue(this, time));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Confirm")) {
            event.setCancelled(true);

            Player sender = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            String action = sender.getMetadata("action").get(0).asString();
            String targetName = sender.getMetadata("target").get(0).asString();
            String time = sender.getMetadata("time").get(0).asString();
            Player target = Bukkit.getPlayer(targetName);

            if (target == null) {
                sender.sendMessage("Player not found.");
                sender.closeInventory();
                return;
            }

            if (clickedItem.getType() == Material.GREEN_WOOL) {
                switch (action) {
                    case "ban":
                        target.kickPlayer("You have been banned for " + time + "!");
                        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, "You have been banned for " + time + "!", null, sender.getName());
                        Bukkit.broadcastMessage(ChatColor.RED + "[POLICE] " + ChatColor.GOLD + sender.getName() + " has banned " + targetName + " for " + time + ". Justice has been made!");
                        break;
                    case "mute":
                        if (mutedPlayers.contains(targetName)) {
                            mutedPlayers.remove(targetName);
                            sender.sendMessage(targetName + " has been unmuted.");
                        } else {
                            mutedPlayers.add(targetName);
                            Bukkit.broadcastMessage(ChatColor.RED + "[POLICE] " + ChatColor.GOLD + sender.getName() + " has muted " + targetName + " for " + time + ". Justice has been made!");
                        }
                        break;
                    case "kick":
                        target.kickPlayer("You have been kicked for " + time + "!");
                        Bukkit.broadcastMessage(ChatColor.RED + "[POLICE] " + ChatColor.GOLD + sender.getName() + " has kicked " + targetName + " for " + time + ". Justice has been made!");
                        break;
                }
            } else if (clickedItem.getType() == Material.RED_WOOL) {
                sender.sendMessage(action + " action cancelled.");
            }

            sender.closeInventory();
        }
    }
}
