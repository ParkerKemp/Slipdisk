package com.spinalcraft.slipdisk;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.joda.time.DateTime;

import com.spinalcraft.slipdisk.database.DatabaseClient;

import net.md_5.bungee.api.ChatColor;

public class Slip {
	public static final int MAX_GATES = 5;
	
	public Gate gates[];
	public int uid;
	public String uuid;
	public String username;
	public String role;
	public int max;
	public int cd;
	public boolean cdImmune;
	
	private Slip(){
		this.gates = new Gate[MAX_GATES];
		for(int i = 0; i < MAX_GATES; i++){
			this.gates[i] = null;
		}
		
		this.uid = -1;
	}

	public int numEndpoints(){
		int count = 0;
		for(int i = 0; i < MAX_GATES; i++)
			if(gates[i] != null)
				count++;
		return count;
	}
	
	public int getMostRecentDate(){
		int max = 0;
		for(int i = 0; i < MAX_GATES; i++)
			if(gates[i] != null)
				max = Math.max(max, gates[i].date);
		return max;
	}

	public boolean pastGracePeriod(){
		long timeElapsed = System.currentTimeMillis() / 1000 - this.getMostRecentDate();
		return timeElapsed > 60;
	}
	
	public boolean hasValidProfile(){
		return this.uid != -1;
	}
	
	public boolean coolingDown(){
		return this.cooldownTimeElapsed() < this.cd && !this.cdImmune;
	}
	
	public long cooldownTimeElapsed(){
		return System.currentTimeMillis() / 1000 - this.getMostRecentDate();
	}
	
	public long cooldownTimeRemaining(){
		return this.cd - this.cooldownTimeElapsed();
	}
	
	public static boolean isSlipSign(Sign sign) {
		return sign.getLine(0).equals(ChatColor.DARK_RED + "Slip");
	}

	private int attachGates(ResultSet resultSet) throws SQLException{		
		int i = 0;
	
		while (resultSet.next()){
			this.gates[i++] = Gate.fromResultSet(resultSet);
		}
		
		return i;
	}
	
	public void createProfileForPlayer(Player player){
		this.createProfile(player.getUniqueId(), player.getName());
	}
	
