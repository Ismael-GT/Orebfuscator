/*
 * Copyright (C) 2011-2014 lishid.  All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.lishid.orebfuscator.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import org.bukkit.block.Block;
import com.lishid.orebfuscator.DeprecatedMethods;
import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.hithack.BlockHitManager;
import com.lishid.orebfuscator.obfuscation.BlockUpdate;

public class OrebfuscatorBlockListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        /** MODIFIED 2 **/
    	Block block = event.getBlock();
        Orebfuscator.nms.notifyBlockChange(event.getPlayer().getWorld(), Orebfuscator.nms.getBlockInfo(event.getPlayer().getWorld(), block.getX()+1, block.getY(), block.getZ()));
           Orebfuscator.nms.notifyBlockChange(event.getPlayer().getWorld(), Orebfuscator.nms.getBlockInfo(event.getPlayer().getWorld(), block.getX()-1, block.getY(), block.getZ()));
           Orebfuscator.nms.notifyBlockChange(event.getPlayer().getWorld(), Orebfuscator.nms.getBlockInfo(event.getPlayer().getWorld(), block.getX(), block.getY()+1, block.getZ()));
           Orebfuscator.nms.notifyBlockChange(event.getPlayer().getWorld(), Orebfuscator.nms.getBlockInfo(event.getPlayer().getWorld(), block.getX(), block.getY()-1, block.getZ()));
           Orebfuscator.nms.notifyBlockChange(event.getPlayer().getWorld(), Orebfuscator.nms.getBlockInfo(event.getPlayer().getWorld(), block.getX(), block.getY(), block.getZ()+1));
           Orebfuscator.nms.notifyBlockChange(event.getPlayer().getWorld(), Orebfuscator.nms.getBlockInfo(event.getPlayer().getWorld(), block.getX(), block.getY(), block.getZ()-1));
           BlockHitManager.breakBlock(event.getPlayer(), block);
        // BlockUpdate.update(event.getBlock());
        /** MODIFIED 2 **/
        // BlockHitManager.breakBlock(event.getPlayer(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (!Orebfuscator.config.isUpdateOnDamage()) {
            return;
        }

        if (!BlockUpdate.needsUpdate(event.getBlock())) {
            return;
        }

        if (!BlockHitManager.hitBlock(event.getPlayer(), event.getBlock())) {
            return;
        }
        /** MODIFIED 3 **/
        Block block = event.getBlock();
        Orebfuscator.nms.notifyBlockChange(event.getPlayer().getWorld(), Orebfuscator.nms.getBlockInfo(event.getPlayer().getWorld(), block.getX(), block.getY(), block.getZ()));
        /** MODIFIED 3 **/
        //BlockUpdate.update(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() != Material.SAND && event.getBlock().getType() != Material.GRAVEL) {
            return;
        }

        if (!DeprecatedMethods.applyPhysics(event.getBlock())) {
            return;
        }

        BlockUpdate.update(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        BlockUpdate.update(event.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        BlockUpdate.update(event.getBlock());
    }
}