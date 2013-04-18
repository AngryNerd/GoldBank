package net.amigocraft.GoldBank.api;

import java.io.File;
import java.util.HashMap;

import net.amigocraft.GoldBank.GoldBank;
import net.amigocraft.GoldBank.util.InventoryUtils;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class WalletInv {

	static GoldBank plugin = GoldBank.plugin;

	/**
	 * Attempts to get the amount of gold in a specified player's given wallet inventory.
	 * @param p The player who's wallet's inventory should be searched.
	 * @param walletIndex The wallet number to search
	 * @return The amount of gold (in nuggets) contained by the specified player's given wallet inventory. Note: this method will return 0 if the player's given wallet inventory cannot be loaded.
	 */
	public static int getGoldInWalletInv(String p, int walletIndex){
		File invf = new File(InventoryUtils.plugin.getDataFolder() + File.separator + "wallets", p + ".inv");
		if(invf.exists()){
			YamlConfiguration invY = new YamlConfiguration();
			try {
				invY.load(invf);
				if (invY.isSet(Integer.toString(walletIndex))){
					int size = invY.getInt(walletIndex + ".size");
					ItemStack[] invI = new ItemStack[size];
					for (int i = 0; i < size; i++){
						invI[i] =  invY.getItemStack(walletIndex + "." + i);
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
				return 0;
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
	 * Attempts to add a specific amount of gold (defined in nuggets) to a player's given wallet inventory.
	 * @param p The player whose wallet inventory should be modified.
	 * @param walletIndex The wallet number to be modified.
	 * @param amount The amount of gold (in nuggets) to be added to the player's given wallet inventory.
	 * @return Whether or not the gold was successfully added (returns false if not enough space is available or the player's given wallet inventory could not be loaded).
	 */
	public static boolean addGoldToWalletInv(String p, int walletIndex, int amount){
		File invF = new File(InventoryUtils.plugin.getDataFolder() + File.separator + "inventories", p + ".inv");
		if(invF.exists()){
			YamlConfiguration invY = new YamlConfiguration();
			try {
				invY.load(invF);
				int size = invY.getInt("size");
				ItemStack[] invI = new ItemStack[size];
				for (int i = 0; i < size; i ++){
					invI[i] = invY.getItemStack(walletIndex + "." + i);
				}
				Inventory inv = plugin.getServer().createInventory(null, size);
				inv.setContents(invI);
				Inventory newInv = InventoryUtils.plugin.getServer().createInventory(null, inv.getSize());
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
	 * Attempts to remove a specific amount of gold (defined in nuggets) from a player's given wallet inventory.
	 * @param p The username of the player who's wallet inventory should be modified.
	 * @param walletIndex The wallet number to be modifed.
	 * @param amount The amount of gold (in nuggets) to be removed the player's given wallet inventory.
	 * @return Whether or not the gold was successfully added (returns false if not enough gold is contained by the player's given wallet inventory, or if the inventory cannot be loaded)
	 */
	public static boolean removeGoldFromWalletInv(String p, int walletIndex, int amount){
		int total = getGoldInWalletInv(p, walletIndex);
		if (total != -1){
			if (total >= amount){
				File invF = new File(InventoryUtils.plugin.getDataFolder() + File.separator + "wallets", p + ".inv");
				if(invF.exists()){
					YamlConfiguration invY = new YamlConfiguration();
					try {
						invY.load(invF);
						int size = invY.getInt(walletIndex + ".size");
						ItemStack[] invI = new ItemStack[size];
						for (int i = 0; i < size; i++){
							invI[i] =  invY.getItemStack(walletIndex + "." + i);
						}
						Inventory inv = plugin.getServer().createInventory(null, size);
						inv.setContents(invI);
						int remaining = amount;
						// remove blocks
						int blocks = InventoryUtils.getAmountInInv(inv, Material.GOLD_BLOCK);
						if (blocks > 0 && remaining / 81 > 0){
							if (blocks >= remaining / 81){
								InventoryUtils.removeFromInv(inv, Material.GOLD_BLOCK, 0, remaining / 81);
								remaining -= (remaining / 81) * 81;
								if (InventoryUtils.getAmountInInv(inv, Material.GOLD_BLOCK) > 0 && remaining > 0){
									InventoryUtils.removeFromInv(inv, Material.GOLD_BLOCK, 0, 1);
									addGoldToWalletInv(p, walletIndex, 81 - remaining);
									remaining = 0;
								}
							}
							else {
								InventoryUtils.removeFromInv(inv, Material.GOLD_BLOCK, 0, blocks);
								remaining -= blocks * 81;
							}
						}
						// remove ingots
						int ingots = InventoryUtils.getAmountInInv(inv, Material.GOLD_INGOT);
						if (ingots > 0 && remaining / 9 > 0){
							if (ingots >= remaining / 9){
								InventoryUtils.removeFromInv(inv, Material.GOLD_INGOT, 0, remaining / 9);
								if (InventoryUtils.getAmountInInv(inv, Material.GOLD_INGOT) > 0 && remaining > 0){
									InventoryUtils.removeFromInv(inv, Material.GOLD_INGOT, 0, 1);
									addGoldToWalletInv(p, walletIndex, 9 - remaining);
									remaining = 0;
								}
							}
							else {
								InventoryUtils.removeFromInv(inv, Material.GOLD_INGOT, 0, ingots);
								remaining -= ingots * 9;
							}
						}
						// remove nuggets
						int nuggets = InventoryUtils.getAmountInInv(inv, Material.GOLD_NUGGET);
						if (nuggets > 0 && remaining > 0){
							if (nuggets > remaining){
								InventoryUtils.removeFromInv(inv, Material.GOLD_NUGGET, 0, remaining);
								remaining = 0;
							}
							else {
								// I don't think this is possible, but just in case ;)
								InventoryUtils.removeFromInv(inv, Material.GOLD_NUGGET, 0, nuggets);
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
		else
			return false;
	}
}
