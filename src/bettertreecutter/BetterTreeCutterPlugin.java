package bettertreecutter;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class BetterTreeCutterPlugin extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
	}

	Random random = new Random();

	HashMap<UUID, Location>lastBreak = new HashMap<UUID, Location>();
	HashMap<UUID, Integer>breakCount = new HashMap<UUID, Integer>();
	HashMap<UUID, Integer>strength = new HashMap<UUID, Integer>();

	public boolean chanceof(double percent) {
		double chance = random.nextDouble(0,100);
		if (chance < percent) {
			return true;
		}
		return false;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {

		if (event.isCancelled() || event.getPlayer() == null || event.getBlock() == null) return;

		Player player = event.getPlayer();
		Block block = event.getBlock();
		World world = block.getWorld();
		String bT = block.getType().toString().toUpperCase();
		String treeType = bT.split("_")[0].toUpperCase();




		if (!bT.contains("LOG") && !bT.contains("ROOTS") && !bT.contains("STEM")) return;

		Block tempGround = null;
		Block groundBlock;

		if (treeType.contains("MANGROVE") || treeType.contains("MUDDY")) {
			mangroveSearch:
			for (int y = block.getY(); y <= block.getY() + 9; y++) {
				for (int x = block.getX() - 5; x <= block.getX() + 5; x++) {
					for (int z = block.getZ() - 5; z <= block.getZ() + 5; z++) {
						Block checkBlock = new Location(world, x, y, z).getBlock();
						String cbT = checkBlock.getType().toString().toUpperCase();
						if (cbT.contains("MANGROVE_LOG")) {
							tempGround = getGround(checkBlock, "MANGROVE");

							break mangroveSearch;
						}
					}
				}
			}
		}

		if (tempGround == null) {
			tempGround = getGround(block, treeType);
		}

		groundBlock = tempGround;

		if (groundBlock == null) return;

		Block tempRoof = getRoof(groundBlock, treeType);

		if (tempRoof == null) return;

		Block roofBlock;

		if (treeType.contains("ACACIA")) {

			for (int y = tempRoof.getY(); y < tempRoof.getY() + 6; y++) {
				Block checkBlock = new Location(world, tempRoof.getX(), y, tempRoof.getZ()).getBlock();
				if (checkBlock.getType().toString().contains("LEAVES")) {
					if (checkBlock.getRelative(BlockFace.UP).getType().toString().contains("AIR")) {
						tempRoof = checkBlock.getRelative(BlockFace.UP);
						break;
					}
				}
			}
		}

		roofBlock = tempRoof;

		boolean safeMode = player.getWorld().getName().toUpperCase().contains("NETHER");

		int size = roofBlock.getY() - groundBlock.getY();

		if (lastBreak.get(player.getUniqueId()) == null) {
			lastBreak.put(player.getUniqueId(), event.getBlock().getLocation());
		}

		Block lastBreakBlock = lastBreak.get(player.getUniqueId()).getBlock();

		int lbX1 = lastBreakBlock.getLocation().getBlockX();
		int lbZ1 = lastBreakBlock.getLocation().getBlockZ();

		int lbX2 = event.getBlock().getLocation().getBlockX();
		int lbZ2 = event.getBlock().getLocation().getBlockZ();

		if (breakCount.get(player.getUniqueId()) == null) {
			breakCount.put(player.getUniqueId(), 0);
		}

		boolean match = false;

		if (lbX1 == lbX2 || lbX1 == lbX2-1 || lbX1 == lbX2+1) {
			if (lbZ1 == lbZ2 || lbZ1 == lbZ2-1 || lbZ1 == lbZ2+1) {
				match = true;
			}
		}

		int strng = size;

		if (match) {
			strength.put(player.getUniqueId(), size);
			breakCount.put(player.getUniqueId(), breakCount.get(player.getUniqueId()) + 1);
		} else {
			strength.put(player.getUniqueId(), size);
			lastBreak.put(player.getUniqueId(), event.getBlock().getLocation());
			breakCount.put(player.getUniqueId(), 1);
		}

		strng = strength.get(player.getUniqueId());

		event.setCancelled(true);

		ItemStack item = player.getInventory().getItemInMainHand();

		if (item.hasItemMeta()) {

			int unbreaking_level = item.getEnchantmentLevel(Enchantment.DURABILITY);

			Damageable meta = (Damageable) item.getItemMeta();

			if (chanceof((100/(unbreaking_level+1)))) {
				meta.setDamage(meta.getDamage() + 1);
				item.setItemMeta((ItemMeta) meta);
			}

		}

		if (treeType.contains("JUNGLE") || treeType.contains("DARK") || treeType.contains("SPRUCE")) {
			int grnds = 0;
			Block checkBlock = groundBlock.getRelative(BlockFace.UP);
			for (int logX = checkBlock.getX() - 1; logX <= checkBlock.getX() + 1; logX++) {
				for (int logZ = checkBlock.getZ() - 1; logZ <= checkBlock.getZ() + 1; logZ++) {
					Block logBlock = new Location(world, logX, checkBlock.getY(), logZ).getBlock();
					if (logBlock.getType().toString().split("_")[0].toUpperCase().contains(treeType)) {
						grnds++;
					}
				}
			}
			strng = strng - 2 * grnds;
		}

		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(String.valueOf(breakCount.get(player.getUniqueId())) + "/" + strng));
		if (breakCount.get(player.getUniqueId()) < strng) {
			return;
		}

		lastBreak.remove(player.getUniqueId());
		breakCount.remove(player.getUniqueId());
		strength.remove(player.getUniqueId());

		boolean treeTypeCheck = true;

		int range = 5;

		AtomicInteger minX = new AtomicInteger(range);
		AtomicInteger maxX = new AtomicInteger(range);
		AtomicInteger minZ = new AtomicInteger(range);
		AtomicInteger maxZ = new AtomicInteger(range);

		AtomicInteger y = new AtomicInteger(groundBlock.getY() - 1);

		Bukkit.getScheduler().runTaskTimer(this, (task)->{
			y.set(y.incrementAndGet());
			if (y.get() <= roofBlock.getY()) {
				Block checkLayer = world.getBlockAt(new Location(world, groundBlock.getX(), y.get(), groundBlock.getZ()));
				for (int x = checkLayer.getX() - minX.get(); x <= checkLayer.getX() + maxX.get(); x++) {
					for (int z = checkLayer.getZ() - minZ.get(); z <= checkLayer.getZ() + maxZ.get(); z++) {

						Block rpBlock = world.getBlockAt(new Location(world, x, y.get(), z));
						String rpBlockType = rpBlock.getType().toString().toUpperCase();
						String dB = rpBlock.getRelative(BlockFace.DOWN).getType().toString().toUpperCase();
						int distance = (int) checkLayer.getLocation().distance(rpBlock.getLocation());

						if (treeType.contains("BIRCH") || treeType.contains("OAK")) {
							if (rpBlockType.contains("BEE_NEST")) {
								if (distance <= 1) {
									for (int beeY = rpBlock.getY(); beeY > rpBlock.getY() - 10; beeY--) {
										Block beeCB = world.getBlockAt(new Location(world, x, beeY, z));
										String beeCBType = beeCB.getType().toString().toUpperCase();
										if (beeCBType.contains("GRASS_BLOCK") || beeCBType.contains("DIRT")) {
											String beeCBUpType = beeCB.getRelative(BlockFace.UP).getType().toString().toUpperCase();
											if (beeCBUpType.contains("AIR") || beeCBUpType.contains("GRASS")) {

												Beehive resourceBB = (Beehive) rpBlock.getState();
												int beeCount = resourceBB.getEntityCount();

												org.bukkit.block.data.type.Beehive resourceBH = (org.bukkit.block.data.type.Beehive) rpBlock.getBlockData();

												rpBlock.setType(Material.AIR);

												beeCB.getRelative(BlockFace.UP).setType(Material.BEE_NEST);
												beeCB.getRelative(BlockFace.UP).setBlockData(resourceBH);

												for (int i = 0 ; i < beeCount; i++) {
													Bee bee = (Bee) beeCB.getWorld().spawnEntity(beeCB.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getLocation(), EntityType.BEE);
													bee.setAnger(500);
													bee.setTarget(player);
												}

												break;

											}
										}
									}
								}
							}
						}

						if (bT.contains("MUSHROOM")) {
							if (roofBlock.getType().toString().toUpperCase().contains("RED")) {
								if (!rpBlockType.contains("RED") && !rpBlockType.contains("MUSHROOM_STEM")) continue;
							} else if (roofBlock.getType().toString().toUpperCase().contains("BROWN")) {
								if (!rpBlockType.contains("BROWN") && !rpBlockType.contains("MUSHROOM_STEM")) continue;
							}
						} else {
							if (treeTypeCheck) {
								if (treeType.contains("OAK")) {
									if (!rpBlockType.split("_")[0].contains("OAK") && !rpBlockType.split("_")[0].contains("AZALEA") && !rpBlockType.split("_")[0].contains("FLOWERING")) continue;
								} else if (treeType.contains("CRIMSON")) {
									if (!rpBlockType.split("_")[0].contains("CRIMSON") && !rpBlockType.contains("NETHER_WART_BLOCK")) continue;
								} else {
									if (!rpBlockType.split("_")[0].contains(treeType)) continue;
								}
							}

						}

						if (rpBlockType.contains("LOG") || rpBlockType.contains("ROOTS") || rpBlockType.contains("STEM")) {
							if (distance >= 2) {
								if (treeType.contains("ACACIA")) {
									if (distance > 3) {
										continue;
									}
								}

								if ((!dB.contains("AIR") && !dB.contains("LEAVES") && !dB.contains("VINE") && !dB.contains("WART_BLOCK")) || dB.contains("treeType")) {
									if (safeMode) {
										int rpX = rpBlock.getX();
										int rpZ = rpBlock.getZ();

										int clX = checkLayer.getX();
										int clZ = checkLayer.getZ();

										int xRange = 0;
										int zRange = 0;
										String valueX = "";
										String valueZ = "";
										if (rpX > clX) {
											xRange = rpX - clX;
											if (xRange > 0) {
												valueX = "+x";
											}

										} else {
											xRange = clX - rpX;
											if (xRange > 0) {
												valueX = "-x";
											}
										}

										if (rpZ > clZ) {
											zRange = rpZ - clZ;
											if (zRange > 0) {
												valueZ = "+z";
											}
										} else {
											zRange = clZ - rpZ;
											if (zRange > 0) {
												valueZ = "-z";
											}
										}

										if (xRange > zRange) {
											if (valueX == "+x") {
												maxX.set(xRange - 1);
											} else {
												minX.set(xRange - 1);
											}
										} else {
											if (valueZ == "+z") {
												maxZ.set(zRange - 1);
											} else {
												minZ.set(zRange - 1);
											}
										}

										if (minX.get() < 0) {
											minX.set(0);
										}
										if (maxX.get() < 0) {
											maxX.set(0);
										}
										if (minZ.get() < 0) {
											minZ.set(0);
										}
										if (maxZ.get() < 0) {
											maxZ.set(0);
										}
									}
									continue;
								}
							}
						}

						if (rpBlockType.contains("LEAVES") || rpBlockType.contains("RED_MUSHROOM_BLOCK") || rpBlockType.contains("BROWN_MUSHROOM_BLOCK") || rpBlockType.contains("WART_BLOCK")) {
							if (rpBlockType.contains("LEAVES")) {
								Leaves leaves = (Leaves) rpBlock.getBlockData();
								if (leaves.isPersistent()) continue;
								int lDistance = leaves.getDistance();
								if (treeType.contains("JUNGLE")) distance = distance - 1;
								if (treeType.contains("DARK")) distance = distance - 2;
								if (lDistance < distance) {
									continue;
								}

							}
							if (dB.contains("STEM") || dB.contains("LOG") || dB.contains("ROOTS")) {
								continue;
							}
						}

						if (rpBlockType.contains("LOG") || rpBlockType.contains("ROOTS") || rpBlockType.contains("STEM") || rpBlockType.contains("LEAVES") || rpBlockType.contains("RED_MUSHROOM_BLOCK") || rpBlockType.contains("BROWN_MUSHROOM_BLOCK") || rpBlockType.contains("WART_BLOCK") || rpBlockType.contains("VINE")  || rpBlockType.contains("SHROOMLIGHT") || rpBlockType.contains("MOSS_CARPET")) {
							if (checkLayer.getY() < roofBlock.getY()) {
								if (!rpBlockType.contains("MUSHROOM") && (rpBlockType.contains("LOG") || rpBlockType.contains("ROOTS") || rpBlockType.contains("STEM"))) {
									world.dropItem(checkLayer.getLocation().add(0, -1, 0), new ItemStack(rpBlock.getType(), 1));
									rpBlock.setType(Material.AIR);
								} else {
									rpBlock.breakNaturally();
								}

							} else if (rpBlockType.contains("LEAVES") || rpBlockType.contains("RED_MUSHROOM_BLOCK") || rpBlockType.contains("BROWN_MUSHROOM_BLOCK") || rpBlockType.contains("WART_BLOCK") || rpBlockType.contains("VINE")  || rpBlockType.contains("SHROOMLIGHT") || rpBlockType.contains("MOSS_CARPET")) {
								rpBlock.breakNaturally();
							}

							//rpBlock.setType(Material.RED_STAINED_GLASS);

						} else {
							//rpBlock.setType(Material.GLASS);
						}

					}
				}
			} else {
				task.cancel();
			}

		}, 0, 3);

	}

	public Block getGround(Block block, String treeType) {

		Block checkBlock = block;

		World world = block.getWorld();

		int airTolerance = 1;

		if (treeType.contains("MANGROVE")) airTolerance = 7;

		for (int y = block.getY(); y > block.getY() - 15; y--) {

			checkBlock = new Location(world, block.getX(), y, block.getZ()).getBlock();
			String bT = checkBlock.getType().toString().toUpperCase();

			if (!bT.contains("LOG") && !bT.contains("ROOTS") && !bT.contains("STEM") && !bT.contains("GRASS_BLOCK") && !bT.contains("DIRT") && !bT.contains("MUD") && !bT.contains("PODZOL") && !bT.contains("MYCELIUM") && !bT.contains("MOSS_BLOCK") && !bT.contains("NYLIUM") && !bT.contains("NETHERRACK") && !bT.contains("GRAVEL") && !bT.contains("SAND")) {
				if (treeType.contains("MANGROVE") && bT.contains("VINE") || treeType.contains("MANGROVE") && bT.contains("MOSS_CARPET") || treeType.contains("MANGROVE") && bT.contains("MANGROVE_PROPAGULE")) {
					continue;
				} else if (bT.contains("AIR")) {
					if (airTolerance > 0) {
						airTolerance--;
						continue;
					}
				} else if (treeType.contains("MANGROVE") && bT.contains("MANGROVE")) {
					continue;
				} else if (checkBlock.getType().equals(Material.GRASS) || bT.contains("DEAD_BUSH") || bT.contains("MOSS_CARPET")) {
					continue;
				}
				return null;
			}
			if (!bT.contains("GRASS_BLOCK") && !bT.contains("DIRT") && !bT.contains("MUD") && !bT.contains("PODZOL") && !bT.contains("MYCELIUM") && !bT.contains("MOSS_BLOCK") && !bT.contains("NYLIUM") && !bT.contains("NETHERRACK") && !bT.contains("GRAVEL") && !bT.contains("SAND")) continue;
			return checkBlock.getRelative(BlockFace.UP);


		}

		return null;

	}


	public Block getRoof(Block block, String treeType) {

		Block checkBlock = block;
		World world = block.getWorld();
		int airTolerance = 1;
		if (treeType.contains("ACACIA")) airTolerance = 4;
		if (treeType.contains("MANGROVE")) airTolerance = 15;
		int size = 0;

		for (int y = block.getY(); y < block.getY() + 35; y++) {

			checkBlock = world.getBlockAt(new Location(world, block.getX(), y, block.getZ()));
			String bT = checkBlock.getType().toString().toUpperCase();
			String uB = checkBlock.getRelative(BlockFace.UP).getType().toString().toUpperCase();


			if (treeType.contains("ACACIA")) {
				size++;
				if (size < 5) {
					continue;
				}
			}

			if (treeType.contains("MANGROVE")) {
				size++;
				if (size < 9) {
					continue;
				}
			}

			if (bT.contains("LOG") || bT.contains("ROOTS") || bT.contains("STEM") || treeType.contains("MANGROVE") && bT.contains("VINE") || treeType.contains("MANGROVE") && bT.contains("MOSS_CARPET") || treeType.contains("MANGROVE") && bT.contains("MANGROVE_PROPAGULE")) continue;
			if (bT.contains("LEAVES") || bT.contains("RED_MUSHROOM_BLOCK") || bT.contains("BROWN_MUSHROOM_BLOCK") || bT.contains("WART_BLOCK") || bT.contains("NETHERRACK")) {
				if (uB.contains("AIR") || uB.contains("NETHERRACK")){
					return checkBlock;
				} else {
					continue;
				}
			}
			if (bT.contains("AIR")) {
				if (airTolerance > 0) {
					airTolerance--;
					continue;
				}
			}
			return null;
		}
		return null;

	}

}
