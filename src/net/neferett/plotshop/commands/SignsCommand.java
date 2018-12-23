package net.neferett.plotshop.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.selections.Selection;

import net.neferett.plotshop.Plotshop;

public class SignsCommand implements PSCommand {
    @Override
    public String getPermission() {
        return "plotshop.signs";
    }

    @Override
    public String help(final Player player) {
        if (Plotshop.hasPermission(player, this.getPermission())) { return "/plotshop signs - Ajoute des pancartes pour la revente"; }
        return "";
    }

    @Override
    public boolean onCommand(final Player player, final String[] params) {
        final Selection selection = Plotshop.worldEdit.getSelection(player);
        if (selection == null) {
            player.sendMessage(ChatColor.RED + "Vous devez faire une sélection avec WorldEdit.");
            return true;
        }
        final Location min = selection.getMinimumPoint();
        final Location max = selection.getMaximumPoint();
        final int minX = min.getBlockX();
        final int minY = min.getBlockY();
        final int minZ = min.getBlockZ();
        final int maxX = max.getBlockX();
        final int maxY = max.getBlockY();
        final int maxZ = max.getBlockZ();
        boolean success = true;
        final List<Location> locations = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            if (!success) {
                break;
            }
            for (int y = minY; y <= maxY; y++) {
                if (!success) {
                    break;
                }
                for (int z = minZ; z <= maxZ; z++) {
                    final Location location = new Location(player.getWorld(), x, y, z);
                    if (!(location.getBlock().getState() instanceof Sign)) {
                        success = false;
                        break;
                    } else {
                        locations.add(location);
                    }
                }
            }
        }
        if (!success) {
            player.sendMessage(ChatColor.RED + "Votre séléction ne doit contenir que des pancartes.");
        } else {
            int count = 0;
            for (final Location location : locations) {
                Plotshop.availableSigns.add(location);
                count++;
            }
            player.sendMessage(ChatColor.GREEN + "Vous venez de définir " + ChatColor.AQUA + count + ChatColor.GREEN + " nouveau" + (count > 1 ? "x" : "") + " panneau" + (count > 1 ? "x" : ""));
        }
        return true;
    }
}
