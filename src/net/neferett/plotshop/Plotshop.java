package net.neferett.plotshop;

import static net.neferett.plotshop.Plot.State.UNAVAILABLE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

import net.milkbowl.vault.economy.Economy;
import net.neferett.plotshop.commands.PSCommandExecutor;
import net.neferett.plotshop.commands.SellCommand;
import net.neferett.plotshop.listeners.BlockListener;
import net.neferett.plotshop.listeners.PlayerListener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class Plotshop extends JavaPlugin {
    private static Plotshop instance;
    public static Economy economy = null;
    public static WorldEditPlugin worldEdit = null;
    public static WorldGuardPlugin worldGuard = null;
    public static List<Location> availableSigns = new ArrayList<>();
    public static List<Location> unavailableSigns = new ArrayList<>();

    public static Plotshop getInstance() {
        return Plotshop.instance;
    }

    public static boolean hasPermission(final Player player, final String perm) {
        if (player.hasPermission("plotshop.admin")) {
            return true;
        } else if (player.hasPermission(perm)) { return true; }
        return false;
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        final List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(Plot.class);
        return list;
    }

    private String toString(final Location location) {
        return location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
    }

    private Location toLocation(final String location) {
        final String[] parts = location.split("_");
        return new Location(Bukkit.getWorld(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    @Override
    public void onDisable() {
        final List<String> availableSigns = new ArrayList<>();
        final List<String> unavailableSigns = new ArrayList<>();
        for (final Location location : Plotshop.availableSigns) {
            availableSigns.add(this.toString(location));
        }
        for (final Location location : Plotshop.unavailableSigns) {
            availableSigns.add(this.toString(location));
        }
        this.getConfig().set("availableSigns", availableSigns);
        this.getConfig().set("unavailableSigns", unavailableSigns);
        this.saveConfig();
    }

    @Override
    public void onEnable() {
        Plotshop.instance = this;
        this.getCommand("plotshop").setExecutor(new PSCommandExecutor());
        this.getCommand("vente").setExecutor(new SellCommand());
        this.setupDatabase();
        this.setupEconomy();
        this.setupWorldEdit();
        this.setupWorldGuard();
        if (!new File(this.getDataFolder(), "config.yml").exists()) {
            this.saveDefaultConfig();
        }
        for (final Plot plot : this.getDatabase().find(Plot.class).findList()) {
            if (!(plot.getLocation().getBlock().getState() instanceof Sign)) {
                if (!plot.isPurchased()) {
                    this.getDatabase().delete(plot);
                } else {
                    plot.setX(-1);
                    plot.setY(-1);
                    plot.setZ(-1);
                    plot.setPrice(-1);
                    plot.setState(UNAVAILABLE);
                    Plotshop.getInstance().getDatabase().save(plot);
                }
            }
        }
        for (final String strLocation : this.getConfig().getStringList("availableSigns")) {
            final Location location = this.toLocation(strLocation);
            if (location.getBlock().getState() instanceof Sign) {
                Plotshop.availableSigns.add(location);
            }
        }
        for (final String strLocation : this.getConfig().getStringList("unavailableSigns")) {
            final Location location = this.toLocation(strLocation);
            if (location.getBlock().getState() instanceof Sign) {
                Plotshop.unavailableSigns.add(location);
            }
        }
        this.getServer().getPluginManager().registerEvents(new BlockListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
    }

    private void setupDatabase() {
        try {
            this.getDatabase().find(Plot.class).findRowCount();
        } catch (final PersistenceException ex) {
            this.getLogger().info("Installing database for " + this.getDescription().getName() + " due to first time usage");
            this.installDDL();
        }
    }

    private boolean setupEconomy() {
        final RegisteredServiceProvider<Economy> economyProvider = this.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            Plotshop.economy = economyProvider.getProvider();
        }

        return Plotshop.economy != null;
    }

    private boolean setupWorldEdit() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("WorldEdit");
        if (plugin != null && plugin instanceof WorldEditPlugin) {
            Plotshop.worldEdit = (WorldEditPlugin) plugin;
        }

        return Plotshop.worldEdit != null;
    }

    private boolean setupWorldGuard() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin != null && plugin instanceof WorldGuardPlugin) {
            Plotshop.worldGuard = (WorldGuardPlugin) plugin;
        }

        return Plotshop.worldGuard != null;
    }
}
