package com.spinalcraft.slipdisk;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import main.java.com.spinalcraft.spinalpack.Co;
import main.java.com.spinalcraft.spinalpack.Spinalpack;
import net.md_5.bungee.api.ChatColor;

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
import org.joda.time.DateTime;
import org.joda.time.Days;

public class Slipdisk extends JavaPlugin implements Listener {
	ConsoleCommandSender console;

	@Override
	public void onEnable() {
		console = Bukkit.getConsoleSender();
		console.sendMessage(Spinalpack.code(Co.BLUE) + "Slipdisk online!");
		getServer().getPluginManager().registerEvents((Listener) this, this);

		createTables();
	}
	
	private static void createTables(){
		String query;
		query = "CREATE TABLE IF NOT EXISTS slip_users (uid INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36) UNIQUE, username VARCHAR(32))";
		Spinalpack.update(query);
		query = "CREATE TABLE IF NOT EXISTS slip_roles (role VARCHAR(16) PRIMARY KEY, max INT, cdimmune BIT(1))";
		Spinalpack.update(query);
		query = "CREATE TABLE IF NOT EXISTS slip_slips (sid INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uid INT, date INT, w VARCHAR(32), "
				+ "sx FLOAT, sy FLOAT, sz FLOAT, x FLOAT, y FLOAT, z FLOAT, pitch FLOAT, yaw FLOAT, FOREIGN KEY (uid) REFERENCES slip_users(uid))";
		Spinalpack.update(query);
		query = "CREATE TABLE IF NOT EXISTS slip_info (uid INT PRIMARY KEY, cddayzero BIGINT, role VARCHAR(16), FOREIGN KEY (uid) REFERENCES slip_users(uid), FOREIGN KEY (role) REFERENCES slip_roles(role))";
		Spinalpack.update(query);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("slipdisk")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				player.sendMessage("");
				player.sendMessage(Spinalpack.code(Co.GOLD)
						+ "Slipdisk is a Spinalcraft-exclusive plugin that allows "
						+ "you to create a two-way \"slip\" to teleport between spawn and your base!");
				player.sendMessage("");
				player.sendMessage(Spinalpack.code(Co.GOLD)
						+ "Each player may have one slip at a time. "
						+ "To create a slip, you need to place a sign at each endpoint. On each sign, simply type "
						+ Spinalpack.code(Co.RED)
						+ "#slip "
						+ Spinalpack.code(Co.GOLD)
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
			if (deleteSlip(player)) {
				player.sendMessage(Spinalpack.code(Co.GOLD) + "Your slip was deleted by a moderator!");
				sender.sendMessage(Spinalpack.code(Co.GOLD)
						+ "Successfully deleted slip!");
			} else {
				sender.sendMessage(Spinalpack.code(Co.RED)
						+ "Unable to delete slip due to database error. (Let Parker know)");
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

	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(SignChangeEvent event) {
		if (!event.getPlayer().hasPermission("slipdisk.createslip")) {
			event.getPlayer().sendMessage(
					Spinalpack.code(Co.RED)
							+ "You're not allowed to create slips!");
			return;
		}

		if (!(event.getLine(0).equalsIgnoreCase("#slip") || event.getLine(1)
				.equalsIgnoreCase("#slip")))
			return;

		Player player = event.getPlayer();
		
		Profile profile = getProfile(player);
		if(profile == null){
			player.sendMessage(ChatColor.RED + "Database error!");
			return;
		}

		if (profile.slip == null) {
			event.setCancelled(true);
			return;
		}
		
		if(profile.slip.numEndpoints() >= profile.max){
			player.sendMessage(Spinalpack.code(Co.RED) + "Your slip already has " + profile.max + " endpoints. Break one first!");
			return;
		}

		event.setLine(0, Spinalpack.code(Co.DARKRED) + "Slip");

		event.setLine(1, profile.username);
		
		Block block;
		Sign tempSign;
		for(int i = 0; i < Slip.MAX_SLIPS; i++){
			if(profile.slip.signs[i] != null){
				block = profile.slip.signs[i].sign.getBlock();
				if (block.getState() instanceof Sign) {
					tempSign = (Sign)block.getState();
					if(slipSign(tempSign)){
						tempSign.setLine(1, profile.username);
						tempSign.update();
					}
				}
			}
		}

		try {
			insertEndpoint(profile, event.getBlock().getLocation(), player.getLocation());
		} catch (SQLException e) {
			e.printStackTrace();
		}

		player.sendMessage(Spinalpack.code(Co.GOLD)
				+ "Created a new slip gate!");
		console.sendMessage(Spinalpack.code(Co.GOLD) + player.getName()
				+ " created a slip gate!");
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK))
			return;

		if (!(event.getClickedBlock().getState() instanceof Sign))
			return;

		if (!event.getPlayer().hasPermission("slipdisk.useslip")) {
			event.getPlayer().sendMessage(
					Spinalpack.code(Co.RED)
							+ "You're not allowed to use slips!");
			return;
		}

		Sign sign = (Sign) event.getClickedBlock().getState();
		if (!slipSign(sign))
			return;
		Player player = event.getPlayer();

		Profile profile = getProfile(sign.getLine(1));
		if (profile.slip == null) {
			player.sendMessage(Spinalpack.code(Co.RED)
					+ "Critical database error!");
			return;
		}
		if (profile.slip.numEndpoints() == 0) {
			player.sendMessage(Spinalpack.code(Co.RED)
					+ "Error: Unable to find this endpoint in the database!");
			return;
		}
		if (profile.slip.numEndpoints() < 2) {
			player.sendMessage(Spinalpack.code(Co.RED)
					+ "Slip has no exit gate!");
			return;
		}

		long timeElapsed = System.currentTimeMillis() / 1000 - profile.slip.getMostRecentDate();

		if (timeElapsed < profile.cd && !profile.cdImmune) {
			long timeRemaining = profile.cd - timeElapsed;
			String timeString = "" + timeRemaining / 60 + ":"
					+ (timeRemaining % 60 < 10 ? "0" : "") + timeRemaining % 60;
			player.sendMessage(Spinalpack.code(Co.GOLD)
					+ "Cooldown remaining: " + Spinalpack.code(Co.RED)
					+ timeString);
			return;
		}

		Location destination = nextSlip(profile.slip, sign);
		if(destination == null)
			return;
		
		player.teleport(destination);
	}
	
	private void elevateUser(CommandSender sender, String username, String level){
		Player player = Bukkit.getPlayer(username);
		if(player == null){
			sender.sendMessage(ChatColor.RED + "Player couldn't be found!");
			return;
		}
		String query = "UPDATE slip_info i JOIN slip_users u ON i.uid = u.uid AND u.uuid = ? SET i.role = ?";
		try {
			PreparedStatement stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, player.getUniqueId().toString());
			stmt.setString(2, level);
			stmt.executeUpdate();
		} catch (SQLException e) {
			sender.sendMessage(ChatColor.RED + "Couldn't find that privilege level in the database!");
			e.printStackTrace();
			return;
		}
		sender.sendMessage(ChatColor.BLUE + username + ChatColor.GOLD + " was changed to " + ChatColor.GREEN + level + ChatColor.GOLD + "!");
	}
	