	public void createProfile(UUID uuid, String username){
		try {
			int uid = DatabaseClient.insertProfile(uuid, username);
			
			this.attachGates(DatabaseClient.selectSlipFromUserID(uid));
			this.attachProfileData(DatabaseClient.selectProfileDataFromUserID(uid));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void attachProfileData(ResultSet resultSet) throws SQLException{
		if(resultSet.first()){
			this.uid = resultSet.getInt("u.uid");
			this.uuid = resultSet.getString("uuid");
			this.role = resultSet.getString("r.role");
			this.username = resultSet.getString("username");
			this.max = resultSet.getInt("max");
			
			this.setCooldownFromDayZero(resultSet.getLong("cddayzero"));
			
			this.cdImmune = resultSet.getInt("cdimmune") == 1;
		}
	}
	
	private void verifyUsername(String username){
		if (!this.username.equals(username)){
			try {
				DatabaseClient.updateProfileWithUsername(this.uid, username);
				
				this.username = username;
				this.setSignLabels();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean shouldUpdateCooldown(){
		return this.pastGracePeriod() && this.numEndpoints() > 0 && !this.cdImmune;
	}
	
	public boolean canCreateGate(){
		return this.numEndpoints() >= this.max;
	}
	
	private void setCooldownFromDayZero(long cooldownDayZero){
		this.cd = Utils.daysUntil(cooldownDayZero) * 15;
	}
	
	public void updateCooldown(){
		long cooldownDayZero = this.getNewCooldownDayZero();
		
		if(cooldownDayZero == -1){
			return;
		}
		
		try {
			DatabaseClient.updateProfileWithCooldownDayZero(uid, cooldownDayZero);
			this.setCooldownFromDayZero(cooldownDayZero);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private long getNewCooldownDayZero(){
		long oldCd;
		try {
			oldCd = DatabaseClient.selectCooldownDayZeroFromUserID(this.uid);
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
		
		//adjust cd as if dayzero were at most 6 days prior to current date
		int modifier = -6 - Math.min(Utils.daysUntil(oldCd), -6);
		DateTime dt = new DateTime(oldCd);
		
		dt = dt.plusDays(2 + modifier);
		
		return dt.getMillis();
	}
	
	private void crunchGates(){
		for (int i = 0; i < Slip.MAX_GATES - 1; i++){
			int j = i;
			
			while(j < Slip.MAX_GATES && this.gates[j] == null){
				j++;
			}
			
			if(j > i && j < Slip.MAX_GATES){
				this.gates[i] = this.gates[j];
				this.gates[j] = null;
			}
		}
	}
	
	public boolean createGate(Location signLocation, Location playerLocation){
		Gate gate = Gate.create(signLocation, playerLocation, this.uid);
		
		if(gate != null){
			this.crunchGates();
			
			int i = 0;
			
			while(this.gates[i] == null){
				i++;
			}
			
			gates[i] = gate;
			
			if(this.shouldUpdateCooldown()){
				this.updateCooldown();
			}
			
			return true;
		}
		
		return false;
	}
	
	public boolean deleteGates() {
		boolean success = true;
		
		for(int i = 0; i < Slip.MAX_GATES; i++){
			if(this.gates[i] != null){
				success &= this.gates[i].delete();
			}
		}
		
		return success;
	}

	public static Slip fromPlayer(Player player){
		Slip slip = new Slip();
		
		try {
			slip.attachGates(DatabaseClient.selectSlipFromUuid(player.getUniqueId()));
			slip.attachProfileData(DatabaseClient.selectProfileDataFromUuid(player.getUniqueId()));
			slip.verifyUsername(Utils.truncatedName(player.getName()));
	
			return slip;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Slip fromUsername(String username){
		Slip slip = new Slip();
		
		try {
			slip.attachGates(DatabaseClient.selectSlipFromUsername(username));
			slip.attachProfileData(DatabaseClient.selectProfileDataFromUsername(username));
	
			return slip;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Slip fromUuid(UUID uuid){
		//Assumes profile exists
		
		Slip slip = new Slip();
		
		try {
			slip.attachGates(DatabaseClient.selectSlipFromUuid(uuid));
			slip.attachProfileData(DatabaseClient.selectProfileDataFromUuid(uuid));
	
			return slip;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Slip fromUserID(int uid){
		Slip slip = new Slip();
		
		try {
			slip.attachGates(DatabaseClient.selectSlipFromUserID(uid));
			slip.attachProfileData(DatabaseClient.selectProfileDataFromUserID(uid));
	
			return slip;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public Gate nextGate(Sign sign){
		for(int i = 0; i < Slip.MAX_GATES; i++)
			if(this.gates[i].signLocation.equals(sign.getLocation())){
				int j = i;
				
				do{
					j = (j + 1) % Slip.MAX_GATES;	
				} while(this.gates[j] == null);
				
				return this.gates[j];
			}
		return null;
	}
	
	private static Slip[] getAllSlips(){
		Slip[] slips = null;
		
		try {
			int[] users = DatabaseClient.selectAllUsers();
			int userCount = users.length;
			
			slips = new Slip[userCount];
			
			for(int i = 0; i < userCount; i++){
				slips[i] = Slip.fromUserID(users[i]);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return slips;
	}
	
	public void setSignLabels(){
		Block block;
		Sign tempSign;
		for(int i = 0; i < Slip.MAX_GATES; i++){
			if(this.gates[i] != null){
				block = this.gates[i].signLocation.getBlock();
				if (block.getState() instanceof Sign) {
					tempSign = (Sign)block.getState();
					if(Slip.isSlipSign(tempSign)){
						tempSign.setLine(1, this.username);
						tempSign.update();
					}
				}
			}
		}
	}
	
	public int validateGates(){
		int count = 0;
		
		for (int i = 0; i < this.gates.length; i++){
			if (this.gates[i].validate(this.username)){
				try {
					DatabaseClient.deleteGateForSid(this.gates[i].sid);
					
					this.gates[i] = null;
					
					count++;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
		return count;
	}
	
	public static void cleanUpDatabase(Slipdisk slipdisk){
		new BukkitRunnable(){
			public void run(){
				int count = 0;
				
				Slip[] slips = Slip.getAllSlips();
				
				if(slips != null){
					for (Slip slip : slips){
						count += slip.validateGates();
					}
				}
				
				if(count > 0){
					System.out.println("Slipdisk: deleted " + count + " out-of-sync records.");
				}
			}
		}.runTask(slipdisk);
	}
}


