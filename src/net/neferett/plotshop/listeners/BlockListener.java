package net.neferett.plotshop.listeners;

import static net.neferett.plotshop.Plot.State.UNAVAILABLE;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.neferett.plotshop.Plot;
import net.neferett.plotshop.Plotshop;
import net.neferett.plotshop.RegionCreator;
import net.neferett.plotshop.Plot.State;
import net.neferett.plotshop.Plot.Type;

public class BlockListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block.getState() instanceof Sign) {
            final Sign sign = (Sign) block.getState();
            final Plot plot = Plotshop.getInstance().getDatabase().find(Plot.class).where().ieq("worldName", sign.getWorld().getName()).ieq("x", sign.getX() + "").ieq("y", sign.getY() + "").ieq("z", sign.getZ() + "").findUnique();
            if (plot != null) {
                final Location location = sign.getLocation();
                final Player player = event.getPlayer();
                if (plot.getPlayerName().equals(player.getName()) || Plotshop.hasPermission(player, "plotshop.admin")) {
                    player.sendMessage(ChatColor.YELLOW + "Vous venez d'annuler la vente de " + (plot.getPlayerName().equals(player.getName()) ? "votre" : "la") + " parcelle" + (plot.getPlayerName().equals(player.getName()) ? "" : " de " + plot.getPlayerName()));
                    if (!plot.isPurchased() && !Plotshop.unavailableSigns.contains(location)) {
                        if (plot.getType() == Type.REGION_CREATOR) {
                            final RegionManager regionManager = Plotshop.worldGuard.getRegionManager(block.getWorld());
                            regionManager.removeRegion(plot.getRegionName());
                            try {
                                regionManager.save();
                                player.sendMessage(ChatColor.GREEN + "R�gion WorldGuard \"" + plot.getRegionName() + "\" supprim�e");
                            } catch (final ProtectionDatabaseException ex) {
                                ex.printStackTrace();
                                player.sendMessage(ChatColor.RED + "Impossible de sauvegarder les r�gions WorldGuard");
                            }
                        }
                        Plotshop.getInstance().getDatabase().delete(plot);
                    } else {
                        Plotshop.unavailableSigns.remove(location);
                        plot.setX(-1);
                        plot.setY(-1);
                        plot.setZ(-1);
                        plot.setPrice(-1);
                        plot.setState(UNAVAILABLE);
                        Plotshop.getInstance().getDatabase().save(plot);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSignChange(final SignChangeEvent event) {
        final boolean createRegion = event.getLine(0).equalsIgnoreCase("[plotshop2]");
        if (event.getLine(0).equalsIgnoreCase("[plotshop]") || createRegion) {
            final Player player = event.getPlayer();
            if (!Plotshop.hasPermission(player, "plotshop.create" + (createRegion ? "2" : ""))) {
                player.sendMessage(ChatColor.RED + "Permission non accord�e");
            } else if (!event.getLine(1).matches("(\\d+\\.\\d+)|(\\d+)")) {
                player.sendMessage(ChatColor.RED + "Panneau invalide");
            } else {
                final Block block = event.getBlock();
                final Location location = block.getLocation();
                String regionName = "";
                if (!event.getLine(2).isEmpty()) {
                    regionName = event.getLine(2);
                } else if (createRegion) {
                    regionName = location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
                } else {
                    final RegionManager regionManager = Plotshop.worldGuard.getRegionManager(player.getWorld());
                    final ApplicableRegionSet applicableRegion = regionManager.getApplicableRegions(block.getLocation());
                    int found = 0;

                    for (final ProtectedRegion rg : applicableRegion) {
                        if (rg.getParent() != null) {
                            found++;
                            regionName = rg.getId();
                        }
                    }
                    if (regionName.isEmpty()) {
                        for (final ProtectedRegion rg : applicableRegion) {
                            found++;
                            regionName = rg.getId();
                        }
                    }

                    if (found == 1) {
                        player.sendMessage(ChatColor.GREEN + "R�gion WorldGuard \"" + regionName + "\" trouv�e");
                    } else {
                        player.sendMessage(ChatColor.RED + "Aucune ou plusieurs r�gions d�tect�es");
                        block.breakNaturally();
                    }

                    player.sendMessage(ChatColor.GRAY + "Il se peut que la pancarte ne d�tecte pas la bonne r�gion, dans ce cas mettez le nom de la r�gion sur la troisi�me ligne du panneau");

                    if (found != 1) { return; }
                }
                ProtectedRegion region = null;
                if (createRegion) {
                    int up = 0;
                    int bottom = 0;
                    if (!event.getLine(3).isEmpty()) {
                        final String[] size = event.getLine(3).split(";");
                        if (size.length != 2 || !size[0].matches("[0-9]+") || !size[1].matches("[0-9]+")) {
                            player.sendMessage(ChatColor.RED + "Panneau invalide");
                            return;
                        } else {
                            up = Integer.parseInt(size[0]);
                            bottom = Integer.parseInt(size[1]);
                        }
                    }
                    region = new RegionCreator().createRegion((Sign) block.getState(), regionName, player, up, bottom);
                } else {
                    region = Plotshop.worldGuard.getRegionManager(player.getWorld()).getRegion(regionName);
                }

                if (region == null) {
                    if (!createRegion) {
                        player.sendMessage(ChatColor.RED + "R�gion WorldGuard \"" + regionName + "\" introuvable");
                    }
                    block.breakNaturally();
                } else {
                    Plot plot = createRegion ? null : Plotshop.getInstance().getDatabase().find(Plot.class).where().ieq("worldName", player.getLocation().getWorld().getName()).ieq("regionName", regionName).ieq("playerName", player.getName()).ieq("purchased", "1").findUnique();
                    if (plot == null) {
                        plot = new Plot();
                        plot.setPlayerName(!event.getLine(3).isEmpty() ? event.getLine(3) : player.getName());
                        plot.setRegionName(regionName);
                    }
                    plot.setLocation(block.getLocation());
                    plot.setPrice(Double.parseDouble(event.getLine(1).replace(",", ".")));
                    plot.setState(State.AVAILABLE);
                    plot.setType(createRegion ? Type.REGION_CREATOR : Type.REGION);
                    Plotshop.getInstance().getDatabase().save(plot);

                    final BlockVector min = region.getMinimumPoint();
                    final BlockVector max = region.getMaximumPoint();
                    final Integer sizeX = Integer.valueOf(max.getBlockX() - min.getBlockX() + 1);
                    final Integer sizeY = Integer.valueOf(max.getBlockY() - min.getBlockY() + 1);
                    final Integer sizeZ = Integer.valueOf(max.getBlockZ() - min.getBlockZ() + 1);

                    final List<String> lines = Plotshop.getInstance().getConfig().getStringList("sign");
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        line = ChatColor.translateAlternateColorCodes('&', line.replace("[sizeX]", sizeX + "").replace("[sizeY]", sizeY + "").replace("[sizeZ]", sizeZ + "").replace("[price]", event.getLine(1).replace(".", ",")).replace("[playerName]", player.getName()).replace("[currencyName]", Double.parseDouble(event.getLine(1)) > 1 ? Plotshop.economy.currencyNamePlural() : Plotshop.economy.currencyNameSingular()).replace("[regionName]", regionName).replace("[state]", Plotshop.getInstance().getConfig().getStringList("state").get(0)));
                        lines.set(i, line);
                    }
                    event.setLine(0, lines.get(0));
                    event.setLine(1, lines.get(1));
                    event.setLine(2, lines.get(2));
                    event.setLine(3, lines.get(3));
                }
            }
        }
    }
}
