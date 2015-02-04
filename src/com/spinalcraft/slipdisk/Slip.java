package com.spinalcraft.slipdisk;

import org.bukkit.Location;

public class Slip {
	public static final int MAX_SLIPS = 2;
	public Location sign[], slip[];
	public int timeCreated, cooldown;
	
	public Slip(){
		sign = new Location[MAX_SLIPS];
		slip = new Location[MAX_SLIPS];
		for(int i = 0; i < MAX_SLIPS; i++){
			sign[i] = null;
			slip[i] = null;
		}
	}
	
	public int numEndpoints(){
		int count = 0;
		for(int i = 0; i < MAX_SLIPS; i++)
			if(sign[i] != null)
				count++;
		return count;
	}
}
