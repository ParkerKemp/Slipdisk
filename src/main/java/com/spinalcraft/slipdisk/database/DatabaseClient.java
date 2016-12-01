package com.spinalcraft.slipdisk.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Location;

import com.spinalcraft.skull.SpinalcraftPlugin;


public class DatabaseClient {
	
	public static void deleteGateForSid(int sid) throws SQLException{
		String query = "DELETE FROM slip_slips WHERE sid = ?";
		
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		
		stmt.setInt(1, sid);
		stmt.executeUpdate();
	}
	
	public static int[] selectAllUsers() throws SQLException{
		//Potential race condition could be prevented here with a transaction.
		
		int userCount = DatabaseClient.selectUserCount();
		int[] users = new int[userCount];
		
		String query = "SELECT uid from slip_users";
		
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		ResultSet resultSet = stmt.executeQuery();
		
		for(int i = 0; i < userCount; i++){
			resultSet.next();
			users[i] = resultSet.getInt(1);
		}
		
		return users;
	}
	
	public static int selectUserCount() throws SQLException{
		String query = "SELECT COUNT(*) from slip_users";
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		ResultSet resultSet = stmt.executeQuery();
		
		resultSet.first();
		
		return resultSet.getInt(1);
	}
	
	public static ResultSet selectSlipFromUsername(String username) throws SQLException {
		String query = "SELECT sid, date, w, sx, sy, sz, x, y, z, pitch, yaw FROM slip_slips s JOIN slip_users u ON s.uid = u.uid AND u.username = ?";
		PreparedStatement stmt;
		
		stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setString(1, username);
		
		return stmt.executeQuery();
	}

	public static ResultSet selectProfileDataFromUsername(String username) throws SQLException{
		String query = "SELECT u.uid, uuid, username, r.role, max, cddayzero, cdimmune FROM slip_users u, slip_info i, slip_roles r WHERE u.uid = i.uid AND i.role = r.role AND u.username = ?";
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setString(1, username);
		
		return stmt.executeQuery();
	}

	public static ResultSet selectSlipFromUuid(UUID uuid) throws SQLException {
		String query = "SELECT sid, date, w, sx, sy, sz, x, y, z, pitch, yaw FROM slip_slips s JOIN slip_users u ON s.uid = u.uid AND u.uuid = ?";
		PreparedStatement stmt;
		
		stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setString(1, uuid.toString());
		
		return stmt.executeQuery();
	}
	
	public static ResultSet selectProfileDataFromUuid(UUID uuid) throws SQLException{
		String query = "SELECT u.uid, uuid, username, r.role, max, cddayzero, cdimmune FROM slip_users u, slip_info i, slip_roles r WHERE u.uid = i.uid AND i.role = r.role AND u.uuid = ?";
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setString(1, uuid.toString());
		
		return stmt.executeQuery();
	}

	public static ResultSet selectSlipFromUserID(int uid) throws SQLException {
		String query = "SELECT sid, date, w, sx, sy, sz, x, y, z, pitch, yaw FROM slip_slips WHERE uid = ?";
		PreparedStatement stmt;
		
		stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setInt(1, uid);
		
		return stmt.executeQuery();	
	}
	
	public static ResultSet selectProfileDataFromUserID(int uid) throws SQLException{
		String query = "SELECT u.uid, uuid, username, r.role, max, cddayzero, cdimmune FROM slip_users u, slip_info i, slip_roles r WHERE u.uid = i.uid AND i.role = r.role AND u.uid = ?";
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setInt(1, uid);
		return stmt.executeQuery();
	}
	
	public static void updateProfileWithUsername(int uid, String username) throws SQLException{
		String query = "UPDATE slip_users SET username = ? WHERE uid = ?";
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setString(1, username);
		stmt.setInt(2, uid);
		stmt.execute();
	}
	
