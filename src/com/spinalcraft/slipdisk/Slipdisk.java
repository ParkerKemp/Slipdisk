package com.spinalcraft.slipdisk;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.spinalcraft.spinalpack.Co;
import com.spinalcraft.spinalpack.Slip;
import com.spinalcraft.spinalpack.Spinalpack;

public class Slipdisk extends JavaPlugin implements Listener{
	ConsoleCommandSender console;
	
	@Override
	public void onEnable(){
		console = Bukkit.getConsoleSender();
		console.sendMessage(Spinalpack.code(Co.BLUE) + "Slipdisk online!");
		getServer().getPluginManager().registerEvents((Listener)this,  this);
		
		//Spinalpack.createSlipTable();
		createSlipTable();
	}
	
	private void createSlipTable(){
		String query = "CREATE TABLE IF NOT EXISTS Slips (uuid VARCHAR(36) PRIMARY KEY, username VARCHAR(31), "
				+ "timeCreated INT, cooldown INT, w1 VARCHAR(31), sx1 FLOAT, sy1 FLOAT, sz1 FLOAT, x1 FLOAT, "
				+ "y1 FLOAT, z1 FLOAT, pitch1 FLOAT, yaw1 FLOAT, w2 VARCHAR(31), sx2 FLOAT, sy2 FLOAT, sz2 FLOAT, "
				+ "x2 FLOAT, y2 FLOAT, z2 FLOAT, pitch2 FLOAT, yaw2 FLOAT)";
		Spinalpack.update(query);
	}
	