	private Location nextSlip(Slip slip, Sign sign){
		for(int i = 0; i < Slip.MAX_SLIPS; i++)
			if(slip.signs[i].sign.equals(sign.getLocation())){
				int j = i;
				do{
					j = (j + 1) % Slip.MAX_SLIPS;	
				}
				while(slip.signs[j] == null);
				return slip.signs[j].slip;
			}
		return null;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.SIGN_POST
				|| event.getBlock().getType() == Material.WALL_SIGN) {
			Sign sign = (Sign) event.getBlock().getState();
			if (slipSign(sign)) {
				if (unlinkSlipSign(sign))
					event.getPlayer().sendMessage(
							Spinalpack.code(Co.RED) + "Unlinked a slip gate!");
				else {
					event.getPlayer()
							.sendMessage(
									Spinalpack.code(Co.RED)
											+ "Unable to unlink due to database error. Go find Parker and tell him "
											+ "this shit isn't working!");
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.getBlock().getType() == Material.SIGN_POST
				|| event.getBlock().getType() == Material.WALL_SIGN) {
			Sign sign = (Sign) event.getBlock().getState();
			Block attachedTo = event.getBlock().getRelative(
					((org.bukkit.material.Sign) sign.getData())
							.getAttachedFace());
			if (attachedTo.getType() == Material.AIR) {
				if (slipSign(sign)) {
					if (!unlinkSlipSign(sign))
						event.setCancelled(true);
				}
			}
		}
	}
	
	private void createUser(String uuid, String username) throws SQLException{
		String query = "INSERT INTO slip_users (uuid, username) VALUES (?, ?)";
		PreparedStatement stmt = Spinalpack.prepareStatement(query);
		stmt.setString(1, uuid);
		stmt.setString(2, username);
		stmt.executeUpdate();
		
		query = "INSERT INTO slip_info (uid, cddayzero, role) SELECT uid, " + System.currentTimeMillis() + ", 'user' FROM slip_users WHERE uuid = ?";
		stmt = Spinalpack.prepareStatement(query);
		stmt.setString(1, uuid);
		stmt.executeUpdate();
	}
	
	private void insertEndpoint(Profile profile, Location sLocation, Location pLocation) throws SQLException {
		String query = "INSERT INTO slip_slips (uid, date, w, sx, sy, sz, x, y, z, pitch, yaw) SELECT uid, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? FROM slip_users WHERE uuid = ?";
		PreparedStatement stmt = Spinalpack.prepareStatement(query);
		stmt.setLong(1, System.currentTimeMillis() / 1000);
		stmt.setString(2, sLocation.getWorld().getName());
		stmt.setFloat(3, sLocation.getBlockX());
		stmt.setFloat(4, sLocation.getBlockY());
		stmt.setFloat(5, sLocation.getBlockZ());
		stmt.setDouble(6, pLocation.getX());
		stmt.setDouble(7, pLocation.getY());
		stmt.setDouble(8, pLocation.getZ());
		stmt.setDouble(9, pLocation.getPitch());
		stmt.setDouble(10, pLocation.getYaw());
		stmt.setString(11, profile.uuid);
		
		stmt.executeUpdate();
		
		if(pastGracePeriod(profile) && profile.slip.numEndpoints() > 0 && !profile.cdImmune){
			query = "UPDATE slip_info i JOIN slip_users u ON i.uid = u.uid AND u.uuid = ? SET cddayzero = ?";
		
			stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, profile.uuid);
			stmt.setLong(2, newCooldown(profile.uuid));
			stmt.executeUpdate();
		}
	}
	
