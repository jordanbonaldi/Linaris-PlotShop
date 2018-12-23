package net.neferett.plotshop.commands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.neferett.plotshop.Plotshop;

public class ReloadCommand implements PSCommand {
    @Override
    public String getPermission() {
        return "plotshop.reload";
    }

    @Override
    public String help(final Player player) {
        if (Plotshop.hasPermission(player, this.getPermission())) { return "/plotshop reload - Recharger la configuration du plugin"; }
        return "";
    }

    @Override
    public boolean onCommand(final Player player, final String[] params) {
        Plotshop.getInstance().reloadConfig();
        player.sendMessage(ChatColor.GREEN + "Configuration rechargée avec succès");
        return true;
    }
}
