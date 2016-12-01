package com.spinalcraft.slipdisk;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Days;

import net.md_5.bungee.api.ChatColor;

public class Utils {
	public static String truncatedName(String name) {
		String trunc = ChatColor.DARK_BLUE + name;
		trunc = trunc.substring(0, Math.min(trunc.length(), 15));
		return trunc;
	}

	public static int daysUntil(long dayZero){
		Date past = new Date(dayZero);
		Date today = new Date();
		return Days.daysBetween(new DateTime(today), new DateTime(past)).getDays() - 1;
	}
	
	public static String timeStringFromSeconds(long seconds){
		int hours = (int) (seconds / 3600);
		seconds = seconds % 3600;
		int minutes = (int) (seconds / 60);
		seconds = seconds % 60;
		
		return "" + hours + ":" + (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
	}
}
