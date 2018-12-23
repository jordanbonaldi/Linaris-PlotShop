package net.neferett.plotshop;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;

@Entity
@Table(name = "plotshop")
public class Plot {
    public static enum State {
        AVAILABLE, UNAVAILABLE;
    }

    public static enum Type {
        REGION, REGION_CREATOR;
    }

    @Id
    private int id;

    @Length(max = 45)
    @NotEmpty
    private String playerName;

    @NotEmpty
    private String regionName;

    @NotEmpty
    private String worldName;

    @NotNull
    private int x;

    @NotNull
    private int y;

    @NotNull
    private int z;

    @NotNull
    private double price;

    @NotNull
    private State state;

    @NotNull
    private Type type;

    @NotNull
    private boolean purchased;

    public int getId() {
        return this.id;
    }

    public Location getLocation() {
        final World world = Bukkit.getServer().getWorld(this.worldName);
        return new Location(world, this.x, this.y, this.z);
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public double getPrice() {
        return this.price;
    }

    public String getRegionName() {
        return this.regionName;
    }

    public State getState() {
        return this.state;
    }

    public Type getType() {
        return this.type;
    }

    public String getWorldName() {
        return this.worldName;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public boolean isPurchased() {
        return this.purchased;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public void setLocation(final Location location) {
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
    }

    public void setPlayerName(final String playerName) {
        this.playerName = playerName;
    }

    public void setPrice(final double price) {
        this.price = price;
    }

    public void setPurchased(final boolean purchased) {
        this.purchased = purchased;
    }

    public void setRegionName(final String regionName) {
        this.regionName = regionName;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public void setWorldName(final String worldName) {
        this.worldName = worldName;
    }

    public void setX(final int x) {
        this.x = x;
    }

    public void setY(final int y) {
        this.y = y;
    }

    public void setZ(final int z) {
        this.z = z;
    }
}
