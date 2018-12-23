package net.neferett.plotshop;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion.CircularInheritanceException;

public class RegionCreator {
    private Location ax(final Location block) {
        int i = 0;
        Location location = null;
        while (i < 100 && location == null) {
            if (block.add(1, 0, 0).getBlock().getType() == Material.FENCE) {
                if (block.add(1, 0, 0).getBlock().getType() == Material.FENCE) {
                    location = block;
                }
            }
            ++i;
        }
        return location;
    }

    private Location az(final Location block) {
        int i = 0;
        Location location = null;
        while (i < 100 && location == null) {
            if (block.add(0, 0, 1).getBlock().getType() == Material.FENCE) {
                if (block.add(0, 0, 1).getBlock().getType() == Material.FENCE) {
                    location = block;
                }
            }
            ++i;
        }
        return location;
    }

    public ProtectedRegion createRegion(final Sign sign, final String regionName, final Player player, final int up, final int bottom) {
        Location pos1 = null;
        Location pos2 = null;
        final Location location = sign.getLocation();
        switch (sign.getData().getData()) {
        case 0:
            pos1 = this.ax(location.clone());
            if (pos1 != null) {
                pos2 = this.sx(location.clone());
                if (pos2 != null) {
                    pos2 = this.sz(pos2);
                }
            }
            break;
        case 4:
            pos1 = this.az(location.clone());
            if (pos1 != null) {
                pos2 = this.sz(location.clone());
                if (pos2 != null) {
                    pos2 = this.ax(pos2);
                }
            }
            break;
        case 8:
            pos1 = this.sx(location.clone());
            if (pos1 != null) {
                pos2 = this.ax(location.clone());
                if (pos2 != null) {
                    pos2 = this.az(pos2);
                }
            }
            break;
        case 12:
            pos1 = this.sz(location.clone());
            if (pos1 != null) {
                pos2 = this.az(location.clone());
                if (pos2 != null) {
                    pos2 = this.sx(pos2);
                }
            }
            break;
        }
        if (pos1 == null || pos2 == null) {
            player.sendMessage(ChatColor.RED + "Impossible de trouver les barrières, vérifiez qu'elles suivent bien ce schéma :");
            player.sendMessage(ChatColor.RED + "┌            �?");
            player.sendMessage("");
            player.sendMessage("");
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "└            ┘");
            return null;
        }
        final RegionManager regionManager = Plotshop.worldGuard.getRegionManager(player.getWorld());
        final ApplicableRegionSet applicableRegion = regionManager.getApplicableRegions(location);
        ProtectedRegion parent = null;
        int found = 0;

        for (final ProtectedRegion rg : applicableRegion) {
            if (rg.getParent() == null) {
                found++;
                parent = rg;
            }
        }

        if (found == 1) {
            player.sendMessage(ChatColor.GREEN + "Région WorldGuard parente \"" + parent.getId() + "\" trouvée");
        } else {
            player.sendMessage(ChatColor.RED + "Aucune ou plusieurs régions WorldGuard parentes trouvées");
        }

        final ProtectedRegion region = new ProtectedCuboidRegion(regionName, new BlockVector(pos1.getBlockX(), pos1.getBlockY() + up, pos1.getBlockZ()), new BlockVector(pos2.getBlockX(), pos2.getBlockY() - bottom, pos2.getBlockZ()));
        try {
            region.setParent(parent);
            regionManager.addRegion(region);
            regionManager.save();
        } catch (final CircularInheritanceException ex) {
            ex.printStackTrace();
            player.sendMessage(ChatColor.RED + "Impossible de définir le parent de la région");
        } catch (final ProtectionDatabaseException ex) {
            ex.printStackTrace();
            player.sendMessage(ChatColor.RED + "Impossible de sauvegarder la région");
            return null;
        }
        player.sendMessage(ChatColor.GREEN + "Région WorldGuard \"" + regionName + "\" crée");
        player.sendMessage(ChatColor.GRAY + "Il se peut que la région parente soit mal détectée ou sauvegardée, dans ce cas tapez la commande /rg setparent " + regionName + " <parent>");
        return region;
    }

    private Location sx(final Location block) {
        int i = 0;
        Location location = null;
        while (i < 100 && location == null) {
            if (block.subtract(1, 0, 0).getBlock().getType() == Material.FENCE) {
                if (block.subtract(1, 0, 0).getBlock().getType() == Material.FENCE) {
                    location = block;
                }
            }
            ++i;
        }
        return location;
    }

    private Location sz(final Location block) {
        int i = 0;
        Location location = null;
        while (i < 100 && location == null) {
            if (block.subtract(0, 0, 1).getBlock().getType() == Material.FENCE) {
                if (block.subtract(0, 0, 1).getBlock().getType() == Material.FENCE) {
                    location = block;
                }
            }
            ++i;
        }
        return location;
    }
}
