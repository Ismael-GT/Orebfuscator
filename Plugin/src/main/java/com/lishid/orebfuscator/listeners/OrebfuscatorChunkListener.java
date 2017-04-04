/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.listeners;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.Material;
import org.bukkit.Location;

import com.lishid.orebfuscator.obfuscation.ChunkReloader;

public class OrebfuscatorChunkListener implements Listener {
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	public void onChunkLoad(ChunkLoadEvent event) {
		Chunk chunk = event.getChunk();
		/** MODIFIED **/
		for (Entity ent : chunk.getEntities()){
			   if(ent instanceof StorageMinecart){
			    Location entityPos = ent.getLocation();
			    StorageMinecart cart = (StorageMinecart) ent;
			    ItemStack[] cofre = cart.getInventory().getContents().clone();
			    cart.getInventory().clear();
			       Block block = entityPos.getBlock();
			       entityPos.getBlock().setType(Material.CHEST);
			       Chest chest = (Chest)block.getState();
			       chest.getInventory().setContents(cofre);
			    cart.remove();
			    System.out.println("minecart encontrada");
			   }
			  }
		/** MODIFIED **/
		ChunkReloader.addLoadedChunk(event.getWorld(), chunk.getX(), chunk.getZ());
		
		//Orebfuscator.log("Chunk x = " + chunk.getX() + ", z = " + chunk.getZ() + " is loaded");/*debug*/
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	public void onChunkUnload(ChunkUnloadEvent event) {
		Chunk chunk = event.getChunk();
		ChunkReloader.addUnloadedChunk(event.getWorld(), chunk.getX(), chunk.getZ());
		
		//Orebfuscator.log("Chunk x = " + chunk.getX() + ", z = " + chunk.getZ() + " is unloaded");/*debug*/
	}
}
