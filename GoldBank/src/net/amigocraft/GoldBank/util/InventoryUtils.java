package net.amigocraft.GoldBank.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import net.amigocraft.GoldBank.GoldBank;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtils {

	public static GoldBank plugin = GoldBank.plugin;

	// method to fill the inventories
	public static void fill(){
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			Class.forName("org.sqlite.JDBC");
			String dbPath = "jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "chestdata.db";
			conn = DriverManager.getConnection(dbPath);
			st = conn.createStatement();
			rs = st.executeQuery("SELECT * FROM chestdata");
			while (rs.next()){
				String p = rs.getString("username");
				File invF = new File(plugin.getDataFolder() + File.separator + "inventories" + File.separator + p + ".inv");
				YamlConfiguration invY = new YamlConfiguration();
				invY.load(invF);
				int size = invY.getInt("size");
				Set<String> keys = invY.getKeys(false);
				ItemStack[] invI = new ItemStack[size];
				for (String invN : keys){
					if (!invN.equalsIgnoreCase("size")){
						int i = Integer.parseInt(invN);
						invI[i] =  invY.getItemStack(invN);
					}
				}
				Inventory inv = plugin.getServer().createInventory(null, size, p + "'s GoldBank Sign");
				inv.setContents(invI);
				if (inv.contains(Material.GOLD_BLOCK) || inv.contains(Material.GOLD_INGOT) || inv.contains(Material.GOLD_NUGGET)){
					int blocks = getAmountInInv(inv, Material.GOLD_BLOCK, -1);
					int ingots = getAmountInInv(inv, Material.GOLD_INGOT, -1);
					int nuggets = getAmountInInv(inv, Material.GOLD_NUGGET, -1);
					int totalblocks = (blocks * 81);
					int totalingots = (ingots * 9);
					double total = (double)(totalblocks + totalingots + nuggets);
					double rate = plugin.getConfig().getDouble("interest");
					double doubleinterest = (total * rate);
					int interest = (int)Math.round(doubleinterest);
					int newBlocks = interest / 81;
					int blockRemainder = interest - newBlocks * 81;
					int newIngots = blockRemainder / 9;
					int newNuggets = blockRemainder - newIngots * 9;
					ItemStack addBlocks = new ItemStack(Material.GOLD_BLOCK, newBlocks);
					ItemStack addIngots = new ItemStack(Material.GOLD_INGOT, newIngots);
					ItemStack addNuggets = new ItemStack(Material.GOLD_NUGGET, newNuggets);
					if (newBlocks != 0){
						inv.addItem(addBlocks);
					}
					if (newIngots != 0){
						inv.addItem(addIngots);
					}
					if (newNuggets != 0){
						inv.addItem(addNuggets);
					}
					invY.load(invF);
					for (int i = 0; i < inv.getSize(); i++){
						invY.set("" + i, inv.getItem(i));
					}
					invY.save(invF);
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		finally {
			try {
				conn.close();
				st.close();
				rs.close();
			}
			catch (Exception u){
				u.printStackTrace();
			}
		}
	}

	// check amount of item in inventory
	public static int getAmountInInv(Inventory inv, Material item, int damage){
		ItemStack[] contents = inv.getContents();
		int total = 0;
		for (ItemStack slot : contents){
			if (slot != null){
				if (slot.getType() == item && (slot.getDurability() == damage || damage < 0)){
					total = total + slot.getAmount();
				}
			}
		}
		return total;
	}

	public static int getAmountInInv(Inventory inv, Material item){
		return getAmountInInv(inv, item, -1);
	}

	// check empty slots in inventory
	public static int getNullsInInv(Inventory inv){
		ItemStack[] contents = inv.getContents();
		int total = 0;
		for (ItemStack slot : contents){
			if (slot == null){
				total = total + 1;
			}
		}
		return total;
	}

	public static void removeFromInv(Inventory inv, Material mat, int dmgValue, int amount){
		if(inv.contains(mat)){
			int remaining = amount;
			ItemStack[] contents = inv.getContents();
			for (ItemStack is : contents){
				if (is != null){
					if (is.getType() == mat){
						if (is.getDurability() == dmgValue || dmgValue <= 0){
							if(is.getAmount() > remaining){
								is.setAmount(is.getAmount() - remaining);
								remaining = 0;
							}
							else if(is.getAmount() <= remaining){
								if (remaining > 0){
									remaining -= is.getAmount();
									is.setType(Material.AIR);
								}
							}
						}
					}
				}
			}
			inv.setContents(contents);
		}
	}

	@SuppressWarnings("deprecation")
	public static void removeFromPlayerInv(Player p, Material mat, int dmgValue, int amount){
		removeFromInv(p.getInventory(), mat, dmgValue, amount);
		p.updateInventory();
	}
}