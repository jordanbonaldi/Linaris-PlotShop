package net.neferett.plotshop.commands;

import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.neferett.plotshop.Plot;
import net.neferett.plotshop.Plotshop;
import net.neferett.plotshop.Plot.State;

public class SellCommand implements CommandExecutor {
    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Vous devez être un joueur !");
            return true;
        } else if (args.length != 1 || !args[0].matches("(\\d+\\.\\d+)|(\\d+)")) {
            sender.sendMessage(ChatColor.RED + "Vous devez indiquer le prix de votre parcelle.");
            sender.sendMessage(ChatColor.RED + "Exemple : " + ChatColor.DARK_RED + "/vente prix");
            return true;
        }
        final String priceString = args[0].replace(".", ",");
        final double price = Double.parseDouble(priceString.replace(",", "."));
        final Player player = (Player) sender;
        final RegionManager regionManager = Plotshop.worldGuard.getRegionManager(player.getWorld());
        final ApplicableRegionSet applicableRegion = regionManager.getApplicableRegions(player.getLocation());
        ProtectedRegion region = null;
        for (final ProtectedRegion rg : applicableRegion) {
            if (rg.getParent() != null) {
                region = rg;
            }
        }
        if (region == null) {
            for (final ProtectedRegion rg : applicableRegion) {
                region = rg;
                break;
            }
        }
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Vous devez être dans une de vos parcelles.");
        } else {
            final Plot plot = Plotshop.getInstance().getDatabase().find(Plot.class).where().ieq("worldName", player.getLocation().getWorld().getName()).ieq("regionName", region.getId()).ieq("playerName", player.getName()).ieq("purchased", "1").findUnique();
            if (plot == null) {
                player.sendMessage(ChatColor.RED + "Vous devez être dans une de vos parcelles.");
            } else {
                Location location = null;
                if (!Plotshop.availableSigns.isEmpty()) {
                    int trials = 0;
                    final Iterator<Location> iterator = Plotshop.availableSigns.iterator();
                    while (iterator.hasNext() && trials < 10) {
                        location = iterator.next();
                        if (location.getBlock().getState() instanceof Sign) {
                            break;
                        }
                        trials++;
                    }
                }
                if (Plotshop.availableSigns.isEmpty() || location == null) {
                    player.sendMessage(ChatColor.RED + "L'agence n'a plus de place, attendez que des pancartes se libèrent ou contactez un administrateur.");
                    return true;
                }
                final Sign sign = (Sign) location.getBlock().getState();
                final String key = "plotshop.sell." + sign.getWorld().getName() + "_" + sign.getX() + "_" + sign.getY() + "_" + sign.getZ();
                if (!player.hasMetadata(key) || player.getMetadata(key).get(0).asDouble() != price) {
                    player.sendMessage(ChatColor.GREEN + "Vous êtes sur le point de mettre en vente votre parcelle pour " + priceString + " " + (price > 1 ? Plotshop.economy.currencyNamePlural() : Plotshop.economy.currencyNameSingular()) + ". Pour confirmer, faites la commande " + ChatColor.AQUA + "/vente " + priceString);
                    player.removeMetadata(key, Plotshop.getInstance());
                    player.setMetadata(key, new FixedMetadataValue(Plotshop.getInstance(), price));
                } else {
                    player.sendMessage(ChatColor.GREEN + "Vous venez de mettre en vente votre parcelle au prix de " + priceString + " " + (price > 1 ? Plotshop.economy.currencyNamePlural() : Plotshop.economy.currencyNameSingular()));
                    player.removeMetadata(key, Plotshop.getInstance());
                    Plotshop.unavailableSigns.add(location);
                    Plotshop.availableSigns.remove(location);
                    plot.setPrice(price);
                    plot.setPurchased(false);
                    plot.setState(State.AVAILABLE);
                    plot.setX(sign.getX());
                    plot.setY(sign.getY());
                    plot.setZ(sign.getZ());
                    Plotshop.getInstance().getDatabase().save(plot);

                    final BlockVector min = region.getMinimumPoint();
                    final BlockVector max = region.getMaximumPoint();
                    final Integer sizeX = Integer.valueOf(max.getBlockX() - min.getBlockX() + 1);
                    final Integer sizeY = Integer.valueOf(max.getBlockY() - min.getBlockY() + 1);
                    final Integer sizeZ = Integer.valueOf(max.getBlockZ() - min.getBlockZ() + 1);

                    final List<String> lines = Plotshop.getInstance().getConfig().getStringList("sign");
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        line = ChatColor.translateAlternateColorCodes('&', line.replace("[sizeX]", sizeX + "").replace("[sizeY]", sizeY + "").replace("[sizeZ]", sizeZ + "").replace("[price]", priceString).replace("[playerName]", player.getName()).replace("[currencyName]", price > 1 ? Plotshop.economy.currencyNamePlural() : Plotshop.economy.currencyNameSingular()).replace("[regionName]", region.getId()).replace("[state]", Plotshop.getInstance().getConfig().getStringList("state").get(0)));
                        lines.set(i, line);
                    }
                    sign.setLine(0, lines.get(0));
                    sign.setLine(1, lines.get(1));
                    sign.setLine(2, lines.get(2));
                    sign.setLine(3, lines.get(3));
                    sign.update();
                }
            }
        }
        return true;
    }
}
