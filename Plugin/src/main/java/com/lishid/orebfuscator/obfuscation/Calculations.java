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

package com.lishid.orebfuscator.obfuscation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.cache.ObfuscatedCachedChunk;
import com.lishid.orebfuscator.cache.ObfuscatedDataCache;
import com.lishid.orebfuscator.chunkmap.ChunkData;
import com.lishid.orebfuscator.chunkmap.ChunkMapManager;
import com.lishid.orebfuscator.config.ProximityHiderConfig;
import com.lishid.orebfuscator.config.WorldConfig;
import com.lishid.orebfuscator.types.BlockCoord;
import com.lishid.orebfuscator.types.BlockState;

public class Calculations {
	private static Random random = new Random();

    public static byte[] obfuscateOrUseCache(ChunkData chunkData, Player player) throws IOException {
    	if(chunkData.primaryBitMask == 0) return null;
    	
        if (!Orebfuscator.config.isEnabled()  || !Orebfuscator.config.obfuscateForPlayer(player)) {
            return null; 
        }
        
        WorldConfig worldConfig = Orebfuscator.configManager.getWorld(player.getWorld());
        
        if(!worldConfig.isEnabled()) {
        	return null;
        }
        
        byte[] output;
        
        ObfuscatedCachedChunk cache = tryUseCache(chunkData, player);
        
        if(cache != null && cache.data != null) {
        	//Orebfuscator.log("Read from cache");/*debug*/
        	output = cache.data;
        } else {
	        // Blocks kept track for ProximityHider
	        ArrayList<BlockCoord> proximityBlocks = new ArrayList<BlockCoord>();
	        
	        output = obfuscate(chunkData, player, proximityBlocks);
	
	        if (cache != null) {
	            // If cache is still allowed
	        	if(chunkData.useCache) {
		            // Save cache
		            int[] proximityList = new int[proximityBlocks.size() * 3];
		            int index = 0;
		            
		            for (int i = 0; i < proximityBlocks.size(); i++) {
		            	BlockCoord b = proximityBlocks.get(i);
		                if (b != null) {
		                    proximityList[index++] = b.x;
		                    proximityList[index++] = b.y;
		                    proximityList[index++] = b.z;
		                }
		            }
		            
		            cache.write(cache.hash, output, proximityList);
		            
		            //Orebfuscator.log("Write to cache");/*debug*/
	        	}
	        	
	            cache.free();
	        }
        }
        
        //Orebfuscator.log("Send chunk x = " + chunkData.chunkX + ", z = " + chunkData.chunkZ + " to player " + player.getName());/*debug*/

        return output;
    }
    