	private boolean pastGracePeriod(Profile profile){
		long timeElapsed = System.currentTimeMillis() / 1000 - profile.slip.getMostRecentDate();
		return timeElapsed > 60;
	}
	
	private long newCooldown(String uuid) throws SQLException{
		String query = "SELECT cddayzero FROM slip_users u JOIN slip_info i ON u.uid = i.uid AND u.uuid = ?";
		PreparedStatement stmt = Spinalpack.prepareStatement(query);
		stmt.setString(1, uuid);
		ResultSet rs = stmt.executeQuery();
		rs.first();
		long oldCd = rs.getLong("cddayzero");
		
		//adjust cd as if dayzero were at most 6 days prior to current date
		int modifier = -6 - Math.min(daysUntil(oldCd), -6);
		DateTime dt = new DateTime(oldCd);
		dt = dt.plusDays(2 + modifier);
		return dt.getMillis();
	}

	private boolean deleteSlip(Player player) {
		Slip slip = slipFromUuid(player.getUniqueId().toString());
		Block block;
		if (!deleteSlipRecord(player.getUniqueId().toString()))
			return false;
		
		for(int i = 0; i < Slip.MAX_SLIPS; i++){
			if(slip.signs[i] != null){
				block = slip.signs[i].sign.getBlock();
				if (block.getState() instanceof Sign) {
					if (slipSign((Sign) block.getState())) {
						block.breakNaturally();
					}
				}
			}
		}
		return true;
	}

