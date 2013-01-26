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
		if (remaining / 81 >= 1){
			block = remaining / 81;
			remaining -= block * 81;
		}
		if (remaining >= 9){
			ingot = remaining / 9;
			remaining -= ingot * 9;
		}
		if (remaining != 0){
			nugget = remaining;
		}
		p.updateInventory();
		HashMap<Integer, ItemStack> unfitBlock = new HashMap<Integer, ItemStack>();
		HashMap<Integer, ItemStack> unfitIngot = new HashMap<Integer, ItemStack>();
		HashMap<Integer, ItemStack> unfitNugget = new HashMap<Integer, ItemStack>();
		if (block > 0)
			unfitBlock = newInv.addItem(new ItemStack(Material.GOLD_BLOCK, block));
		if (ingot > 0)
			unfitIngot = newInv.addItem(new ItemStack(Material.GOLD_INGOT, ingot));
		if (nugget > 0)
			unfitNugget = newInv.addItem(new ItemStack(Material.GOLD_NUGGET, nugget));
		if (unfitBlock.isEmpty() && unfitIngot.isEmpty() && unfitNugget.isEmpty()){
			if (block > 0)
				inv.addItem(new ItemStack(Material.GOLD_BLOCK, block));
			if (ingot > 0)
				inv.addItem(new ItemStack(Material.GOLD_INGOT, ingot));
			if (nugget > 0)
				inv.addItem(new ItemStack(Material.GOLD_NUGGET, nugget));
			return true;
		}
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
			int remaining = amount;
			// remove blocks
			int blocks = GoldBank.getAmountInInv(p.getInventory(), Material.GOLD_BLOCK);
			if (blocks > 0 && remaining / 81 > 0){
				if (blocks >= remaining / 81){
					GoldBank.removeFromPlayerInv(p, Material.GOLD_BLOCK, 0, remaining / 81);
					remaining -= (remaining / 81) * 81;
					if (GoldBank.getAmountInInv(p.getInventory(), Material.GOLD_BLOCK) > 0 && remaining > 0){
						GoldBank.removeFromPlayerInv(p, Material.GOLD_BLOCK, 0, 1);
						addGoldToPlayerInv(p, 81 - remaining);
						GoldBank.log.info("blocks");
						GoldBank.log.info(Integer.toString(remaining));
						remaining = 0;
					}
				}
				else {
					GoldBank.removeFromPlayerInv(p, Material.GOLD_BLOCK, 0, blocks);
					remaining -= blocks * 81;
				}
			}
			// remove ingots
			int ingots = GoldBank.getAmountInInv(p.getInventory(), Material.GOLD_INGOT);
			if (ingots > 0 && remaining / 9 > 0){
				if (ingots >= remaining / 9){
					GoldBank.removeFromPlayerInv(p, Material.GOLD_INGOT, 0, remaining / 9);
					remaining -= (remaining / 9) * 9;
					if (GoldBank.getAmountInInv(p.getInventory(), Material.GOLD_INGOT) > 0 && remaining > 0){
						GoldBank.removeFromPlayerInv(p, Material.GOLD_INGOT, 0, 1);
						addGoldToPlayerInv(p, 9 - remaining);
						GoldBank.log.info("ingots");
						GoldBank.log.info(Integer.toString(remaining));
						remaining = 0;
					}
				}
				else {
					GoldBank.removeFromPlayerInv(p, Material.GOLD_INGOT, 0, ingots);
					remaining -= ingots * 9;
				}
			}
			// remove nuggets
			int nuggets = GoldBank.getAmountInInv(p.getInventory(), Material.GOLD_NUGGET);
			if (nuggets > 0 && remaining > 0){
				if (nuggets > remaining){
					GoldBank.removeFromPlayerInv(p, Material.GOLD_NUGGET, 0, remaining);
					remaining = 0;
				}
				else {
					// I don't think this is possible, but just in case ;)
					GoldBank.removeFromPlayerInv(p, Material.GOLD_NUGGET, 0, nuggets);
					remaining -= nuggets;
				}
			}
			p.updateInventory();
			return true;
		}
		return false;
	}
}