    private static byte[] obfuscate(ChunkData chunkData, Player player, ArrayList<BlockCoord> proximityBlocks) throws IOException {
    	WorldConfig worldConfig = Orebfuscator.configManager.getWorld(player.getWorld());
    	ProximityHiderConfig proximityHider = worldConfig.getProximityHiderConfig();
    	int initialRadius = Orebfuscator.config.getInitialRadius();

        // Track of pseudo-randomly assigned randomBlock
        int randomIncrement = 0;
        int randomIncrement2 = 0;
        int randomCave = 0;

        int engineMode = Orebfuscator.config.getEngineMode();
        int maxChance = worldConfig.getAirGeneratorMaxChance();
        int incrementMax = maxChance;

        int randomBlocksLength = worldConfig.getRandomBlocks().length;
        boolean randomAlternate = false;

		int startX = chunkData.chunkX << 4;
		int startZ = chunkData.chunkZ << 4;

		BlockState blockState = new BlockState();

		ChunkMapManager manager = new ChunkMapManager(chunkData);
		manager.init();
		
		for(int i = 0; i < manager.getSectionCount(); i++) {
            worldConfig.shuffleRandomBlocks();

            for(int offsetY = 0; offsetY < 16; offsetY++) {
            	for(int offsetZ = 0; offsetZ < 16; offsetZ++) {
                    incrementMax = (maxChance + random(maxChance)) / 2;
                    
                    for(int offsetX = 0; offsetX < 16; offsetX++) {
                    	int blockData = manager.readNextBlock();
                    	
                    	ChunkMapManager.blockDataToState(blockData, blockState);

                        if (blockState.id < 256) {
							int x = startX | offsetX;
							int y = manager.getY();
							int z = startZ | offsetZ;
	
	                        // Initialize data
	                        boolean obfuscate = false;
	                        boolean specialObfuscate = false;
	
	                        // Check if the block should be obfuscated for the default engine modes
	                        if (worldConfig.isObfuscated(blockState.id)) {
	                            if (initialRadius == 0) {
	                                // Do not interfere with PH
	                                if (proximityHider.isEnabled() && proximityHider.isProximityObfuscated(y, blockState.id)) {
	                                    if (!areAjacentBlocksTransparent(manager, player.getWorld(), false, x, y, z, 1)) {
	                                        obfuscate = true;
	                                    }
	                                } else {
	                                    // Obfuscate all blocks
	                                    obfuscate = true;
	                                }
	                            } else {
	                                // Check if any nearby blocks are transparent
	                                if (!areAjacentBlocksTransparent(manager, player.getWorld(), false, x, y, z, initialRadius)) {
	                                    obfuscate = true;
	                                }
	                            }
	                        }
	                        
	                        // Check if the block should be obfuscated because of proximity check
	                        if (!obfuscate && proximityHider.isEnabled() && proximityHider.isProximityObfuscated(y, blockState.id)) {
                            	BlockCoord block = new BlockCoord(x, y, z);
                                if (block != null) {
                                    proximityBlocks.add(block);
                                }
                                
                                obfuscate = true;
                                if (proximityHider.isUseSpecialBlock()) {
                                    specialObfuscate = true;
                                }
	                        }
	
	                        // Check if the block is obfuscated
	                        /** MODIFIED **/
	                        if(blockState.id == 5 && blockState.meta > 0 && blockState.meta < 5){obfuscate=false;}
	                        /** MODIFIED **/
	                        if (obfuscate) {
	                            if (specialObfuscate) {
	                                // Proximity hider
	                                blockState.id = proximityHider.getSpecialBlockID();
	                            } else {
	                                if (engineMode == 1) {
	                                    // Engine mode 1, replace with stone
	                                	blockState.id = worldConfig.getMode1BlockId();
	                                } else if (engineMode == 2) {
	                                    // Ending mode 2, replace with random block
	                                    if (randomBlocksLength > 1) {
	                                        randomIncrement = CalculationsUtil.increment(randomIncrement, randomBlocksLength);
	                                    }
	                                    
	                                    blockState.id = worldConfig.getRandomBlock(randomIncrement, randomAlternate);
	                                    randomAlternate = !randomAlternate;
	                                }
	                                // Anti texturepack and freecam
	                                if (worldConfig.isAntiTexturePackAndFreecam()) {
	                                	// Add random air blocks
		                                randomIncrement2 = random(incrementMax);

		                                if (randomIncrement2 == 0) {
	                                        randomCave = 1 + random(3);
	                                    }
	
	                                    if (randomCave > 0) {
	                                    	blockState.id = 0;
	                                        randomCave--;
	                                    }
	                                }
	                            }
	
	                            blockState.meta = 0;
	                        }
	
	                        // Check if the block should be obfuscated because of the darkness
	                        if (!obfuscate && worldConfig.isDarknessHideBlocks() && worldConfig.isDarknessObfuscated(blockState.id)) {
	                            if (!areAjacentBlocksBright(player.getWorld(), x, y, z, 1)) {
	                                // Hide block, setting it to air
	                            	blockState.id = 0;
	                            	blockState.meta = 0;
	                            }
	                        }
	                        
	                        blockData = ChunkMapManager.blockStateToData(blockState);
                        } else {
                        	blockData = 0;
                        }
                        
						if(offsetY == 0 && offsetZ == 0 && offsetX == 0) {
							manager.finalizeOutput();							
							manager.initOutputPalette();
							addBlocksToPalette(manager, worldConfig);
							manager.initOutputSection();
						}
						
                        manager.writeOutputBlock(blockData);
                    }
                }
            }
        }
		
		manager.finalizeOutput();
		
		byte[] output = manager.createOutput();

        ProximityHider.addProximityBlocks(player, chunkData.chunkX, chunkData.chunkZ, proximityBlocks);
        
        //Orebfuscator.log("Create new chunk data for x = " + chunkData.chunkX + ", z = " + chunkData.chunkZ);/*debug*/
        
        return output;
    }
    
    private static void addBlocksToPalette(ChunkMapManager manager, WorldConfig worldConfig) {
    	if(!manager.inputHasNonAirBlock()) {
    		return;
    	}

    	for(int id : worldConfig.getPaletteBlocks()) {
    		int blockData = ChunkMapManager.getBlockDataFromId(id);
    		manager.addToOutputPalette(blockData);
    	}
    }
    
