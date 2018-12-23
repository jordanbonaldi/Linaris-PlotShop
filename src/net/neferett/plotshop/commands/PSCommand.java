package net.neferett.plotshop.commands;

import org.bukkit.entity.Player;

public abstract interface PSCommand {
    public abstract String getPermission();

    public abstract String help(final Player player);

    public abstract boolean onCommand(final Player player, final String[] params);
}
