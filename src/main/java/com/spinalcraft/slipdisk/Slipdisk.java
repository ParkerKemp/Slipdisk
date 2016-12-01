package com.spinalcraft.slipdisk;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.spinalcraft.skull.SpinalcraftPlugin;
import com.spinalcraft.slipdisk.command.CommandExecutor;
import com.spinalcraft.slipdisk.database.DatabaseClient;
import com.spinalcraft.slipdisk.event.EventListener;

public class Slipdisk extends SpinalcraftPlugin {
	public ConsoleCommandSender console;
	
	private CommandExecutor commandExecutor;
	
	@Override
	public void onEnable() {
		super.onEnable();
		console = Bukkit.getConsoleSender();
		
		getServer().getPluginManager().registerEvents(new EventListener(this), this);
		
		DatabaseClient.createTables();
		saveDefaultConfig();
//		loadConfig();
		Slip.cleanUpDatabase(this);
		
		commandExecutor = new CommandExecutor();
	}
	

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		return this.commandExecutor.onCommand(sender, cmd, label, args);
	}
	
//	private void loadConfig(){
//		reloadConfig();
//		FileConfiguration config = getConfig();
//	}
//	
//	private void writeConfig(){
//		FileConfiguration config = getConfig();
//		saveConfig();
//	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll((JavaPlugin) this);
	}
}
