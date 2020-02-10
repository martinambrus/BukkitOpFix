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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;

import net.md_5.bungee.api.ChatColor;

public class BukkitOpFix extends JavaPlugin implements Listener {

	private Integer opLevel = 0;
	private Map<String, Integer> perPlayerLevel = new HashMap<String, Integer>();

	@Override
	public void onEnable() {
		// save default config if not saved yet
		getConfig().options().copyDefaults(true);
		saveConfig();

		// get current global OP permission level setting
		File f = new File("server.properties");
		opLevel = Integer.parseInt(getString("op-permission-level", f));

		// get per-player OP levels
		reloadOpsCache();

		this.getServer().getPluginManager().registerEvents(this, this);
    }

	@SuppressWarnings("unchecked")
	private void reloadOpsCache() {
		String opsFileContent;
		perPlayerLevel = new HashMap<String, Integer>();
		try {
			opsFileContent = readFile("ops.json", StandardCharsets.UTF_8);
			List<Object> ops = new Gson().fromJson(opsFileContent, ArrayList.class);

			for (Object pair : ops) {
				Map<String, Object> values = (Map<String, Object>) pair;

				perPlayerLevel.put((String) values.get("uuid"), ((Double) values.get("level")).intValue());
			}
		} catch (IOException e1) {
			Bukkit.getLogger().warning("[BukkitOpFix] Failed to load data from ops.json file.");
			e1.printStackTrace();
		}
	}

	private String readFile(String path, Charset encoding) throws IOException
	{
	  byte[] encoded = Files.readAllBytes(Paths.get(path));
	  return new String(encoded, encoding);
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

    	// check if we're running OP or DEOP operation and update the ops cache in such case
    	if (event.getMessage().startsWith("/op") || event.getMessage().startsWith("/deop")) {
    		reloadOpsCache();
    	}

    	if (player.isOp()) {
    		// check the command against currently disabled vanilla OP commands
    		String cmd = event.getMessage().replace("/", "");

    		if (cmd.contains(" ")) {
    			cmd = cmd.split(" ")[0];
    		}

    		List<String> disabledOpCommands = new ArrayList<String>();
    		Integer localOpLevel = (perPlayerLevel.containsKey(player.getUniqueId().toString()) ? perPlayerLevel.get(player.getUniqueId().toString()) : opLevel);
    		disabledOpCommands = this.getConfig().getStringList("level" + localOpLevel + "disabledCommands");

    		if (disabledOpCommands.contains(cmd.toLowerCase())) {
    			player.sendMessage(ChatColor.RED + this.getConfig().getString("disallowMessage"));
    			event.setCancelled(true);
    		}
    	}
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	public void redirectConsoleCommand(ServerCommandEvent event) {
    	// check if we're running OP or DEOP operation and update the ops cache in such case
    	if (event.getCommand().startsWith("op") || event.getCommand().startsWith("deop")) {
    		reloadOpsCache();
    	}
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
    	// check this player's OP level
    	Integer localOpLevel = (perPlayerLevel.containsKey(event.getPlayer().getUniqueId().toString()) ? perPlayerLevel.get(event.getPlayer().getUniqueId().toString()) : opLevel);
    	if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock().getType().equals(Material.COMMAND)) {
        	if (localOpLevel < 2) {
        		event.setCancelled(true);
        		event.getPlayer().sendMessage(ChatColor.RED + this.getConfig().getString("disallowMessage"));
        		event.getPlayer().closeInventory();
        	}
        }
    }
}