	public static int insertProfile(UUID uuid, String username) throws SQLException{
		//Should definitely be a transaction
		
		int uid;
		String query = "INSERT INTO slip_users (uuid, username) VALUES (?, ?)";
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setString(1, uuid.toString());
		stmt.setString(2, username);
		stmt.executeUpdate();
		
		ResultSet resultSet = stmt.getGeneratedKeys();
		resultSet.first();
		uid = resultSet.getInt(1);
		
		query = "INSERT INTO slip_info (uid, cddayzero, role) ?, ?, 'user'";
		stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setInt(1, uid);
		stmt.setLong(2, System.currentTimeMillis());
		stmt.executeUpdate();
		
		return uid;
	}
	
	public static void updateProfileWithRole(int uid, String role) throws SQLException{
		String query = "UPDATE slip_info SET role = ? WHERE uid = ?";
				
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setString(1, role);
		stmt.setInt(2, uid);
		stmt.executeUpdate();
	}
	
	public static long selectCooldownDayZeroFromUserID(int uid) throws SQLException{
		String query = "SELECT cddayzero FROM slip_info WHERE uid = ?";
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setInt(1, uid);
		ResultSet rs = stmt.executeQuery();
		
		return rs.getLong("cddayzero");
	}
	
	public static void updateProfileWithCooldownDayZero(int uid, long cooldownDayZero) throws SQLException{
		String query = "UPDATE slip_info SET cddayzero = ? WHERE uid = ?";
	
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setLong(1, cooldownDayZero);
		stmt.setInt(2, uid);
		stmt.executeUpdate();
	}
	
	public static ResultSet selectGateFromSid(int sid) throws SQLException{
		String query = "SELECT * FROM slip_slips WHERE sid = ?";
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setInt(1, sid);
		return stmt.executeQuery();
	}
	
	public static int insertGate(Location gateLocation, Location playerLocation, int uid) throws SQLException {
		String query = "INSERT INTO slip_slips (uid, date, w, sx, sy, sz, x, y, z, pitch, yaw) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setInt(1, uid);
		stmt.setLong(2, System.currentTimeMillis() / 1000);
		stmt.setString(3, gateLocation.getWorld().getName());
		stmt.setFloat(4, gateLocation.getBlockX());
		stmt.setFloat(5, gateLocation.getBlockY());
		stmt.setFloat(6, gateLocation.getBlockZ());
		stmt.setDouble(7, playerLocation.getX());
		stmt.setDouble(8, playerLocation.getY());
		stmt.setDouble(9, playerLocation.getZ());
		stmt.setDouble(10, playerLocation.getPitch());
		stmt.setDouble(11, playerLocation.getYaw());
		stmt.executeUpdate();
		ResultSet resultSet = stmt.getGeneratedKeys();
		resultSet.first();
		return resultSet.getInt(1);
	}

	public static void deleteSlipForUid(int uid) throws SQLException {
		String query = "DELETE s.* FROM slip_slips WHERE uid = ?";
		PreparedStatement stmt = SpinalcraftPlugin.prepareStatement(query);
		stmt.setInt(1, uid);
		stmt.executeUpdate();
	}
	
	public static void createTables(){
		String query;
		query = "CREATE TABLE IF NOT EXISTS slip_users (uid INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36) UNIQUE, username VARCHAR(32))";
		SpinalcraftPlugin.update(query);
		query = "CREATE TABLE IF NOT EXISTS slip_roles (role VARCHAR(16) PRIMARY KEY, max INT, cdimmune BIT(1))";
		SpinalcraftPlugin.update(query);
		query = "CREATE TABLE IF NOT EXISTS slip_slips (sid INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uid INT, date INT, w VARCHAR(32), "
				+ "sx FLOAT, sy FLOAT, sz FLOAT, x FLOAT, y FLOAT, z FLOAT, pitch FLOAT, yaw FLOAT, FOREIGN KEY (uid) REFERENCES slip_users(uid))";
		SpinalcraftPlugin.update(query);
		query = "CREATE TABLE IF NOT EXISTS slip_info (uid INT PRIMARY KEY, cddayzero BIGINT, role VARCHAR(16), FOREIGN KEY (uid) REFERENCES slip_users(uid), FOREIGN KEY (role) REFERENCES slip_roles(role))";
		SpinalcraftPlugin.update(query);
	}
}
