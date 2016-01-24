/*
 * Decompiled with CFR 0_110.
 *
 * Could not load the following classes:
 *  com.nijiko.permissions.PermissionHandler
 *  com.nijikokun.bukkit.Permissions.Permissions
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Server
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginDescriptionFile
 *  org.bukkit.plugin.PluginManager
 *  org.bukkit.plugin.java.JavaPlugin
 *  ru.tehkode.permissions.PermissionManager
 *  ru.tehkode.permissions.bukkit.PermissionsEx
 */
package com.martinambrus.bukkitopfix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class BukkitOpFix extends JavaPlugin implements Listener {

	private Integer opLevel = 0;
	private List<String> disabledOpCommands = new ArrayList<String>();

	@Override
	public void onEnable() {
		// save default config if not saved yet
		getConfig().options().copyDefaults(true);
		saveConfig();

		// get current OP permission level setting
		File f = new File("server.properties");
		opLevel = Integer.parseInt(getString("op-permission-level", f));
		disabledOpCommands = this.getConfig().getStringList("level" + opLevel + "disabledCommands");

		this.getServer().getPluginManager().registerEvents(this, this);

		try {
        	Metrics metrics = new Metrics(this);
            metrics.start();
		} catch (IOException e) {
			Bukkit.getLogger().warning("Failed to initialize Metrics.");
		}
    }

    public static String getString(String s, File f)
    {
        Properties pr = new Properties();
        try
        {
            FileInputStream in = new FileInputStream(f);
            pr.load(in);
            String string = pr.getProperty(s);
            return string;
        }
        catch (IOException e)
        {

        }
        return "";
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
    	Player player = event.getPlayer();

    	if (player.isOp()) {
    		// check the command against currently disabled vanilla OP commands
    		String cmd = event.getMessage().replace("/", "");

    		if (cmd.contains(" ")) {
    			cmd = cmd.split(" ")[0];
    		}

    		if (disabledOpCommands.contains(cmd.toLowerCase())) {
    			player.sendMessage(ChatColor.RED + this.getConfig().getString("disallowMessage"));
    			event.setCancelled(true);
    		}
    	}
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock().getType().equals(Material.COMMAND)) {
        	if (opLevel < 2) {
        		event.setCancelled(true);
        		event.getPlayer().sendMessage(ChatColor.RED + this.getConfig().getString("disallowMessage"));
        		event.getPlayer().closeInventory();
        	}
        }
    }
}

