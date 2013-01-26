package net.amigocraft.GoldBank.api;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

import net.amigocraft.GoldBank.GoldBank;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BankInv {

	static GoldBank plugin = GoldBank.plugin;

	/**
	 * Attempts to get the amount of gold in a specified player's ban inventory.
	 * @param p The player who's bank inventory's gold should be counted.
	 * @return The amount of gold (in nuggets) contained by the specified player's bank inventory. Note: this method will return 0 if the player's bank inventory cannot be loaded.
	 */
	public static int getGoldInBankInv(String p){
		File invf = new File(GoldBank.plugin.getDataFolder() + File.separator + "inventories", p + ".inv");
		if(invf.exists()){
			YamlConfiguration invY = new YamlConfiguration();
			try {
				invY.load(invf);
				int size = invY.getInt("size");
				Set<String> keys = invY.getKeys(false);
				ItemStack[] invI = new ItemStack[size];
				for (String invN : keys){
					if (!invN.equalsIgnoreCase("size")){
						int i = Integer.parseInt(invN);
						invI[i] =  invY.getItemStack(invN);
					}
				}
				Inventory inv = plugin.getServer().createInventory(null, size);
				inv.setContents(invI);
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
			catch (Exception ex){
				ex.printStackTrace();
				return 0;
			}
		}
		else
			return 0;
	}

	/**
	 * Attempts to add a specific amount of gold (defined in nuggets) to a player's bank inventory.
	 * @param p The player whose bank inventory should be modified.
	 * @param amount The amount of gold (in nuggets) to be added to the player's bank inventory.
	 * @param simplify Whether or not the gold should be simplified (into blocks and ingots) before being distributed as nuggets
	 * @return Whether or not the gold was successfully added (returns false if not enough space is available or the player's bank inventory could not be loaded).
	 */
	public static boolean addGoldToBankInv(String p, int amount){
		File invf = new File(GoldBank.plugin.getDataFolder() + File.separator + "inventories", p + ".inv");
		if(invf.exists()){
			YamlConfiguration invY = new YamlConfiguration();
			try {
				invY.load(invf);
				int size = invY.getInt("size");
				Set<String> keys = invY.getKeys(false);
				ItemStack[] invI = new ItemStack[size];
				for (String invN : keys){
					if (!invN.equalsIgnoreCase("size")){
						int i = Integer.parseInt(invN);
						invI[i] =  invY.getItemStack(invN);
					}
				}
				Inventory inv = plugin.getServer().createInventory(null, size);
				inv.setContents(invI);
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
			catch (Exception ex){
				ex.printStackTrace();
				return false;
			}
		}
		else
			return false;
	}

	/**
	 * Attempts to remove a specific amount of gold (defined in nuggets) from a player's inventory.
	 * @param p The username of the player who's bank inventory should be modified.
	 * @param amount The amount of gold (in nuggets) to be removed the player's bank inventory.
	 * @return Whether or not the gold was successfully added (returns false if not enough gold is contained by the player's bank inventory)
	 */
	public static boolean removeGoldFromBankInv(String p, int amount){
		int total = getGoldInBankInv(p);
		if (total >= amount){
			File invf = new File(GoldBank.plugin.getDataFolder() + File.separator + "inventories", p + ".inv");
			if(invf.exists()){
				YamlConfiguration invY = new YamlConfiguration();
				try {
					invY.load(invf);
					int size = invY.getInt("size");
					Set<String> keys = invY.getKeys(false);
					ItemStack[] invI = new ItemStack[size];
					for (String invN : keys){
						if (!invN.equalsIgnoreCase("size")){
							int i = Integer.parseInt(invN);
							invI[i] =  invY.getItemStack(invN);
						}
					}
					Inventory inv = plugin.getServer().createInventory(null, size);
					inv.setContents(invI);
					int remaining = amount;
					// remove blocks
					int blocks = GoldBank.getAmountInInv(inv, Material.GOLD_BLOCK);
					if (blocks > 0 && remaining / 81 > 0){
						if (blocks >= remaining / 81){
							GoldBank.removeFromInv(inv, Material.GOLD_BLOCK, 0, remaining / 81);
							remaining -= (remaining / 81) * 81;
							if (GoldBank.getAmountInInv(inv, Material.GOLD_BLOCK) > 0 && remaining > 0){
								GoldBank.removeFromInv(inv, Material.GOLD_BLOCK, 0, 1);
								addGoldToBankInv(p, 81 - remaining);
								remaining = 0;
							}
						}
						else {
							GoldBank.removeFromInv(inv, Material.GOLD_BLOCK, 0, blocks);
							remaining -= blocks * 81;
						}
					}
					// remove ingots
					int ingots = GoldBank.getAmountInInv(inv, Material.GOLD_INGOT);
					if (ingots > 0 && remaining / 9 > 0){
						if (ingots >= remaining / 9){
							GoldBank.removeFromInv(inv, Material.GOLD_INGOT, 0, remaining / 9);
							if (GoldBank.getAmountInInv(inv, Material.GOLD_INGOT) > 0 && remaining > 0){
								GoldBank.removeFromInv(inv, Material.GOLD_INGOT, 0, 1);
								addGoldToBankInv(p, 9 - remaining);
								remaining = 0;
							}
						}
						else {
							GoldBank.removeFromInv(inv, Material.GOLD_INGOT, 0, ingots);
							remaining -= ingots * 9;
						}
					}
					// remove nuggets
					int nuggets = GoldBank.getAmountInInv(inv, Material.GOLD_NUGGET);
					if (nuggets > 0 && remaining > 0){
						if (nuggets > remaining){
							GoldBank.removeFromInv(inv, Material.GOLD_NUGGET, 0, remaining);
							remaining = 0;
						}
						else {
							// I don't think this is possible, but just in case ;)
							GoldBank.removeFromInv(inv, Material.GOLD_NUGGET, 0, nuggets);
							remaining -= nuggets;
						}
					}
					for (int i = 0; i < inv.getSize(); i++){
						invY.set(Integer.toString(i), inv.getItem(i));
					}
					return true;
				}
				catch (Exception ex){
					ex.printStackTrace();
					return false;
				}
			}
			else
				return false;
		}
		else
			return false;
	}
}
