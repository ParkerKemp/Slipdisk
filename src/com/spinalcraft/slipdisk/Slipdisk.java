package com.spinalcraft.slipdisk;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
import com.spinalcraft.spinalpack.Spinalpack;

public class Slipdisk extends JavaPlugin implements Listener {
	ConsoleCommandSender console;

	@Override
	public void onEnable() {
		console = Bukkit.getConsoleSender();
		console.sendMessage(Spinalpack.code(Co.BLUE) + "Slipdisk online!");
		getServer().getPluginManager().registerEvents((Listener) this, this);

		createSlipTable();
	}

	private void createSlipTable() {
		String query = "CREATE TABLE IF NOT EXISTS Slips (uuid VARCHAR(36) PRIMARY KEY, username VARCHAR(31), "
				+ "timeCreated INT, cooldown INT, w1 VARCHAR(31), sx1 FLOAT, sy1 FLOAT, sz1 FLOAT, x1 FLOAT, "
				+ "y1 FLOAT, z1 FLOAT, pitch1 FLOAT, yaw1 FLOAT, w2 VARCHAR(31), sx2 FLOAT, sy2 FLOAT, sz2 FLOAT, "
				+ "x2 FLOAT, y2 FLOAT, z2 FLOAT, pitch2 FLOAT, yaw2 FLOAT)";
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
			Player player = (Player) sender;
			if (deleteSlip(player)) {
				player.sendMessage(Spinalpack.code(Co.GOLD)
						+ "Successfully deleted slip!");
			} else {
				player.sendMessage(Spinalpack.code(Co.RED)
						+ "Unable to delete slip due to database error. (Let Parker know)");
			}
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(SignChangeEvent event) {
		int slipno;

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

		String uuid = player.getUniqueId().toString();
		Slip slip = slipFromUuid(uuid);

		if (slip == null) {
			event.setCancelled(true);
			return;
		}
		if (slip.sign[0] == null)
			slipno = 1;
		else if (slip.sign[1] == null)
			slipno = 2;
		else {
			player.sendMessage(Spinalpack.code(Co.RED)
					+ "Your slip already has two endpoints. Break one first!");
			return;
		}

		event.setLine(0, Spinalpack.code(Co.DARKRED) + "Slip");

		String trunc = truncatedName(player.getName());

		event.setLine(1, trunc);
		
		Block block;
		Sign tempSign;
		for(int i = 0; i < Slip.MAX_SLIPS; i++){
			if(slip.sign[i] != null){
				block = slip.sign[i].getBlock();
				if (block.getState() instanceof Sign) {
					tempSign = (Sign)block.getState();
					if(slipSign(tempSign)){
						tempSign.setLine(1, trunc);
						tempSign.update();
					}
				}
			}
		}

		insertEndpoint(uuid, trunc, event.getBlock().getLocation(),
				player.getLocation(), slipno);

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

		Slip slip = slipFromUsername(sign.getLine(1));
		if (slip == null) {
			player.sendMessage(Spinalpack.code(Co.RED)
					+ "Critical database error!");
			return;
		}
		if (slip.numEndpoints() == 0) {
			player.sendMessage(Spinalpack.code(Co.RED)
					+ "Error: Unable to find this endpoint in the database!");
			return;
		}

		if (slip.numEndpoints() < 2) {
			player.sendMessage(Spinalpack.code(Co.RED)
					+ "Slip has no exit gate!");
			return;
		}

		long timeElapsed = System.currentTimeMillis() / 1000 - slip.timeCreated;

		if (timeElapsed < slip.cooldown
				&& !player.hasPermission("slipdisk.nocooldown")) {
			long timeRemaining = slip.cooldown - timeElapsed;
			String timeString = "" + timeRemaining / 60 + ":"
					+ (timeRemaining % 60 < 10 ? "0" : "") + timeRemaining % 60;
			player.sendMessage(Spinalpack.code(Co.GOLD)
					+ "Cooldown remaining: " + Spinalpack.code(Co.RED)
					+ timeString);
			return;
		}

		Location destination = nextSlip(slip, sign);
		if(destination == null)
			return;
		
		player.teleport(destination);
	}
	
	private Location nextSlip(Slip slip, Sign sign){
		for(int i = 0; i < Slip.MAX_SLIPS; i++)
			if(slip.sign[i].equals(sign.getLocation())){
				int j = i;
				do{
					j = (j + 1) % Slip.MAX_SLIPS;	
				}
				while(slip.sign[j] == null);
				return slip.slip[j];
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
	
	private void insertEndpoint(String uuid, String username, Location sLocation,
			Location pLocation, int slipno) {
		String query;
		PreparedStatement stmt;
		query = String
				.format("INSERT INTO Slips (uuid, username, timeCreated, cooldown, w%d, sx%d, "
						+ "sy%d, sz%d, x%d, y%d, z%d, pitch%d, yaw%d) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
						+ "ON DUPLICATE KEY UPDATE username = ?, timeCreated = ?, cooldown = cooldown + 30, w%d = ?, sx%d = ?, "
						+ "sy%d = ?, sz%d = ?, x%d = ?, y%d = ?, z%d = ?, pitch%d = ?, yaw%d = ?",
						slipno, slipno, slipno, slipno, slipno, slipno, slipno,
						slipno, slipno, slipno, slipno, slipno, slipno, slipno,
						slipno, slipno, slipno, slipno, slipno, slipno, slipno,
						slipno);
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
			stmt.setDouble(9, pLocation.getX());
			stmt.setDouble(10, pLocation.getY());
			stmt.setDouble(11, pLocation.getZ());
			stmt.setDouble(12, pLocation.getPitch());
			stmt.setDouble(13, pLocation.getYaw());
			stmt.setString(14, username);
			stmt.setLong(15, System.currentTimeMillis() / 1000);
			stmt.setString(16, sLocation.getWorld().getName());
			stmt.setFloat(17, sLocation.getBlockX());
			stmt.setFloat(18, sLocation.getBlockY());
			stmt.setFloat(19, sLocation.getBlockZ());
			stmt.setDouble(20, pLocation.getX());
			stmt.setDouble(21, pLocation.getY());
			stmt.setDouble(22, pLocation.getZ());
			stmt.setDouble(23, pLocation.getPitch());
			stmt.setDouble(24, pLocation.getYaw());

			stmt.executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	private boolean deleteSlip(Player player) {
		Slip slip = slipFromUuid(player.getUniqueId().toString());
		Block block;
		if (!deleteSlipRecord(player.getUniqueId().toString()))
			return false;
		
		for(int i = 0; i < Slip.MAX_SLIPS; i++){
			if(slip.sign[i] != null){
				block = slip.sign[i].getBlock();
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
		return unlinkSignWithUuid(uuid, 1) && unlinkSignWithUuid(uuid, 2);
	}

	private boolean unlinkSignWithUuid(String uuid, int slipno) {
		String query;
		PreparedStatement stmt;
		query = "UPDATE Slips SET w" + slipno + " = NULL WHERE uuid = ?";
		try {
			stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, uuid);
			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean unlinkSignWithUsername(String username, int slipno) {
		String query;
		PreparedStatement stmt;
		query = "UPDATE Slips SET w" + slipno + " = NULL WHERE username = ?";
		try {
			stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, username);
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
			if(slip.sign[i] != null){
				if(slip.sign[i].equals(sign.getLocation()))
					return unlinkSignWithUsername(sign.getLine(1), i + 1);
			}
		}
		return true;
	}

	private Slip slipFromUsername(String username) {
		String query = "SELECT * FROM Slips WHERE username = ?";
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
		String query = "SELECT * FROM Slips WHERE uuid = ?";
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

		if (!res.first())
			return ret;

		ret.timeCreated = res.getInt("timeCreated");
		ret.cooldown = res.getInt("cooldown");
		
		for(int i = 0; i < Slip.MAX_SLIPS; i++){
			world = res.getString("w" + (i + 1));
			if (world != null) {
				x = res.getFloat("sx" + (i + 1));
				y = res.getFloat("sy" + (i + 1));
				z = res.getFloat("sz" + (i + 1));
				ret.sign[i] = new Location(Bukkit.getWorld(world), x, y, z);

				x = res.getFloat("x" + (i + 1));
				y = res.getFloat("y" + (i + 1));
				z = res.getFloat("z" + (i + 1));
				pitch = res.getFloat("pitch" + (i + 1));
				yaw = res.getFloat("yaw" + (i + 1));
				ret.slip[i] = new Location(Bukkit.getWorld(world), x, y, z, yaw,
						pitch);
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
