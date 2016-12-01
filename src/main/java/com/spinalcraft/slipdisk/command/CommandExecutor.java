package com.spinalcraft.slipdisk.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.spinalcraft.slipdisk.Slip;
import com.spinalcraft.slipdisk.database.DatabaseClient;
import com.spinalcraft.usernamehistory.UUIDFetcher;

import net.md_5.bungee.api.ChatColor;

public class CommandExecutor{
	
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("slipdisk")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				player.sendMessage("");
				player.sendMessage(ChatColor.GOLD
						+ "Slipdisk is a Spinalcraft-exclusive plugin that allows "
						+ "you to create a two-way \"slip\" to teleport between spawn and your base!");
				player.sendMessage("");
				player.sendMessage(ChatColor.GOLD
						+ "Each player may have one slip at a time. "
						+ "To create a slip, you need to place a sign at each endpoint. On each sign, simply type "
						+ ChatColor.RED
						+ "#slip "
						+ ChatColor.GOLD
						+ "in the top row, and it will automatically register it in your name. "
						+ "Now you and others can use your slip to instantly teleport back and forth!");
				player.sendMessage("");
				return true;
			}
		}
		
		if (cmd.getName().equalsIgnoreCase("deleteslip")) {
			if(args.length == 0)
				return false;
			
			Player player = Bukkit.getPlayer(args[0]);
			
			if(player == null){
				sender.sendMessage("Player couldn't be found!");
				return true;
			}
			
			Slip slip = Slip.fromPlayer(player);
			
			if(slip.deleteGates()){
				player.sendMessage(ChatColor.GOLD + "Your slip was deleted by a moderator!");
				sender.sendMessage(ChatColor.GOLD + "Successfully deleted slip!");
			} else {
				sender.sendMessage(ChatColor.RED + "One or more gates failed to be deleted due to a database error.");
			}
			
			return true;
		}
		if(cmd.getName().equalsIgnoreCase("elevate")){
			if(args.length < 2)
				return false;
			elevateUser(sender, args[0], args[1]);
			return true;
		}

		return false;
	}

	private void elevateUser(final CommandSender sender, final String username, final String level){
		new Thread(){
			public void run(){
				
				UUID uuid = null;
				
				try {
					uuid = UUIDFetcher.getUUIDOf(username);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				if(uuid == null){
					sender.sendMessage(ChatColor.RED + "Slipdisk: Unable to elevate privileges: Player couldn't be found!");
					return;
				}

				Slip slip = Slip.fromUuid(uuid);
				
				if(!slip.hasValidProfile()){
					slip.createProfile(uuid, username);
				}
				
				try {
					DatabaseClient.updateProfileWithRole(slip.uid, level);
				} catch (SQLException e) {
					sender.sendMessage(ChatColor.RED + "Slipdisk: Unable to elevate privileges: Couldn't find that privilege level in the database!");
					e.printStackTrace();
					return;
				}
				
				sender.sendMessage(ChatColor.BLUE + username + ChatColor.GOLD + " was changed to " + ChatColor.GREEN + level + ChatColor.GOLD + "!");
			}
		}.run();
	}
}
