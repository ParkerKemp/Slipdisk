package com.spinalcraft.slipdisk.event;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.spinalcraft.slipdisk.Slip;
import com.spinalcraft.slipdisk.Slipdisk;
import com.spinalcraft.slipdisk.Utils;
import com.spinalcraft.slipdisk.Gate;

import net.md_5.bungee.api.ChatColor;

public class EventListener implements Listener{
	
	private Slipdisk slipdisk;
	
	public EventListener(Slipdisk slipdisk) {
		this.slipdisk = slipdisk;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(SignChangeEvent event) {
		if (!(event.getLine(0).equalsIgnoreCase("#slip") || event.getLine(1).equalsIgnoreCase("#slip")))
			return;

		if (!event.getPlayer().hasPermission("slipdisk.createslip")) {
			event.getPlayer().sendMessage(ChatColor.RED + "You're not allowed to create slips!");
			return;
		}

		Player player = event.getPlayer();
		
		Slip slip = Slip.fromPlayer(player);
		
		if(slip == null){
			player.sendMessage(ChatColor.RED + "Database error!");
			event.setCancelled(true);
			return;
		}
		
		if(!slip.hasValidProfile()){
			slip.createProfileForPlayer(player);
		}

		if(!slip.canCreateGate()){
			player.sendMessage(ChatColor.RED + "Your slip already has " + slip.max + " endpoints. Break one first!");
			event.setCancelled(true);
			return;
		}

		if(!slip.createGate(event.getBlock().getLocation(), player.getLocation())){
			player.sendMessage(ChatColor.RED + "Database error!");
			event.setCancelled(true);
			return;
		}
		
		event.setLine(0, ChatColor.DARK_RED + "Slip");
		
		slip.setSignLabels();

		player.sendMessage(ChatColor.GOLD + "Created a new slip gate!");
		slipdisk.console.sendMessage(ChatColor.GOLD + player.getName() + " created a slip gate!");	
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK)){
			return;
		}

		if (!(event.getClickedBlock().getState() instanceof Sign)){
			return;
		}

		Sign sign = (Sign) event.getClickedBlock().getState();
		
		if (!Slip.isSlipSign(sign)){
			return;
		}
		
		if (!event.getPlayer().hasPermission("slipdisk.useslip")) {
			event.getPlayer().sendMessage(ChatColor.RED + "You're not allowed to use slips!");
			return;
		}
		
		Player player = event.getPlayer();
		
		Slip slip = Slip.fromUsername(sign.getLine(1));
		
		if (slip == null) {
			player.sendMessage(ChatColor.RED + "Error: Slip owner not found in database!");
			return;
		}
		
		if (slip.numEndpoints() == 0) {
			player.sendMessage(ChatColor.RED + "Error: Slip gate not found in database!");
			return;
		}
		
		if (slip.numEndpoints() < 2) {
			player.sendMessage(ChatColor.RED + "Slip has no exit gate!");
			return;
		}

		if(slip.coolingDown()){
			player.sendMessage(ChatColor.GOLD + "Cooldown remaining: " + ChatColor.RED + Utils.timeStringFromSeconds(slip.cooldownTimeRemaining()));
			return;
		}
		
		Gate nextGate = slip.nextGate(sign);
		
		if(nextGate == null)
			return;
		
		player.teleport(nextGate.playerLocation);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.SIGN_POST || event.getBlock().getType() == Material.WALL_SIGN) {
			Sign sign = (Sign) event.getBlock().getState();
			
			if (Slip.isSlipSign(sign)) {
				if (Gate.unlinkSlipSign(sign))
					event.getPlayer().sendMessage(ChatColor.RED + "Unlinked a slip gate!");
				else {
					event.getPlayer().sendMessage(ChatColor.RED	+ "Unable to unlink slip gate due to database error.");
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.getBlock().getType() == Material.SIGN_POST || event.getBlock().getType() == Material.WALL_SIGN) {
			Sign sign = (Sign) event.getBlock().getState();
			Block attachedTo = event.getBlock().getRelative(((org.bukkit.material.Sign) sign.getData()).getAttachedFace());
			
			if (attachedTo.getType() == Material.AIR) {
				if (Slip.isSlipSign(sign)) {
					if (!Gate.unlinkSlipSign(sign)){
						event.setCancelled(true);
					}
				}
			}
		}
	}
}
