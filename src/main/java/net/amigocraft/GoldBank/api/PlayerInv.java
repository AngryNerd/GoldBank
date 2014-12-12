/*
 * GoldBank
 * Copyright (c) 2014, Maxim Roncacé <http://bitbucket.org/mproncace>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.amigocraft.GoldBank.api;

import java.util.HashMap;

import net.amigocraft.GoldBank.util.InventoryUtils;

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
	 * @return Whether or not the gold was successfully added (returns false if not enough space is available)
	 */
	@SuppressWarnings("deprecation")
	public static boolean addGoldToPlayerInv(Player p, int amount){
		Inventory inv = p.getInventory();
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
			int blocks = InventoryUtils.getAmountInInv(p.getInventory(), Material.GOLD_BLOCK);
			if (blocks > 0 && remaining / 81 > 0){
				if (blocks >= remaining / 81){
					InventoryUtils.removeFromPlayerInv(p, Material.GOLD_BLOCK, 0, remaining / 81);
					remaining -= (remaining / 81) * 81;
					if (InventoryUtils.getAmountInInv(p.getInventory(), Material.GOLD_BLOCK) > 0 && remaining > 0){
						InventoryUtils.removeFromPlayerInv(p, Material.GOLD_BLOCK, 0, 1);
						addGoldToPlayerInv(p, 81 - remaining);
						remaining = 0;
					}
				}
				else {
					InventoryUtils.removeFromPlayerInv(p, Material.GOLD_BLOCK, 0, blocks);
					remaining -= blocks * 81;
				}
			}
			// remove ingots
			int ingots = InventoryUtils.getAmountInInv(p.getInventory(), Material.GOLD_INGOT);
			if (ingots > 0 && remaining / 9 > 0){
				if (ingots >= remaining / 9){
					InventoryUtils.removeFromPlayerInv(p, Material.GOLD_INGOT, 0, remaining / 9);
					remaining -= (remaining / 9) * 9;
					if (InventoryUtils.getAmountInInv(p.getInventory(), Material.GOLD_INGOT) > 0 && remaining > 0){
						InventoryUtils.removeFromPlayerInv(p, Material.GOLD_INGOT, 0, 1);
						addGoldToPlayerInv(p, 9 - remaining);
						remaining = 0;
					}
				}
				else {
					InventoryUtils.removeFromPlayerInv(p, Material.GOLD_INGOT, 0, ingots);
					remaining -= ingots * 9;
				}
			}
			// remove nuggets
			int nuggets = InventoryUtils.getAmountInInv(p.getInventory(), Material.GOLD_NUGGET);
			if (nuggets > 0 && remaining > 0){
				if (nuggets > remaining){
					InventoryUtils.removeFromPlayerInv(p, Material.GOLD_NUGGET, 0, remaining);
					remaining = 0;
				}
				else {
					// I don't think this is possible, but just in case ;)
					InventoryUtils.removeFromPlayerInv(p, Material.GOLD_NUGGET, 0, nuggets);
					remaining -= nuggets;
				}
			}
			p.updateInventory();
			return true;
		}
		return false;
	}
}
