package com.spinalcraft.slipdisk;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import com.spinalcraft.slipdisk.database.DatabaseClient;

public class Gate {

	public Location signLocation, playerLocation;
	public int date, sid;
		
	public static Gate fromResultSet(ResultSet resultSet) throws SQLException{
		String world = resultSet.getString("w");
		
		if (world == null){
			return null;
		}
		
		Gate gate = new Gate();
		
		gate.signLocation = new Location(Bukkit.getWorld(world), resultSet.getFloat("sx"), resultSet.getFloat("sy"), resultSet.getFloat("sz"));

		gate.playerLocation = new Location(Bukkit.getWorld(world), resultSet.getFloat("x"), resultSet.getFloat("y"), resultSet.getFloat("z"), resultSet.getFloat("yaw"), resultSet.getFloat("pitch"));
		
		gate.date = resultSet.getInt("date");
		gate.sid = resultSet.getInt("sid");
		
		return gate;
	}
	
	public static Gate fromSid(int sid){
		try {
			return Gate.fromResultSet(DatabaseClient.selectGateFromSid(sid));
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Gate create(Location gateLocation, Location playerLocation, int uid){		
		try {
			int sid = DatabaseClient.insertGate(gateLocation, playerLocation, uid);

			if(sid != -1){
				return Gate.fromSid(sid);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static boolean unlinkSlipSign(Sign sign) {
		Slip slip = Slip.fromUsername(sign.getLine(1));
		
		if (slip == null){
			return false;
		}
		
		for(int i = 0; i < Slip.MAX_GATES; i++){
			if(slip.gates[i] != null){
				if(slip.gates[i].signLocation.equals(sign.getLocation())){
					try {
						DatabaseClient.deleteGateForSid(slip.gates[i].sid);
						
						return true;
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return false;
	}
	
	public boolean validate(String username){
		Block block = this.signLocation.getBlock();
		
		if(!(block.getState() instanceof Sign)){
			return false;
		}
		
		Sign sign = (Sign)block.getState();
		
		if(!sign.getLine(1).equals(username)){
			return false;
		}
		
		return true;
	}
	
	public boolean delete(){
		try {
			DatabaseClient.deleteGateForSid(this.sid);
			
			Block block = this.signLocation.getBlock();
			
			if (block.getState() instanceof Sign) {
				if (Slip.isSlipSign((Sign) block.getState())) {
					block.breakNaturally();
				}
			}
			
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}
}
