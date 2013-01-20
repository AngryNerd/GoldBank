package net.amigocraft.GoldBank.api;

import java.util.HashMap;

import net.amigocraft.GoldBank.GoldBank;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerInv extends JavaPlugin {

	/**
	 * Attempts to find the amount of gold in a specfied player's inventory.
	 * @param p The player who's inventory should be searched
	 * @return The amount of gold (in nuggets) in the player's inventory
	 */
	public static int getGoldInPlayerInv(Player p){
		Inventory inv = p.getInventory();
		int gold = 0;
		for (int i = 0; i < inv.getSize(); i++){
			if (inv.getItem(i) != null){
				if (inv.getItem(i).getType() == Material.GOLD_NUGGET)
					gold += inv.getItem(i).getAmount();
				else if (inv.getItem(i).getType() == Material.GOLD_INGOT)
					gold += (inv.getItem(i).getAmount() * 9);
				else if (inv.getItem(i).getType() == Material.GOLD_BLOCK)
					gold += (inv.getItem(i).getAmount() * 81);
			}
		}
		return gold;
	}

	/**
	 * Attempts to add a specific amount of gold (defined in nuggets) to a player's inventory.
	 * @param p The player whose inventory should be modified
	 * @param amount The amount of gold (in nuggets) to be added to the player's inventory.
	 * @param simplify Whether or not the gold should be simplified (into blocks and ingots) before being distributed as nuggets
	 * @return Whether or not the gold was successfully added (returns false if not enough space is available)
	 */
	@SuppressWarnings("deprecation")
	public static boolean addGoldToPlayerInv(Player p, int amount){
		Inventory inv = p.getInventory();
		Inventory newInv = GoldBank.plugin.getServer().createInventory(null, inv.getSize());
		newInv.setContents(inv.getContents());
		int block = 0;
		int ingot = 0;
		int nugget = 0;
		int remaining = amount;
		if (GoldBank.getAmountInInv(inv, Material.GOLD_BLOCK) >= (amount / 81) && amount / 81 >= 1){
			block = amount / 81;
			block = block * 81;
			remaining = amount - block;
		}
		if (GoldBank.getAmountInInv(inv, Material.GOLD_INGOT) >= (amount / 9) && remaining >= 9){
			ingot = amount / 9;
			ingot = ingot * 9;
			remaining = remaining - ingot;
		}
		if (remaining != 0){
			nugget = remaining;
		}
		p.updateInventory();
		HashMap<Integer, ItemStack> unfitBlock = newInv.addItem(new ItemStack(Material.GOLD_BLOCK, block));
		HashMap<Integer, ItemStack> unfitIngot = newInv.addItem(new ItemStack(Material.GOLD_INGOT, ingot));
		HashMap<Integer, ItemStack> unfitNugget = newInv.addItem(new ItemStack(Material.GOLD_NUGGET, nugget));
		if (unfitBlock.isEmpty() && unfitIngot.isEmpty() && unfitNugget.isEmpty())
			return true;
		else
			return false;
	}

	/**
	 * Attempts to remove a specific amount of gold (defined in nuggets) from a player's inventory.
	 * @param p The player whose inventory should be modified
	 * @param amount The amount of gold (in nuggets) to be removed the player's inventory.
	 * @return Whether or not the gold was successfully added (returns false if not enough gold is contained by the player's inventory)
	 */
	@SuppressWarnings("deprecation")
	public static boolean removeGoldFromPlayerInv(Player p, int amount){
		int total = getGoldInPlayerInv(p);
		if (total >= amount){
			Inventory inv = p.getInventory();
			int removedBlock = 0;
			int removedIngot = 0;
			int remaining = total;
			p.updateInventory();
			if (GoldBank.getAmountInInv(inv, Material.GOLD_BLOCK) >= (amount / 81) && amount / 81 >= 1){
				int remainingB = amount / 81;
				ItemStack[] contents = inv.getContents();
				for (ItemStack is : contents){
					if (is != null){
						if (is.getType() == Material.GOLD_BLOCK){
							if(is.getAmount() > remainingB){
								is.setAmount(is.getAmount() - remainingB);
								remainingB = 0;
							}
							else if(is.getAmount() <= remainingB){
								if (remainingB > 0){
									remainingB -= is.getAmount();
									is.setType(Material.AIR);
								}
							}
						}
					}
				}
				inv.setContents(contents);
				removedBlock = amount / 81;
				removedBlock = removedBlock * 81;
				remaining = total - removedBlock;
			}
			if (GoldBank.getAmountInInv(inv, Material.GOLD_INGOT) >= (amount / 9) && remaining >= 9){
				int remainingI = remaining;
				ItemStack[] contents = inv.getContents();
				for (ItemStack is : contents){
					if (is != null){
						if (is.getType() == Material.GOLD_BLOCK){
							if(is.getAmount() > remainingI){
								is.setAmount(is.getAmount() - remainingI);
								remainingI = 0;
							}
							else if(is.getAmount() <= remainingI){
								if (remainingI > 0){
									remainingI -= is.getAmount();
									is.setType(Material.AIR);
								}
							}
						}
					}
				}
				inv.setContents(contents);
				removedIngot = amount / 9;
				removedIngot = removedIngot * 9;
				remaining = remaining - removedIngot;
			}
			if (remaining != 0){
				if (GoldBank.getAmountInInv(inv, Material.GOLD_NUGGET) >= remaining){
					int remainingN = remaining;
					ItemStack[] contents = inv.getContents();
					for (ItemStack is : contents){
						if (is != null){
							if (is.getType() == Material.GOLD_BLOCK){
								if(is.getAmount() > remainingN){
									is.setAmount(is.getAmount() - remainingN);
									remainingN = 0;
								}
								else if(is.getAmount() <= remainingN){
									if (remainingN > 0){
										remainingN -= is.getAmount();
										is.setType(Material.AIR);
									}
								}
							}
						}
					}
					inv.setContents(contents);
				}
			}
			return true;
		}
		else
			return false;
	}
}
