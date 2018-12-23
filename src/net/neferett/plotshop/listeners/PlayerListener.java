package net.neferett.plotshop.listeners;

import static net.neferett.plotshop.Plot.State.AVAILABLE;
import static net.neferett.plotshop.Plot.State.UNAVAILABLE;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.neferett.plotshop.Plot;
import net.neferett.plotshop.Plotshop;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Block clickedBlock = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clickedBlock != null) {
            if (clickedBlock.getState() instanceof Sign) {
                final Sign sign = (Sign) clickedBlock.getState();
                final List<Plot> plots = Plotshop.getInstance().getDatabase().find(Plot.class).where().ieq("worldName", sign.getWorld().getName()).ieq("x", sign.getX() + "").ieq("y", sign.getY() + "").ieq("z", sign.getZ() + "").findList();
                if (plots.size() == 0) { return; }
                final Plot plot = plots.get(0);
                if (plot != null) {
                    final Player player = event.getPlayer();
                    event.setCancelled(true);
                    if (plot.getPlayerName().equals(player.getName()) || Plotshop.hasPermission(player, "plotshop.admin")) {
                        if (plot.getState() == UNAVAILABLE) {
                            plot.setState(AVAILABLE);
                            sign.setLine(3, ChatColor.translateAlternateColorCodes('&', Plotshop.getInstance().getConfig().getStringList("state").get(0)));
                            player.sendMessage(ChatColor.GREEN + "La parcelle est maintenant disponible à l'achat");
                        } else {
                            plot.setState(UNAVAILABLE);
                            sign.setLine(3, ChatColor.translateAlternateColorCodes('&', Plotshop.getInstance().getConfig().getStringList("state").get(1)));
                            player.sendMessage(ChatColor.GREEN + "La parcelle est maintenant indisponible à l'achat");
                        }
                        sign.update();
                        Plotshop.getInstance().getDatabase().save(plot);
                    } else if (plot.getState() == UNAVAILABLE) {
                        player.sendMessage(ChatColor.RED + "La parcelle est indisponible à l'achat");
                    } else if (!Plotshop.economy.has(player.getName(), plot.getPrice())) {
                        player.sendMessage(ChatColor.RED + "Vous n'avez pas assez d'argent");
                    } else if (!player.hasMetadata("plotshop.click." + sign.getWorld().getName() + "_" + sign.getX() + "_" + sign.getY() + "_" + sign.getZ())) {
                        player.sendMessage(ChatColor.GREEN + "Vous êtes sur le point d'acheter une parcelle en échange de " + String.valueOf(plot.getPrice()).replace(".", ",") + " " + (plot.getPrice() > 1 ? Plotshop.economy.currencyNamePlural() : Plotshop.economy.currencyNameSingular()) + ". Pour confirmer cet achat, faites de nouveau un clic droit sur cette pancarte.");
                        player.setMetadata("plotshop.click." + sign.getWorld().getName() + "_" + sign.getX() + "_" + sign.getY() + "_" + sign.getZ(), new FixedMetadataValue(Plotshop.getInstance(), true));
                    } else {
                        final int maxPlotsCountPerPlayer = Plotshop.getInstance().getConfig().getInt("maxPlotsCountPerPlayer");
                        final List<Plot> playerPlots = Plotshop.getInstance().getDatabase().find(Plot.class).where().ieq("playerName", player.getName()).ieq("purchased", "1").findList();
                        final RegionManager regionManager = Plotshop.worldGuard.getRegionManager(player.getWorld());
                        final ProtectedRegion region = regionManager.getRegion(plot.getRegionName());
                        if (region == null) {
                            player.sendMessage(ChatColor.RED + "Impossible d'acheter la parcelle, contactez un administrateur");
                        } else if (playerPlots.size() >= maxPlotsCountPerPlayer) {
                            player.sendMessage(ChatColor.RED + "Vous avez atteint la limite de parcelles par joueur (" + playerPlots.size() + "/" + maxPlotsCountPerPlayer + ")");
                        } else {
                            for (final String ownerName : region.getOwners().getPlayers()) {
                                region.getOwners().removePlayer(ownerName);
                            }
                            for (final String memberName : region.getMembers().getPlayers()) {
                                region.getMembers().removePlayer(memberName);
                            }
                            region.getOwners().addPlayer(player.getName());
                            region.getMembers().addPlayer(player.getName());
                            try {
                                if (!Plotshop.getInstance().getConfig().getString("greetMessage").isEmpty()) {
                                    region.setFlag(DefaultFlag.GREET_MESSAGE, DefaultFlag.GREET_MESSAGE.parseInput(Plotshop.worldGuard, Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', Plotshop.getInstance().getConfig().getString("greetMessage").replace("[playerName]", player.getName()))));
                                }
                                if (!Plotshop.getInstance().getConfig().getString("farewellMessage").isEmpty()) {
                                    region.setFlag(DefaultFlag.FAREWELL_MESSAGE, DefaultFlag.FAREWELL_MESSAGE.parseInput(Plotshop.worldGuard, Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', Plotshop.getInstance().getConfig().getString("farewellMessage").replace("[playerName]", player.getName()))));
                                }
                                regionManager.save();
                            } catch (final InvalidFlagFormat ex) {
                                ex.printStackTrace();
                            } catch (final ProtectionDatabaseException ex) {
                                ex.printStackTrace();
                            }
                            final Location location = sign.getLocation();
                            if (!Plotshop.unavailableSigns.contains(location)) {
                                sign.getBlock().breakNaturally();
                            } else {
                                Plotshop.availableSigns.add(location);
                                sign.setLine(0, ChatColor.RED + "Vendu");
                                sign.setLine(1, "");
                                sign.setLine(2, "");
                                sign.setLine(3, "");
                                sign.update();
                            }
                            Plotshop.economy.withdrawPlayer(player.getName(), plot.getPrice());
                            Plotshop.economy.depositPlayer(plot.getPlayerName(), plot.getPrice());
                            player.sendMessage(ChatColor.GREEN + "Vous venez d'acheter une parcelle en échange de " + String.valueOf(plot.getPrice()).replace(".", ",") + " " + (plot.getPrice() > 1 ? Plotshop.economy.currencyNamePlural() : Plotshop.economy.currencyNameSingular()));
                            plot.setPlayerName(player.getName());
                            plot.setX(-1);
                            plot.setY(-1);
                            plot.setZ(-1);
                            plot.setPrice(-1);
                            plot.setState(UNAVAILABLE);
                            plot.setPurchased(true);
                            Plotshop.getInstance().getDatabase().save(plot);
                        }
                        player.removeMetadata("plotshop.click." + sign.getWorld().getName() + "_" + sign.getX() + "_" + sign.getY() + "_" + sign.getZ(), Plotshop.getInstance());
                    }
                }
            }
        }
    }
}