	private void insertSlip(String uuid, String username, Location sLocation, Location pLocation, int slipno){
		String query;
		PreparedStatement stmt;
		query = String.format("INSERT INTO Slips (uuid, username, timeCreated, cooldown, w%d, sx%d, "
				+ "sy%d, sz%d, x%d, y%d, z%d, pitch%d, yaw%d) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE timeCreated = ?, cooldown = cooldown + 30, w%d = ?, sx%d = ?, "
				+ "sy%d = ?, sz%d = ?, x%d = ?, y%d = ?, z%d = ?, pitch%d = ?, yaw%d = ?", 
				slipno, slipno, slipno, slipno, slipno, slipno, slipno, slipno, slipno, slipno, slipno, 
				slipno, slipno, slipno, slipno, slipno, slipno, slipno, slipno, slipno, slipno, slipno);
		try {
			stmt = Spinalpack.prepareStatement(query);
			
			stmt.setString(1, uuid);
			stmt.setString(2, username);
			stmt.setLong(3, System.currentTimeMillis() / 1000);
			stmt.setInt(4, -300);
			stmt.setString(5, sLocation.getWorld().getName());
			stmt.setFloat(6, sLocation.getBlockX());
			stmt.setFloat(7, sLocation.getBlockY());
			stmt.setFloat(8, sLocation.getBlockZ());
			stmt.setDouble(9, sLocation.getX());
			stmt.setDouble(10, sLocation.getY());
			stmt.setDouble(11, sLocation.getZ());
			stmt.setDouble(12, sLocation.getPitch());
			stmt.setDouble(13, sLocation.getYaw());
			stmt.setLong(14, System.currentTimeMillis() / 1000);
			stmt.setString(15, sLocation.getWorld().getName());
			stmt.setFloat(16, sLocation.getBlockX());
			stmt.setFloat(17, sLocation.getBlockY());
			stmt.setFloat(18, sLocation.getBlockZ());
			stmt.setDouble(19, sLocation.getX());
			stmt.setDouble(20, sLocation.getY());
			stmt.setDouble(21, sLocation.getZ());
			stmt.setDouble(22, sLocation.getPitch());
			stmt.setDouble(23, sLocation.getYaw());
			
			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	/*
public static void insertSlipNode(String uuid, String username, Location sLocation, Location pLocation, int slipno){
		String query;
		if(slipno == 1)
			query = "INSERT INTO Slips (uuid, username, timeCreated, cooldown, w1, sx1, sy1, sz1, x1, y1, z1, pitch1, yaw1) VALUES ('"
				+ uuid
				+ "', '"
				+ username
				+ "', '"
				+ System.currentTimeMillis() / 1000
				+ "', '"
				+ -300
				+ "', '"
				+ sLocation.getWorld().getName()
				+ "', '"
				+ sLocation.getBlockX()
				+ "', '"
				+ sLocation.getBlockY()
				+ "', '"
				+ sLocation.getBlockZ()
				+ "', '"
				+ pLocation.getX()
				+ "', '"
				+ pLocation.getY()
				+ "', '"
				+ pLocation.getZ()
				+ "', '"
				+ pLocation.getPitch()
				+ "', '"
				+ pLocation.getYaw()
				+ "') ON DUPLICATE KEY UPDATE "
				+ "timeCreated = '"
				+ System.currentTimeMillis() / 1000
				+ "', cooldown = (cooldown + 30), w1 = '"
				+ sLocation.getWorld().getName()
				+ "', sx1 = '"
				+ sLocation.getBlockX()
				+ "', sy1 = '"
				+ sLocation.getBlockY()
				+ "', sz1 = '"
				+ sLocation.getBlockZ()
				+ "', x1 = '"
				+ pLocation.getX()
				+ "', y1 = '"
				+ pLocation.getY()
				+ "', z1 = '"
				+ pLocation.getZ()
				+ "', pitch1 = '"
				+ pLocation.getPitch()
				+ "', yaw1 = '"
				+ pLocation.getYaw()
				+ "'";
		else
			query = "INSERT INTO Slips (uuid, username, timeCreated, cooldown, w2, sx2, sy2, sz2, x2, y2, z2, pitch2, yaw2) VALUES ('"
				+ uuid
				+ "', '"
				+ username
				+ "', '"
				+ System.currentTimeMillis() / 1000
				+ "', '"
				+ -300
				+ "', '"
				+ sLocation.getWorld().getName()
				+ "', '"
				+ sLocation.getBlockX()
				+ "', '"
				+ sLocation.getBlockY()
				+ "', '"
				+ sLocation.getBlockZ()
				+ "', '"
				+ pLocation.getX()
				+ "', '"
				+ pLocation.getY()
				+ "', '"
				+ pLocation.getZ()
				+ "', '"
				+ pLocation.getPitch()
				+ "', '"
				+ pLocation.getYaw()
				+ "') ON DUPLICATE KEY UPDATE "
				+ "timeCreated = '"
				+ System.currentTimeMillis() / 1000
				+ "', cooldown = (cooldown + 30), w2 = '"
				+ sLocation.getWorld().getName()
				+ "', sx2 = '"
				+ sLocation.getBlockX()
				+ "', sy2 = '"
				+ sLocation.getBlockY()
				+ "', sz2 = '"
				+ sLocation.getBlockZ()
				+ "', x2 = '"
				+ pLocation.getX()
				+ "', y2 = '"
				+ pLocation.getY()
				+ "', z2 = '"
				+ pLocation.getZ()
				+ "', pitch2 = '"
				+ pLocation.getPitch()
				+ "', yaw2 = '"
				+ pLocation.getYaw()
				+ "'";
		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	 */
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("slipdisk")){
			if(sender instanceof Player){
				Player player = (Player)sender;
				player.sendMessage("");
				player.sendMessage(Spinalpack.code(Co.GOLD) + "Slipdisk is a Spinalcraft-exclusive plugin that allows "
						+ "you to create a two-way \"slip\" to teleport between spawn and your base!");
				player.sendMessage("");
				player.sendMessage(Spinalpack.code(Co.GOLD) + "Each player may have one slip at a time. "
						+ "To create a slip, you need to place a sign at each endpoint. On each sign, simply type "
						+ Spinalpack.code(Co.RED) + "#slip " + Spinalpack.code(Co.GOLD)
						+ "in the top row, and it will automatically register it in your name. "
						+ "Now you and others can use your slip to instantly teleport back and forth!");
				player.sendMessage("");
				return true;
			}
		}
		if(cmd.getName().equalsIgnoreCase("deleteslip")){
			Player player = (Player)sender;
			if(deleteSlip(player)){
				player.sendMessage(Spinalpack.code(Co.GOLD) + "Successfully deleted slip!");
			}
			else{
				player.sendMessage(Spinalpack.code(Co.RED) + "Unable to delete slip due to database error. (Let Parker know)");
			}
			return true;
		}
		return false;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(SignChangeEvent event){
		int slipno;
		
		if(!event.getPlayer().hasPermission("slipdisk.createslip")){
			event.getPlayer().sendMessage(Spinalpack.code(Co.RED) + "You're not allowed to create slips!");
			return;
		}
		
		if(!(event.getLine(0).equalsIgnoreCase("#slip") || event.getLine(1).equalsIgnoreCase("#slip")))
			return;
		
		Player player = event.getPlayer();
		
		//Update: check with UUID instead
		//Slip slip = Spinalpack.slip(truncatedName(player.getName()));
		String uuid = player.getUniqueId().toString();
		Slip slip = Spinalpack.slipFromUuid(uuid);
		
		if(slip == null){
			event.setCancelled(true);
			return;
		}
		if(!slip.sign1Valid())
			slipno = 1;
		else if(!slip.sign2Valid())
			slipno = 2;
		else{
			player.sendMessage(Spinalpack.code(Co.RED) + "Your slip already has two endpoints. Break one first!");
			return;
		}
		
		event.setLine(0, Spinalpack.code(Co.DARKRED) + "Slip");
		
		String trunc = truncatedName(player.getName());
		
		//Update: check DB for existing trunc name on a separate record. If so, append "#1" to this one, and "#0" to the original slip
		//(both signs and DB record)
		event.setLine(1, trunc);
		
		insertSlip(uuid, trunc, event.getBlock().getLocation(), player.getLocation(), slipno);
		
		player.sendMessage(Spinalpack.code(Co.GOLD) + "Created a new slip gate!");
		console.sendMessage(Spinalpack.code(Co.GOLD) + player.getName() + " created a slip gate!");
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event){
		if(!(event.getAction() == Action.RIGHT_CLICK_BLOCK))
			return;
		
		if(!(event.getClickedBlock().getState() instanceof Sign))
			return;
		
		if(!event.getPlayer().hasPermission("slipdisk.useslip")){
			event.getPlayer().sendMessage(Spinalpack.code(Co.RED) + "You're not allowed to use slips!");
			return;
		}

		Sign sign = (Sign)event.getClickedBlock().getState();
		if(!slipSign(sign))
			return;
		Player player = event.getPlayer();
		
		Slip slip = Spinalpack.slipFromUsername(sign.getLine(1));
		if(slip == null){
			player.sendMessage(Spinalpack.code(Co.RED) + "Critical database error!");
			return;
		}
		if(slip.noSlip()){
			player.sendMessage(Spinalpack.code(Co.RED) + "Error: Unable to find this endpoint in the database!");
			return;
		}
		
		if(!slip.wholeSlip()){
			player.sendMessage(Spinalpack.code(Co.RED) + "Slip has no exit gate!");
			return;
		}
		
		long timeElapsed = System.currentTimeMillis() / 1000 - slip.timeCreated;
		
		if(timeElapsed < slip.cooldown && !player.hasPermission("slipdisk.nocooldown")){
			long timeRemaining = slip.cooldown - timeElapsed;
			String timeString = "" + timeRemaining / 60 + ":" + (timeRemaining % 60 < 10 ? "0" : "") + timeRemaining % 60;
			player.sendMessage(Spinalpack.code(Co.GOLD) + "Cooldown remaining: " + Spinalpack.code(Co.RED) + timeString);
			return;
		}
		
		player.sendMessage(Spinalpack.code(Co.LIGHTPURPLE) + "Slipped!");
		
		if(slip.sign1.equals(sign.getLocation()))
			event.getPlayer().teleport(slip.slip2);
		else
			event.getPlayer().teleport(slip.slip1);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreak(BlockBreakEvent event){
		if(event.getBlock().getType() == Material.SIGN_POST || event.getBlock().getType() == Material.WALL_SIGN){
			Sign sign = (Sign)event.getBlock().getState();
			if(slipSign(sign)){
				if(unlinkSlipSign(sign))
					event.getPlayer().sendMessage(Spinalpack.code(Co.RED) + "Unlinked a slip gate!");
				else{
					event.getPlayer().sendMessage(Spinalpack.code(Co.RED) + "Unable to unlink due to database error. Go find Parker and tell him "
							+ "this shit isn't working!");
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPhysics(BlockPhysicsEvent event){
		if(event.getBlock().getType() == Material.SIGN_POST || event.getBlock().getType() == Material.WALL_SIGN){
			Sign sign = (Sign)event.getBlock().getState();
			Block attachedTo = event.getBlock().getRelative(((org.bukkit.material.Sign)sign.getData()).getAttachedFace());
            if(attachedTo.getType() == Material.AIR){
            	if(slipSign(sign)){
            		if(!unlinkSlipSign(sign))
            			event.setCancelled(true);
            	}
            }
		}
	}
	
	private String truncatedName(String name){
		String trunc = Spinalpack.code(Co.DARKBLUE) + name;
		trunc = trunc.substring(0, Math.min(trunc.length(), 15));
		return trunc;
	}
	
	private boolean deleteSlip(Player player){
		Slip slip = Spinalpack.slipFromUuid(player.getUniqueId().toString());
		Block block;
		if(!Spinalpack.deleteSlip(truncatedName(player.getName())))
			return false;
		if(slip.sign1Valid()){
			block = slip.sign1.getBlock();
			if(block.getState() instanceof Sign){
				if(slipSign((Sign)block.getState())){
					block.breakNaturally();
				}
			}
		}
		
		if(slip.sign2Valid()){
			block = slip.sign2.getBlock();
			if(block.getState() instanceof Sign)
				if(slipSign((Sign)block.getState()))
					block.breakNaturally();
		}
		return true;
	}
	
	private boolean unlinkSlipSign(Sign sign){
		Slip slip = Spinalpack.slipFromUsername(sign.getLine(1));
		if(slip.sign1Valid())
			if(slip.sign1.equals(sign.getLocation()))
				return Spinalpack.unlinkSign(sign.getLine(1), 1);
		if(slip.sign2Valid())
			if(slip.sign2.equals(sign.getLocation()))
				return Spinalpack.unlinkSign(sign.getLine(1), 2);
		return true;
	}
	
	private boolean slipSign(Sign sign){
		return sign.getLine(0).equals(Spinalpack.code(Co.DARKRED) + "Slip");
	}
	
	@Override
	public void onDisable(){
		HandlerList.unregisterAll((JavaPlugin)this);
	}
}
