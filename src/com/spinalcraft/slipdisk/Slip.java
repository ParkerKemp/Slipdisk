package com.spinalcraft.slipdisk;

import org.bukkit.Location;

class SlipSign{
	public Location sign, slip;
	public int date, sid;
}

public class Slip {
	public static final int MAX_SLIPS = 5;
	public SlipSign signs[];
	
	public Slip(){
		signs = new SlipSign[MAX_SLIPS];
		for(int i = 0; i < MAX_SLIPS; i++){
			signs[i] = null;
		}
	}
	
	public int numEndpoints(){
		int count = 0;
		for(int i = 0; i < MAX_SLIPS; i++)
			if(signs[i] != null)
				count++;
		return count;
	}
	
	public int getMostRecentDate(){
		int max = 0;
		for(int i = 0; i < MAX_SLIPS; i++)
			if(signs[i] != null)
				max = Math.max(max, signs[i].date);
		return max;
	}
}
