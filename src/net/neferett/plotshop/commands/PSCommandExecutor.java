package net.neferett.plotshop.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.neferett.plotshop.Plotshop;

public class PSCommandExecutor implements CommandExecutor {
    private final HashMap<String, PSCommand> commands;

    public PSCommandExecutor() {
        this.commands = new HashMap<String, PSCommand>();
        this.loadCommands();
    }

    public void addCommand(final String cmd, final PSCommand command) {
        this.commands.put(cmd, command);
    }

    public void help(final Player player) {
        player.sendMessage("/plotshop <command> [args]");
        for (final PSCommand cmd : this.commands.values()) {
            player.sendMessage(ChatColor.GRAY + "- " + cmd.help(player));
        }
    }

    private void loadCommands() {
        this.addCommand("reload", new ReloadCommand());
        this.addCommand("signs", new SignsCommand());
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String commandLabel, String[] args) {
        if (!(sender instanceof Player)) { return false; }
        final Player player = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("plotshop")) {
            if (args == null || args.length < 1) {
                return true;
            }
            if (args[0].equalsIgnoreCase("help")) {
                this.help(player);
                return true;
            }
            final String sub = args[0];
            final Vector<String> l = new Vector<String>();
            l.addAll(Arrays.asList(args));
            l.remove(0);
            args = l.toArray(new String[0]);
            if (!this.commands.containsKey(sub)) {
                player.sendMessage(ChatColor.RED + "Commande inexistante");
                player.sendMessage(ChatColor.GOLD + "Tape /plotshop help pour de l'aide");
                return true;
            }
            try {
                if (Plotshop.hasPermission(player, this.commands.get(sub).getPermission())) {
                    this.commands.get(sub).onCommand(player, args);
                } else {
                    player.sendMessage(ChatColor.RED + "Vous n'avez pas la permission");
                }
            } catch (final Exception e) {
                e.printStackTrace();
                player.sendMessage(ChatColor.RED + "Une erreur s'est produite lors de l'exécution de la commande. Vérifiez la console");
                player.sendMessage(ChatColor.BLUE + "Tape /plotshop help pour de l'aide");
            }
            return true;
        }
        return false;
    }
}