	private boolean deleteSlipRecord(String uuid) {
		String query = "DELETE s.* FROM slip_slips AS s JOIN slip_users AS u ON s.uid = u.uid AND u.uuid = ?";
		try {
			PreparedStatement stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, uuid);
			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean unlinkSignWithSid(int sid){
		String query = "DELETE FROM slip_slips WHERE sid = ?";
		try {
			PreparedStatement stmt = Spinalpack.prepareStatement(query);
			stmt.setInt(1, sid);
			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean unlinkSlipSign(Sign sign) {
		Slip slip = slipFromUsername(sign.getLine(1));
		
		for(int i = 0; i < Slip.MAX_SLIPS; i++){
			if(slip.signs[i] != null){
				if(slip.signs[i].sign.equals(sign.getLocation()))
					return unlinkSignWithSid(slip.signs[i].sid);
			}
		}
		return true;
	}
	
	private Profile getProfile(String username){
		//Used for an existing slip sign
		
		String query = "SELECT uuid FROM slip_users WHERE username = ?";
		PreparedStatement stmt;
		try {
			stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();
			rs.first();
			return getProfile(UUID.fromString(rs.getString("uuid")));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private Profile getProfile(Player player){
		//Should be used when creating slip signs. Creates new profile if none exists
		
		String query = "SELECT * FROM slip_users WHERE uuid = ?";
		String truncName = truncatedName(player.getName());
		String uuidString = player.getUniqueId().toString();
		try {
			PreparedStatement stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, uuidString);
			ResultSet rs = stmt.executeQuery();
			if(!rs.first())
				createUser(uuidString, truncName);
			else if(rs.getString("username") != truncName){
				query = "UPDATE slip_users SET username = ? WHERE uuid = ?";
				stmt = Spinalpack.prepareStatement(query);
				stmt.setString(1, truncName);
				stmt.setString(2, uuidString);
				stmt.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return getProfile(player.getUniqueId());
	}
	
	private Profile getProfile(UUID uuid){
		//Assumes profile exists
		
		String query = "SELECT u.uid, uuid, username, r.role, max, cddayzero, cdimmune FROM slip_users u, slip_info i, slip_roles r WHERE u.uid = i.uid AND i.role = r.role AND u.uuid = ?";
		try {
			PreparedStatement stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, uuid.toString());
			ResultSet rs = stmt.executeQuery();
			rs.first();
			Profile profile = new Profile();
			profile.uid = rs.getInt("u.uid");
			profile.uuid = rs.getString("uuid");
			profile.role = rs.getString("r.role");
			profile.username = rs.getString("username");
			profile.max = rs.getInt("max");
			profile.slip = slipFromUuid(profile.uuid);
			
			long cdDayZero = rs.getLong("cddayzero");
						
			profile.cd = daysUntil(cdDayZero) * 15;
			profile.cdImmune = rs.getInt("cdimmune") == 1;
			return profile;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private int daysUntil(long dayZero){
		Date past = new Date(dayZero);
		Date today = new Date();
		return Days.daysBetween(new DateTime(today), new DateTime(past)).getDays() - 1;
	}

	private Slip slipFromUsername(String username) {
		String query = "SELECT sid, date, w, sx, sy, sz, x, y, z, pitch, yaw FROM slip_slips s JOIN slip_users u ON s.uid = u.uid AND u.username = ?";
		PreparedStatement stmt;
		try {
			stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, username);
			return getSlip(stmt);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private Slip slipFromUuid(String uuid) {
		String query = "SELECT sid, date, w, sx, sy, sz, x, y, z, pitch, yaw FROM slip_slips s JOIN slip_users u ON s.uid = u.uid AND u.uuid = ?";
		PreparedStatement stmt;
		try {
			stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, uuid);
			return getSlip(stmt);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Slip getSlip(PreparedStatement stmt) throws SQLException {
		Slip ret = new Slip();
		float x, y, z, pitch, yaw;
		String world;

		ResultSet res = stmt.executeQuery();

		int i = 0;
		while (res.next()) {
			world = res.getString("w");
			if (world != null) {
				x = res.getFloat("sx");
				y = res.getFloat("sy");
				z = res.getFloat("sz");
				ret.signs[i] = new SlipSign();
				ret.signs[i].sign = new Location(Bukkit.getWorld(world), x, y, z);

				x = res.getFloat("x");
				y = res.getFloat("y");
				z = res.getFloat("z");
				pitch = res.getFloat("pitch");
				yaw = res.getFloat("yaw");
				ret.signs[i].slip = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
				
				ret.signs[i].date = res.getInt("date");
				ret.signs[i].sid = res.getInt("sid");
				i++;
			}
		}
		return ret;
	}

	private String truncatedName(String name) {
		String trunc = Spinalpack.code(Co.DARKBLUE) + name;
		trunc = trunc.substring(0, Math.min(trunc.length(), 15));
		return trunc;
	}

	private boolean slipSign(Sign sign) {
		return sign.getLine(0).equals(Spinalpack.code(Co.DARKRED) + "Slip");
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll((JavaPlugin) this);
	}
}