    private static ObfuscatedCachedChunk tryUseCache(ChunkData chunkData, Player player) {
        if (!Orebfuscator.config.isUseCache()) return null;
        
        chunkData.useCache = true;
        
        // Hash the chunk
        long hash = CalculationsUtil.Hash(chunkData.data, chunkData.data.length);
        // Get cache folder
        File cacheFolder = new File(ObfuscatedDataCache.getCacheFolder(), player.getWorld().getName());
        // Create cache objects
        ObfuscatedCachedChunk cache = new ObfuscatedCachedChunk(cacheFolder, chunkData.chunkX, chunkData.chunkZ);
        
        // Check if hash is consistent
        cache.read();
        
        long storedHash = cache.getHash();

        if (storedHash == hash && cache.data != null) {
            int[] proximityList = cache.proximityList;
        	ArrayList<BlockCoord> proximityBlocks = new ArrayList<BlockCoord>();
        	
            // Decrypt chest list
            if (proximityList != null) {
            	int index = 0;
            	
                while (index < proximityList.length) {
                	int x = proximityList[index++];
                	int y = proximityList[index++];
                	int z = proximityList[index++];
                	BlockCoord b = new BlockCoord(x, y, z);
                	
                	if(b != null) {
                		proximityBlocks.add(b);
                	}
                }
            }

            // ProximityHider add blocks
            ProximityHider.addProximityBlocks(player, chunkData.chunkX, chunkData.chunkZ, proximityBlocks);

            // Hash match, use the cached data instead and skip calculations
            return cache;
        }
        
        cache.hash = hash;
        cache.data = null;
        
        return cache;
    }
    
    public static boolean areAjacentBlocksTransparent(
    		ChunkMapManager manager,
    		World world,
    		boolean checkCurrentBlock,
    		int x,
    		int y,
    		int z,
    		int countdown
    		) throws IOException
    {
        if (y >= world.getMaxHeight() || y < 0)
            return true;

        if(checkCurrentBlock) {
	    	ChunkData chunkData = manager.getChunkData();
	        int blockData = manager.get(x, y, z);
	        int id;
	
	        if (blockData < 0) {
	        	id = Orebfuscator.nms.getBlockId(world, x, y, z);
	        	
	            if (id < 0) {
	                id = 1;
	                chunkData.useCache = false;
	            }
	        } else {
	        	id = ChunkMapManager.getBlockIdFromData(blockData);
	        }
	
	        if (Orebfuscator.config.isBlockTransparent(id)) {
	            return true;
	        }
        }

        if (countdown == 0)
            return false;

        if (areAjacentBlocksTransparent(manager, world, true, x, y + 1, z, countdown - 1))
            return true;
        if (areAjacentBlocksTransparent(manager, world, true, x, y - 1, z, countdown - 1))
            return true;
        if (areAjacentBlocksTransparent(manager, world, true, x + 1, y, z, countdown - 1))
            return true;
        if (areAjacentBlocksTransparent(manager, world, true, x - 1, y, z, countdown - 1))
            return true;
        if (areAjacentBlocksTransparent(manager, world, true, x, y, z + 1, countdown - 1))
            return true;
        if (areAjacentBlocksTransparent(manager, world, true, x, y, z - 1, countdown - 1))
            return true;

        return false;
    }

    public static boolean areAjacentBlocksBright(World world, int x, int y, int z, int countdown) {
    	if(Orebfuscator.nms.getBlockLightLevel(world, x, y, z) > 0) {
    		return true;
    	}

        if (countdown == 0)
            return false;

        if (areAjacentBlocksBright(world, x, y + 1, z, countdown - 1))
            return true;
        if (areAjacentBlocksBright(world, x, y - 1, z, countdown - 1))
            return true;
        if (areAjacentBlocksBright(world, x + 1, y, z, countdown - 1))
            return true;
        if (areAjacentBlocksBright(world, x - 1, y, z, countdown - 1))
            return true;
        if (areAjacentBlocksBright(world, x, y, z + 1, countdown - 1))
            return true;
        if (areAjacentBlocksBright(world, x, y, z - 1, countdown - 1))
            return true;

        return false;
    }
    
    private static int random(int max) {
        return random.nextInt(max);
    }
}