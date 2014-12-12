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
package net.amigocraft.goldbank.bukkit;

import static net.amigocraft.goldbank.bukkit.util.MiscUtils.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import net.amigocraft.goldbank.bukkit.api.BankInv;
import net.amigocraft.goldbank.bukkit.economy.VaultConnector;
import net.amigocraft.goldbank.bukkit.util.InventoryUtils;
import net.amigocraft.goldbank.bukkit.util.UUIDFetcher;
import net.milkbowl.vault.economy.Economy;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

/**
 * To whomever may be reviewing this plugin: I am so, so sorry.
 */
public class GoldBank extends JavaPlugin implements Listener {
	public static GoldBank plugin;
	public Logger log;
	public static String ANSI_RED = "\u001B[31m";
	public static String ANSI_GREEN = "\u001B[32m";
	public static String ANSI_WHITE = "\u001B[37m";
	private UUID[] openPlayer = new UUID[256];
	private UUID[] openingPlayer = new UUID[256];
	private String[] openType = new String[256];
	private int[] openWalletNo = new int[256];
	private int nextIndex = 0;
	public HashMap<String, Integer> shopLog = new HashMap<String, Integer>();
	public String header = "########################## #\n# GoldBank Configuration # #\n########################## #";
	public static boolean UUID_SUPPORT = true;

	public static HashMap<String, UUID> onlineUUIDs = new HashMap<String, UUID>();

	@SuppressWarnings("unchecked")
	@Override
	public void onEnable(){

		log = this.getLogger();

		if (!Bukkit.getOnlineMode())
			log.warning("Server is running in offline mode! Without proper authentication, GoldBank may not work correctly " +
					"due to Minecraft's UUID system.");

		try {
			Bukkit.getOfflinePlayer(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"));
		}
		catch (NoSuchMethodError ex){
			UUID_SUPPORT = false;
			log.info("Native UUID support not detected. Falling back to online API...");
		}

		// autoupdate
		if (getConfig().getBoolean("enable-auto-update")){
			try {new Updater(this, 47431, this.getFile(), Updater.UpdateType.DEFAULT, true);}
			catch (Exception e){e.printStackTrace();}
		}

		// submit metrics
		if (getConfig().getBoolean("enable-metrics")){
			try {
				Metrics metrics = new Metrics(this);
				metrics.start();
			}
			catch (IOException e) {log.warning(ANSI_RED + "Failed to submit statistics to Plugin Metrics" + ANSI_WHITE);}
		}

		// register events and the plugin variable
		getServer().getPluginManager().registerEvents(this, this);
		GoldBank.plugin = this;

		// register economy with Vault
		if (getServer().getPluginManager().getPlugin("Vault") != null){
			final ServicesManager sm = getServer().getServicesManager();
			sm.register(Economy.class, new VaultConnector(), this, ServicePriority.Highest);
			log.info(ANSI_GREEN + "Registered Vault interface." + ANSI_WHITE);
		}
		else {
			log.info(ANSI_RED + "Vault not found. Other plugins may not be able to access GoldBank accounts." + ANSI_WHITE);
		}

		// initialize wallet arrays

		for (int i = 0; i < 256; i++){
			openPlayer[i] = null;
			openingPlayer[i] = null;
			openType[i] = null;
			openWalletNo[i] = -1;
		}

		// add the crafting recipe for wallets
		ItemStack is = new ItemStack(Material.BOOK, 1);
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName("§2Wallet");
		is.setItemMeta(meta);
		final ShapedRecipe walletRecipe1 = new ShapedRecipe(is);
		walletRecipe1.shape("XXX", "LXL", "LLL");
		walletRecipe1.setIngredient('L', Material.LEATHER);
		getServer().addRecipe(walletRecipe1);
		final ShapedRecipe walletRecipe2 = new ShapedRecipe(is);
		walletRecipe2.shape("LXL", "LLL", "XXX");
		walletRecipe2.setIngredient('L', Material.LEATHER);
		getServer().addRecipe(walletRecipe2);

		// create the data folders
		this.getDataFolder().mkdir();
		File invDir = new File(this.getDataFolder() + File.separator + "inventories");
		invDir.mkdir();
		File walletDir = new File(this.getDataFolder() + File.separator + "wallets");
		walletDir.mkdir();

		// check config values
		ConfigCheck.check();

		// create the variable storage file 
		File file = new File(getDataFolder(), "filled.txt");
		if (!(file.exists())){
			try {
				file.createNewFile();
				PrintWriter pw = new PrintWriter(file);
				pw.print("0");
				pw.close();
			}
			catch (IOException e){
				e.printStackTrace();
			}
		}

		// create the plugin table if it does not exist and update older tables to take several updates into account
		if (new File(getDataFolder(), "chestdata.db").exists())
			new File(getDataFolder(), "chestdata.db").renameTo(new File(getDataFolder(), "data.db"));	
		Connection conn = null;
		Statement st = null;
		Statement st2 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		try {
			Class.forName("org.sqlite.JDBC");
			String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
			conn = DriverManager.getConnection(dbPath);
			st = conn.createStatement();
			st2 = conn.createStatement();
			boolean copyTable = false;
			DatabaseMetaData md = conn.getMetaData();
			rs = md.getTables(null, null, "%", null);
			while (rs.next()){
				if (rs.getString(3).equals("chestdata")){ // it's a pre-UUID table
					copyTable = true;
					break;
				}
			}
			if (copyTable){
				log.warning("Detected old database tables!");
				log.info("In Minecraft 1.7.6 and above, Mojang completely transitioned from using usernames to UUIDs. This "
						+ "makes username data in GoldBank's database unreliable. Therefore, it must be converted before it "
						+ "can be used with the plugin.");
				log.info("A copy of the original database will be made in data.old.db. Use this in case something goes " +
						"wrong, or if you need to downgrade to an earlier version of the plugin.");
				log.info("Depending on how much data GoldBank has stored on this server, this process may take a while. "
						+ "Feel free to make a pot of tea while it runs.");

				Files.copy(new File(getDataFolder(), "data.db"), new File(getDataFolder(), "data.old.db"));

				st.executeUpdate("CREATE TABLE IF NOT EXISTS banks (" +
						"id INTEGER NOT NULL PRIMARY KEY," +
						"uuid VARCHAR(36) NOT NULL," +
						"world VARCHAR(100) NOT NULL," +
						"x INTEGER NOT NULL," +
						"y INTEGER NOT NULL," +
						"z INTEGER NOT NULL," +
						"sign BOOLEAN NOT NULL," +
						"tier INTEGER NOT NULL)");
				HashMap<String, String> uuids = new HashMap<String, String>();
				rs2 = st2.executeQuery("SELECT COUNT(*) FROM chestdata");
				int total = 0;
				while (rs2.next()){
					total = rs2.getInt(1);
				}
				log.info("Discovered " + total + " rows in table \"chestdata\"");
				rs2 = st2.executeQuery("SELECT * FROM chestdata");
				int i = 0;
				int messages = 0;
				while (rs2.next()){
					String username = rs2.getString("username");
					String uuid;
					if (uuids.containsKey(username))
						uuid = uuids.get(username);
					else {
						uuid = getSafeUUID(rs2.getString("username")).toString();
						uuids.put(username, uuid);
					}
					st.executeUpdate("INSERT INTO banks (uuid, world, x, y, z, sign, tier) VALUES ('" +
							getSafeUUID(rs2.getString("username")) + "', '" +
							rs2.getString("world") + "', '" +
							rs2.getInt("x") + "', '" +
							rs2.getInt("y") + "', '" +
							rs2.getInt("z") + "', '" +
							"true', '" +
							rs2.getString("tier") + "')");
					if (i > (total / 10f) * (messages + 1)){
						if (total == 0)
							messages = 10;
						else
							messages = (int)Math.floor(i / (total / 10f));
						log.info(messages * 10 + "% converted (" + i + " records processed)");
					}
					i += 1;
				}
				rs2.close();
				st.executeUpdate("ALTER TABLE shops RENAME TO shops_old");
				st.executeUpdate("CREATE TABLE IF NOT EXISTS shops (" +
						"id INTEGER NOT NULL PRIMARY KEY," +
						"creator VARCHAR(36) NOT NULL," +
						"world VARCHAR(100) NOT NULL," +
						"x INTEGER NOT NULL," +
						"y INTEGER NOT NULL," +
						"z INTEGER NOT NULL," +
						"material INTEGER," +
						"data INTEGER NOT NULL," +
						"buyamount INTEGER NOT NULL," +
						"buyprice INTEGER NOT NULL," +
						"sellamount INTEGER NOT NULL," +
						"sellprice INTEGER NOT NULL," +
						"buyunit VARCHAR(1) NOT NULL," +
						"sellunit VARCHAR(1) NOT NULL," +
						"admin BOOLEAN NOT NULL)");
				st.executeUpdate("INSERT INTO shops (id, creator, world, x, y, z, material, data, buyamount, buyprice, " +
						"sellamount, sellprice, buyunit, sellunit, admin) SELECT id, creator, world, x, y, z, material, " +
						"data, buyamount, buyprice, sellamount, sellprice, buyunit, sellunit, admin FROM shops_old");
				rs2 = st2.executeQuery("SELECT COUNT(*) FROM shops");
				while (rs2.next()){
					total = rs2.getInt(1);
				}
				log.info("Discovered " + total + " rows in table \"shops\"");
				rs2 = st2.executeQuery("SELECT * FROM shops");
				i = 0;
				messages = 0;
				while (rs2.next()){
					String username = rs2.getString("creator");
					String uuid;
					if (uuids.containsKey(username))
						uuid = uuids.get(username);
					else {
						uuid = getSafeUUID(username).toString();
						uuids.put(username, uuid);
					}
					st.executeUpdate("UPDATE shops SET creator = '" + getSafeUUID(rs2.getString("creator")) +
							"' WHERE id = '" + rs2.getInt("id") + "'");
					if (i > (total / 10f) * (messages + 1)){
						if (total == 0)
							messages = 10;
						else
							messages = (int)Math.floor(i / (total / 10f));
						log.info(messages * 10 + "% converted (" + i + " records processed)");
					}
					i += 1;
				}
				rs2.close();
				st.executeUpdate("ALTER TABLE shoplog RENAME TO shoplog_old");
				st.executeUpdate("CREATE TABLE shoplog (" +
						"id INTEGER NOT NULL PRIMARY KEY," +
						"shop INTEGER NOT NULL," +
						"player VARCHAR(36) NOT NULL," +
						"action INTEGER NOT NULL," +
						"material INTEGER," +
						"data INTEGER," +
						"quantity INTEGER," +
						"time INTEGER)");
				st.executeUpdate("INSERT INTO shoplog (id, shop, player, action, material, data, quantity, time)" +
						"SELECT id, shop, player, action, material, data, quantity, time FROM shoplog_old");
				rs2 = st2.executeQuery("SELECT COUNT(*) FROM shoplog_old");
				while (rs2.next()){
					total = rs2.getInt(1);
				}
				log.info("Discovered " + total + " rows in table \"shoplog\"");
				rs2 = st2.executeQuery("SELECT * FROM shoplog");
				i = 0;
				messages = 0;
				while (rs2.next()){
					String username = rs2.getString("player");
					String uuid;
					if (uuids.containsKey(username))
						uuid = uuids.get(username);
					else {
						uuid = getSafeUUID(username).toString();
						uuids.put(username, uuid);
					}
					st.executeUpdate("UPDATE shoplog SET player = '" + getSafeUUID(rs2.getString("player")) +
							"' WHERE id = '" + rs2.getInt("id") + "'");
					if (i > (total / 10f) * (messages + 1)){
						if (total == 0)
							messages = 10;
						else
							messages = (int)Math.floor(i / (total / 10f));
						log.info(messages * 10 + "% converted (" + i + " records processed)");
					}
					i += 1;
				}
				rs2.close();
				st2.close();
				conn.close();
				Class.forName("org.sqlite.JDBC");
				conn = DriverManager.getConnection(dbPath);
				st = conn.createStatement();
				st.executeUpdate("DROP TABLE chestdata");
				st.executeUpdate("DROP TABLE shops_old");
				st.executeUpdate("DROP TABLE shoplog_old");
				log.info("Finished converting tables! :)");
				log.info("Now we need to rename the data files. This shouldn't take very long. How was your tea, by the way?");
				for (File f : new File(getDataFolder(), "inventories").listFiles()){
					String username = f.getName().split("\\.")[0];
					String uuid;
					if (uuids.containsKey(username))
						uuid = uuids.get(username);
					else {
						uuid = getSafeUUID(username).toString();
						uuids.put(username, uuid);
					}
					YamlConfiguration y = new YamlConfiguration();
					y.load(f);
					y.set("username", username);
					y.save(f);
					y = null;
					f.renameTo(new File(getDataFolder() + File.separator + "inventories", uuid + ".dat"));
				}
				for (File f : walletDir.listFiles()){
					String username = f.getName().split("\\.")[0];
					String uuid;
					if (uuids.containsKey(username))
						uuid = uuids.get(username);
					else {
						uuid = getSafeUUID(username).toString();
						uuids.put(username, uuid);
					}
					YamlConfiguration y = new YamlConfiguration();
					y.load(f);
					y.set("username", username);
					y.save(f);
					y = null;
					f.renameTo(new File(getDataFolder() + File.separator + "wallets", uuid + ".dat"));
				}
				uuids.clear();
				log.info("Thanks for your patience. We've converted all data to the new format, so you should be good to go. :)");
			}
			else
				st.executeUpdate("CREATE TABLE IF NOT EXISTS banks (" +
						"id INTEGER NOT NULL PRIMARY KEY," +
						"uuid VARCHAR(36) NOT NULL," +
						"world VARCHAR(100) NOT NULL," +
						"x INTEGER NOT NULL," +
						"y INTEGER NOT NULL," +
						"z INTEGER NOT NULL," +
						"sign BOOLEAN NOT NULL," +
						"tier INTEGER NOT NULL)");
			try {
				st.executeUpdate("ALTER TABLE banks ADD sign BOOLEAN DEFAULT 'false' NOT NULL");
				st.executeUpdate("UPDATE banks SET y='y+1', sign='true'");
			}
			catch (Exception ex){}
			try {
				st.executeUpdate("ALTER TABLE banks ADD tier BOOLEAN DEFAULT '1' NOT NULL");
			}
			catch (Exception ex){}
			try {
				String world = getServer().getWorlds().get(0).getName();
				st.executeUpdate("ALTER TABLE banks ADD world VARCHAR(100) DEFAULT 'world' NOT NULL");
				st.executeUpdate("UPDATE banks SET world = '" + world + "'");
			}
			catch (Exception ex){}
			st.executeUpdate("CREATE TABLE IF NOT EXISTS shops (" +
					"id INTEGER NOT NULL PRIMARY KEY," +
					"creator VARCHAR(36) NOT NULL," +
					"world VARCHAR(100) NOT NULL," +
					"x INTEGER NOT NULL," +
					"y INTEGER NOT NULL," +
					"z INTEGER NOT NULL," +
					"material INTEGER," +
					"data INTEGER NOT NULL," +
					"buyamount INTEGER NOT NULL," +
					"buyprice INTEGER NOT NULL," +
					"sellamount INTEGER NOT NULL," +
					"sellprice INTEGER NOT NULL," +
					"buyunit VARCHAR(1) NOT NULL," +
					"sellunit VARCHAR(1) NOT NULL," +
					"admin BOOLEAN NOT NULL)");
			st.executeUpdate("DROP TABLE IF EXISTS nbt");
			try {
				String world = getServer().getWorlds().get(0).getName();
				st.executeUpdate("ALTER TABLE shops ADD world VARCHAR(100) DEFAULT 'world' NOT NULL");
				st.executeUpdate("UPDATE shops SET world = '" + world + "'");
			}
			catch (Exception ex){}
			try {
				st.executeUpdate("ALTER TABLE shops ADD buyunit VARCHAR(1) DEFAULT 'i' NOT NULL");
			}
			catch (Exception ex){}
			try {
				st.executeUpdate("ALTER TABLE shops ADD sellunit VARCHAR(1) DEFAULT 'i' NOT NULL");
			}
			catch (Exception ex){}
			st.executeUpdate("CREATE TABLE IF NOT EXISTS shoplog (" +
					"id INTEGER NOT NULL PRIMARY KEY," +
					"shop INTEGER NOT NULL," +
					"player VARCHAR(36) NOT NULL," +
					"action INTEGER NOT NULL," +
					"material INTEGER," +
					"data INTEGER," +
					"quantity INTEGER," +
					"time INTEGER)");
		}
		catch (Exception e){
			e.printStackTrace();
		}
		finally {
			try {
				rs.close();
				st.close();
				conn.close();
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}

		List<Player> players = new ArrayList<Player>();
		try {
			if (Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).getReturnType() == Collection.class)
				players.addAll(((Collection<? extends Player>)Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0])));
			else
				players.addAll(Arrays.asList((Player[])Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0])));
		}
		catch (NoSuchMethodException ex){} // can never happen
		catch (InvocationTargetException ex){} // can also never happen
		catch (IllegalAccessException ex){} // can still never happen

		List<String> names = new ArrayList<String>();
		for (Player p : players)
			names.add(p.getName());
		try {
			onlineUUIDs.putAll(new UUIDFetcher(names).call());
		}
		catch (Exception e){
			e.printStackTrace();
		}

		log.info(ANSI_GREEN + this + " has been enabled!" + ANSI_WHITE);
	}
	public void onDisable(){
		log.info(ANSI_GREEN + ANSI_WHITE + "Please wait, purging variables...");
		ANSI_RED = null;
		plugin = null;
		boolean first = true;
		for (int i = 0; i < openingPlayer.length; i++){
			if (openType[i] != null){
				Player p = getSafePlayer(openingPlayer[i]);
				if (p != null){
					p.closeInventory();
					p.sendMessage(ChatColor.RED + WordUtils.capitalize(openType[i]) + " automatically closed by reload");
				}
				openType[i] = null;
				openingPlayer[i] = null;
				openPlayer[i] = null;
				openWalletNo = null;
				if (first){
					if (nextIndex > i)
						nextIndex = i;
					first = false;
				}
			}
		}
		log.info(ANSI_GREEN + this + " has been disabled!" + ANSI_WHITE);
		ANSI_GREEN = null;
		ANSI_WHITE = null;
	}

	// initiate function for detecting player clicking sign
	@SuppressWarnings({"deprecation"})
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClick(PlayerInteractEvent e){
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR){
			// check if wallet is in hand
			if (e.getPlayer().getItemInHand().getType() == Material.BOOK){
				ItemStack is = e.getPlayer().getItemInHand();
				ItemMeta meta = is.getItemMeta();
				if (!(meta.getDisplayName() == null) && meta.getLore() != null){
					if (meta.getLore().size() >= 4){
						if (meta.getDisplayName().equals("§2Wallet") && meta.getLore().get(3).equals("§2GoldBank")){
							// cancel the event because the item in hand is a wallet
							e.setCancelled(true);
							boolean own = false;
							if (meta.getLore().get(1).equals(e.getPlayer().getName()))
								own = true;
							String node = "goldbank.wallet.open";
							if (own)
								node = "goldbank.wallet.open.own";
							if (e.getPlayer().hasPermission(node)){
								String owner = meta.getLore().get(1);
								String numLine = meta.getLore().get(2);
								char[] chars = numLine.toCharArray();
								int length = numLine.length();
								String numStr = "";
								for (int i = 10; i < length; i++){
									numStr = numStr + Character.toString(chars[i]);
								}
								int num = Integer.parseInt(numStr);
								UUID ownerUUID = getSafeUUID(owner);
								File invF = new File(getDataFolder() + File.separator + "wallets", ownerUUID + ".dat");
								if(invF.exists()){
									YamlConfiguration invY = new YamlConfiguration();
									try {
										invY.load(invF);
										if (invY.isSet(Integer.toString(num))){
											ItemStack[] invI = new ItemStack[this.getConfig().getInt("walletsize")];
											for (int i = 0; i < invI.length; i++){
												String key = Integer.toString(num) + "." + i;
												invI[i] =  invY.getItemStack(key);
											}
											Inventory inv = this.getServer().createInventory(null, this.getConfig().getInt("walletsize"),
													owner + "'s Wallet - #" + numStr);
											inv.setContents(invI);
											e.getPlayer().openInventory(inv);
											openPlayer[nextIndex] = ownerUUID;
											openingPlayer[nextIndex] = getSafeUUID(e.getPlayer().getName());
											openType[nextIndex] = "wallet";
											openWalletNo[nextIndex] = num;
											nextIndex += 1;
										}
										else {
											e.getPlayer().sendMessage(ChatColor.RED + "Error: This wallet does not have an associated YAML configuration section. Attempting to create one...");
											try {
												invY.set(num + ".size", this.getConfig().getInt("walletsize"));
												invY.save(invF);
												e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "Success!");
											}
											catch (Exception ex){ex.printStackTrace();}
										}
									}
									catch (Exception ex){
										ex.printStackTrace();
										e.getPlayer().sendMessage(ChatColor.RED + "An error occurred while attempting to open this wallet.");
									}
								}
								else {
									e.getPlayer().sendMessage(ChatColor.RED + "Error: This wallet does not have an associated YAML file. Attempting to create one...");
									try {
										invF.createNewFile();
										YamlConfiguration invY = new YamlConfiguration();
										invY.load(invF);
										invY.set(num + ".size", this.getConfig().getInt("walletsize"));
										invY.save(invF);
										e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "Success!");
									}
									catch (Exception exc){
										exc.printStackTrace();
										String cookieMsg = "";
										if (this.getConfig().getBoolean("give-cookie-when-wallet-creation-fails")){
											cookieMsg = " Here's a cookie to make up for it. :)";
											e.getPlayer().getInventory().addItem(new ItemStack(Material.COOKIE, 1));
										}
										try {
											invF.createNewFile();
										}
										catch (Exception ex){
											ex.printStackTrace();
											e.getPlayer().sendMessage(ChatColor.RED + "An error occurred while attempting to add this wallet to the YAML configuration." + cookieMsg);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_BLOCK){
			// check if player is checking shop
			if (shopLog.containsKey(e.getPlayer().getName())){
				if (shopLog.get(e.getPlayer().getName()) <= 0){
					e.setCancelled(true);
					shopLog.remove(e.getPlayer().getName());
					if (e.getClickedBlock().getState() instanceof Sign){
						Connection conn = null;
						Statement st = null;
						ResultSet rs = null;
						try {
							Class.forName("org.sqlite.JDBC");
							String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
							conn = DriverManager.getConnection(dbPath);
							st = conn.createStatement();
							String world = e.getClickedBlock().getWorld().getName();
							int x = e.getClickedBlock().getX();
							int y = e.getClickedBlock().getY();
							int z = e.getClickedBlock().getZ();
							rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE world = '" + world + "' AND x = '" + x + "' AND y = '" + y + "' AND z = '" + z + "'");
							int count = 0;
							while (rs.next()){
								count = rs.getInt(1);
							}
							if (count != 0){
								rs = st.executeQuery("SELECT * FROM shops WHERE world = '" + world + "' AND x = '" + x + "' AND y = '" + y + "' AND z = '" + z + "'");
								int shopId = rs.getInt("id");
								shopLog.put(e.getPlayer().getName(), shopId);
								rs = st.executeQuery("SELECT COUNT(*) FROM shoplog WHERE shop = '" + shopId + "' AND action < '2'");
								int total = 0;
								while (rs.next()){
									total = rs.getInt(1);
								}
								if (total != 0){
									int perPage = 10;
									int pages = total / perPage;
									if (pages * perPage != total)
										pages += 1;
									e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "Page 1/" + pages);
									rs = st.executeQuery("SELECT * FROM shoplog WHERE shop = '" + shopId + "' AND action < '2' ORDER BY id DESC");
									for (int i = 1; i <= perPage; i++){
										if (i <= total){
											String action = "";
											ChatColor actionColor = ChatColor.DARK_GREEN;
											if (rs.getInt("action") == 0)
												action = "bought";
											else if (rs.getInt("action") == 1){
												action = "sold";
												actionColor = ChatColor.DARK_RED;
											}
											String data = "";
											if (rs.getInt("data") > 0)
												data = ":" + rs.getInt("data");
											Calendar cal = Calendar.getInstance();
											cal.setTimeInMillis((long)rs.getInt("time") * 1000);
											String month = Integer.toString(cal.get(Calendar.MONTH) + 1);
											String day = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
											String hour = Integer.toString(cal.get(Calendar.HOUR_OF_DAY));
											String min = Integer.toString(cal.get(Calendar.MINUTE));
											String sec = Integer.toString(cal.get(Calendar.SECOND));
											if (month.length() < 2)
												month = "0" + month;
											if (day.length() < 2)
												day = "0" + day;
											while (hour.length() < 2)
												hour = "0" + hour;
											while (min.length() < 2)
												min = "0" + min;
											while (sec.length() < 2)
												sec = "0" + sec;
											String dateStr = cal.get(Calendar.YEAR) + "-" + month + "-" + day + " " + hour + ":" + min + ":" + sec;
											//TODO: Phase out "magic numbers"
											e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + Integer.toString(i) + ") " +
													ChatColor.DARK_AQUA + dateStr + " " + ChatColor.LIGHT_PURPLE +
													getSafePlayerName(UUID.fromString(rs.getString("player"))) + " " +
													actionColor + action + " " + ChatColor.GOLD + rs.getInt("quantity") + " " +
													Material.getMaterial(rs.getInt("material")).toString() + data);
											rs.next();
										}
										else
											break;
									}
									if (pages > 1)
										e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "Type " + ChatColor.DARK_GREEN + "/gb shop log page 2 " + ChatColor.DARK_PURPLE + "to view the next page");
								}
								else
									e.getPlayer().sendMessage(ChatColor.RED + "Error: The selected shop does not have any logged transactions!");
							}
							else {
								e.getPlayer().sendMessage(ChatColor.RED + "Selected block is not a GoldShop! Operation aborted.");
							}
						}
						catch (Exception ex){
							ex.printStackTrace();
						}
						finally {
							try {
								rs.close();
								st.close();
								conn.close();
							}
							catch (Exception exc){
								exc.printStackTrace();
							}
						}
					}
					else {
						e.getPlayer().sendMessage(ChatColor.RED + "Selected block is not a GoldShop! Operation aborted.");
					}
				}
			}
		}
	}

	@SuppressWarnings({"deprecation"})
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onOneClick(PlayerInteractEvent e){
		// this code is here to prevent bugs when clicking a bank sign with a wallet
		boolean wallet = false;
		if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR){
			if (e.getPlayer().getItemInHand().getType() == Material.BOOK){
				ItemStack is = e.getPlayer().getItemInHand();
				ItemMeta meta = is.getItemMeta();
				if (!(meta.getDisplayName() == null) && meta.getLore() != null){
					if (meta.getLore().size() >= 4){
						if (meta.getDisplayName().equals("§2Wallet") && meta.getLore().get(3).equals("§2GoldBank")){
							// cancel the event because the item in hand is a wallet
							e.setCancelled(true);
							wallet = true;
						}
					}
				}
			}
		}
		// check for right click
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK){
			if (!wallet){
				// check if clicked block is sign
				if (e.getClickedBlock() != null){
					if (e.getClickedBlock().getType() == Material.WALL_SIGN || e.getClickedBlock().getType() == Material.SIGN_POST){
						Player player = e.getPlayer();
						String p = player.getName();
						UUID pUUID = getSafeUUID(p);
						Sign sign = (Sign) e.getClickedBlock().getState();
						String fline = sign.getLine(0);
						if (fline.equalsIgnoreCase("§2[GoldBank]")){
							e.setCancelled(true);
							if (player.hasPermission("goldbank.sign.bank.use")){
								Connection conn = null;
								ResultSet rs = null;
								Statement st = null;
								try {
									Class.forName("org.sqlite.JDBC");
									String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
									conn = DriverManager.getConnection(dbPath);
									st = conn.createStatement();
									String checkWorld = e.getClickedBlock().getWorld().getName();
									int checkX = e.getClickedBlock().getX();
									int checkY = e.getClickedBlock().getY();
									int checkZ = e.getClickedBlock().getZ();
									rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE world = '" + checkWorld + "' AND x = '" + checkX + "' AND y = '" + checkY + "' AND z = '" + checkZ + "'");
									int regcount = 0;
									while (rs.next()){
										regcount = rs.getInt(1);
									}
									boolean master = false;
									rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE world = '" + checkWorld + "' AND x = '" + checkX + "' AND y = '" + checkY + "' AND z = '" + checkZ + "' AND uuid = 'MASTER'");
									int masterCount = 0;
									while (rs.next()){
										masterCount = rs.getInt(1);
									}
									if (masterCount != 0)
										master = true;
									rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE uuid = '" + pUUID + "'");
									int fpcount = 0;
									while (rs.next()){
										fpcount = rs.getInt(1);
									}
									if (regcount == 0 || (master && fpcount == 0)){
										if (fpcount == 0){
											int tier = 1;
											if (sign.getLine(1).length() >= 6){
												if (sign.getLine(1).substring(0, 6).equalsIgnoreCase("§4Tier")){
													if (isInt(sign.getLine(1).substring(7, 8))){
														if (getConfig().isSet("tiers." + Integer.parseInt(sign.getLine(1).substring(7, 8)) + ".size")){
															if (getConfig().isSet("tiers." + Integer.parseInt(sign.getLine(1).substring(7, 8)) + ".fee")){
																tier = Integer.parseInt(sign.getLine(1).substring(7, 8));
															}
														}
													}
												}
											}
											int fee = getConfig().getInt("tiers." + Integer.toString(tier) + ".fee");
											boolean free = false;
											if (fee == 0 || player.hasPermission("goldbank.fee.bank.exempt"))
												free = true;
											ItemStack hand = player.getItemInHand();
											if (hand.getType() == Material.GOLD_INGOT || free){
												if (hand.getAmount() >= fee || free){
													sign.setLine(2, "");
													if (master)
														sign.setLine(3, "§dMaster");
													else
														sign.setLine(3, "§5" + p);
													sign.update();
													int signX = sign.getX();
													int signY = sign.getY();
													int signZ = sign.getZ();
													st.executeUpdate("INSERT INTO banks (uuid, world, x, y, z, sign, tier) VALUES ('" + pUUID + "', '" + player.getWorld().getName() + "', '" + signX + "', '" + signY + "', '" + signZ + "', 'true', '" + tier + "')");
													try {
														File invF = new File(getDataFolder() + File.separator + "inventories", pUUID + ".dat");
														if (!invF.exists()){
															invF.createNewFile();
														}
														YamlConfiguration invY = new YamlConfiguration();
														invY.load(invF);
														Inventory inv = this.getServer().createInventory(null, getConfig().getInt("tiers." + tier + ".size"), p + "'s GoldBank Sign");
														invY.set("size", inv.getSize());
														for (int i = 0; i < inv.getSize(); i++){
															invY.set("" + i, inv.getItem(i));
														}
														invY.save(invF);
													}
													catch (Exception ex){
														log.warning("Couldn't save inventory for " + p);
														ex.printStackTrace();
													}
													finally {
														try {
															conn.close();
															st.close();
															rs.close();
														}
														catch (Exception g){
															g.printStackTrace();
														}
													}
													if (!free){
														ItemStack newstack = new ItemStack(Material.GOLD_INGOT, hand.getAmount() - fee);
														player.getInventory().setItemInHand(newstack);
														player.updateInventory();
														player.sendMessage(ChatColor.DARK_PURPLE + "Charged " + Integer.toString(fee) + " golden ingots");
													}
													else {
														player.sendMessage(ChatColor.DARK_PURPLE + "This one's on us!");
													}
													player.sendMessage(ChatColor.DARK_GREEN + "Thanks for registering!");
												}
												else {
													player.sendMessage(ChatColor.RED + "You must have " + Integer.toString(fee) + " golden ingots to buy a Bank Sign!");
												}
											}
											else {
												player.sendMessage(ChatColor.RED + "You must have golden ingots in your hand to buy a Bank Sign!");
											}
										}
										else {
											player.sendMessage(ChatColor.RED + "You already have a sign!");
										}
									}
									else {
										if (player.hasPermission("goldbank.sign.bank.use")){
											try {
												rs.close();
												rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE x = '" + checkX + "' AND y = '" + checkY + "' AND z = '" + checkZ + "' AND uuid = '" + getSafeUUID(p) + "'");
												int pcount = 0;
												while (rs.next()){
													pcount = rs.getInt(1);
												}
												if (pcount == 1 || (player.hasPermission("goldbank.sign.bank.use.others") && !master)){
													rs = st.executeQuery("SELECT * FROM banks WHERE uuid = '" + pUUID + "'");
													if (!master)
														rs = st.executeQuery("SELECT * FROM banks WHERE world = '" + checkWorld + "' AND x = '" + checkX + "' AND y = '" + checkY + "' AND z = '" + checkZ + "'");
													UUID dpUUID = UUID.fromString(rs.getString("uuid"));
													String dp = getSafePlayerName(dpUUID);
													File invF = new File(getDataFolder() + File.separator + "inventories", dpUUID + ".dat");
													if(invF.exists()){
														YamlConfiguration invY = new YamlConfiguration();
														invY.load(invF);
														int size = invY.getInt("size");
														Set<String> keys = invY.getKeys(false);
														ItemStack[] invI = new ItemStack[size];
														for (String invN : keys){
															if (isInt(invN)){
																int i = Integer.parseInt(invN);
																invI[i] =  invY.getItemStack(invN);
															}
														}
														Inventory inv = this.getServer().createInventory(null, size, dp + "'s GoldBank Sign");
														inv.setContents(invI);
														player.openInventory(inv);
														openPlayer[nextIndex] = dpUUID;
														openingPlayer[nextIndex] = pUUID;
														openType[nextIndex] = "bank";
														nextIndex += 1;
													}
												}
												else {
													System.out.println(p + ", " + getSafeUUID(p));
													if (!master)
														player.sendMessage(ChatColor.RED + "This Bank Sign does not belong to you!");
													else
														player.sendMessage(ChatColor.RED + "You have not registered a Bank Sign with this Master Sign!");
												}
											}
											catch (Exception h){
												h.printStackTrace();
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
									}
								}
								catch(Exception q){
									q.printStackTrace();
								}
								finally {
									try {
										rs.close();
										st.close();
										conn.close();
									}
									catch (Exception g){
										g.printStackTrace();
									}
								}
							}
							else {
								player.sendMessage(ChatColor.RED + "You don't have permission to do this!");
							}
						}
						else if (fline.equalsIgnoreCase("§2[GoldATM]")){
							e.setCancelled(true);
							if (player.hasPermission("goldbank.sign.atm.use")){
								int atmfee = getConfig().getInt("atmfee");
								boolean enough = false;
								boolean notzero = false;
								if (atmfee != 0){
									notzero = true;
									Inventory pInv = player.getInventory();
									int nuggets = InventoryUtils.getAmountInInv(pInv, Material.GOLD_NUGGET, -1);
									if (nuggets >= atmfee){
										enough = true;
									}
								}
								else {
									enough = true;
								}
								if (player.hasPermission("goldbank.fee.atm.exempt")){
									notzero = false;
									enough = true;
								}
								Connection conn = null;
								ResultSet rs = null;
								Statement st = null;
								try {
									Class.forName("org.sqlite.JDBC");
									String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
									conn = DriverManager.getConnection(dbPath);
									st = conn.createStatement();
									rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE uuid = '" + pUUID + "'");
									int count = 0;
									while (rs.next()){
										count = rs.getInt(1);
									}
									if (count == 1){
										if (enough == true){
											if (notzero == true){
												InventoryUtils.removeFromPlayerInv(player, Material.GOLD_NUGGET, 0, atmfee);
												player.sendMessage(ChatColor.DARK_PURPLE + "Charged " + atmfee + " golden nuggets");
											}
											else {
												player.sendMessage(ChatColor.DARK_PURPLE + "This one's on us!");
											}
											File invF = new File(getDataFolder() + File.separator + "inventories", pUUID + ".dat");
											if(invF.exists()){
												YamlConfiguration invY = new YamlConfiguration();
												invY.load(invF);
												int size = invY.getInt("size");
												Set<String> keys = invY.getKeys(false);
												ItemStack[] invI = new ItemStack[size];
												for (String invN : keys){
													if (isInt(invN)){
														int i = Integer.parseInt(invN);
														invI[i] =  invY.getItemStack(invN);
													}
												}
												Inventory inv = this.getServer().createInventory(null, size, p + "'s GoldBank Sign");
												inv.setContents(invI);
												player.openInventory(inv);
												openPlayer[nextIndex] = pUUID;
												openingPlayer[nextIndex] = pUUID;
												openType[nextIndex] = "bank";
												nextIndex += 1;
											}
										}
										else {
											player.sendMessage(ChatColor.RED + "You don't have enough golden nuggets to use that!");
										}
									}
									else {
										player.sendMessage(ChatColor.RED + "You don't have a GoldBank Sign!");
									}
								}
								catch (Exception f){
									f.printStackTrace();
								}
								finally {
									try {
										conn.close();
										st.close();
										rs.close();
									}
									catch (Exception q){
										q.printStackTrace();
									}
								}
							}
							else {
								player.sendMessage(ChatColor.RED + "You don't have permission to do this!");
							}
						}
						int i = 0;
						Connection conn = null;
						Statement st = null;
						ResultSet rs = null;
						try {
							Class.forName("org.sqlite.JDBC");
							String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
							conn = DriverManager.getConnection(dbPath);
							st = conn.createStatement();
							rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE world = '" + e.getClickedBlock().getWorld().getName() + "' AND x = '" + e.getClickedBlock().getX() + "' AND y = '" + e.getClickedBlock().getY() + "' AND z = '" + e.getClickedBlock().getZ() + "'");
							while (rs.next()){
								i = rs.getInt(1);
							}
						}
						catch (Exception q){
							q.printStackTrace();
						}
						finally {
							try {
								conn.close();
								st.close();
								rs.close();
							}
							catch (Exception k){
								k.printStackTrace();
							}
						}
						if (i > 0){
							boolean logging = false;
							if (shopLog.containsKey(player.getName())){
								if (shopLog.get(player.getName()) == 0)
									logging = true;
							}
							if (!logging){
								if (player.hasPermission("goldbank.sign.shop.use")){
									try {
										Class.forName("org.sqlite.JDBC");
										String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
										conn = DriverManager.getConnection(dbPath);
										st = conn.createStatement();
										rs = st.executeQuery("SELECT * FROM shops WHERE world = '" + e.getClickedBlock().getWorld().getName() + "' AND x = '" + e.getClickedBlock().getX() + "' AND y = '" + e.getClickedBlock().getY() + "' AND z = '" + e.getClickedBlock().getZ() + "'");
										String adminS = rs.getString("admin");
										boolean admin = false;
										if (adminS.equals("true"))
											admin = true;
										int shopId = rs.getInt("id");
										int matId = rs.getInt("material");
										Material mat = null;
										boolean pHead = false;
										if (matId >= 0)
											mat = Material.getMaterial(matId);
										else if (matId == -2)
											pHead = true;
										String matName = "";
										if (!pHead)
											matName = mat.toString();
										else
											matName = "PlayerHead";
										int dataValue = rs.getInt("data");
										String forMatName = matName.toLowerCase().replace("_", " ");
										int buyPrice = rs.getInt("buyprice");
										int buyAmount = rs.getInt("buyamount");
										int sellPrice = rs.getInt("sellprice");
										int sellAmount = rs.getInt("sellamount");
										String buyUnit = rs.getString("buyunit");
										String sellUnit = rs.getString("sellunit");
										int buyMult = 1;
										if (buyUnit.equals("i"))
											buyMult = 9;
										if (buyUnit.equals("b"))
											buyMult = 81;
										int sellMult = 1;
										if (sellUnit.equals("i"))
											sellMult = 9;
										if (sellUnit.equals("b"))
											sellMult = 81;
										buyPrice *= buyMult;
										sellPrice *= sellMult;
										ItemStack buyIs = null;
										ItemStack sellIs = null;
										if (!pHead){
											buyIs = new ItemStack(mat, buyAmount);
											buyIs.setDurability((short)dataValue);
											sellIs = new ItemStack(mat, sellAmount);
											sellIs.setDurability((short)dataValue);
										}
										else {
											buyIs = new ItemStack(Material.SKULL_ITEM, buyAmount);
											buyIs.setDurability((short)3);
											SkullMeta meta = (SkullMeta)buyIs.getItemMeta();
											meta.setOwner(e.getPlayer().getName());
											buyIs.setItemMeta(meta);
										}
										Location chestLoc = new Location(e.getClickedBlock().getWorld(), e.getClickedBlock().getX(), (e.getClickedBlock().getY() - 1), e.getClickedBlock().getZ());
										boolean valid = true;
										if (chestLoc.getBlock().getType() != Material.CHEST && !admin)
											valid = false;
										if (valid){
											Chest chest = null;
											Inventory chestInv = null;
											if (!admin){
												chest = (Chest)chestLoc.getBlock().getState();
												chestInv = chest.getInventory();
											}
											// buy
											if (player.getItemInHand().getType() == Material.GOLD_BLOCK || player.getItemInHand().getType() == Material.GOLD_INGOT || player.getItemInHand().getType() == Material.GOLD_NUGGET){
												e.setCancelled(true);
												if (buyPrice > 0 && buyAmount > 0){
													boolean enough = true;
													if (chestInv != null)
														if ((InventoryUtils.getAmountInInv(chestInv, mat, dataValue) < buyAmount && !admin) || admin)
															enough = false;
													if (enough){
														Inventory inv = player.getInventory();
														int blocks = InventoryUtils.getAmountInInv(inv, Material.GOLD_BLOCK, -1);
														int ingots = InventoryUtils.getAmountInInv(inv, Material.GOLD_INGOT, -1);
														int nuggets = InventoryUtils.getAmountInInv(inv, Material.GOLD_NUGGET, -1);
														int totalblocks = (blocks * 81);
														int totalingots = (ingots * 9);
														int total = totalblocks + totalingots + nuggets;
														if (total >= buyPrice){
															if (InventoryUtils.getNullsInInv(inv) >= (buyAmount / 64) + 1){
																int remaining = buyPrice;
																if (remaining >= 81 && InventoryUtils.getAmountInInv(inv, Material.GOLD_BLOCK, -1) >= 1){
																	int remove = 0;
																	if (blocks >= remaining / 81){
																		remove = remaining / 81;
																	}
																	else
																		remove = blocks;
																	InventoryUtils.removeFromPlayerInv(player, Material.GOLD_BLOCK, 0, remove);
																	remaining = buyPrice - (remove * 81);
																}
																if (remaining >= 9 && InventoryUtils.getAmountInInv(inv, Material.GOLD_INGOT, -1) >= 1){
																	int remove = 0;
																	if (ingots >= remaining / 9){
																		remove = remaining / 9;
																	}
																	else {
																		remove = ingots;
																	}
																	InventoryUtils.removeFromPlayerInv(player, Material.GOLD_INGOT, 0, remove);
																	remaining = remaining - (remove * 9);
																}
																if (remaining >= 9 && InventoryUtils.getAmountInInv(player.getInventory(), Material.GOLD_BLOCK) >= 1){
																	InventoryUtils.removeFromPlayerInv(player, Material.GOLD_BLOCK, 0, 1);
																	inv.addItem(new ItemStack[] {
																			new ItemStack(Material.GOLD_INGOT, 9 - (remaining * 9))});
																	remaining = 0;
																}
																if (remaining >= 1){
																	int nuggetNum = InventoryUtils.getAmountInInv(player.getInventory(), Material.GOLD_NUGGET);
																	if (nuggetNum >= 1){
																		if (nuggetNum >= remaining){
																			InventoryUtils.removeFromPlayerInv(player, Material.GOLD_NUGGET, 0, remaining);
																			remaining = 0;
																		}
																		else {
																			InventoryUtils.removeFromPlayerInv(player, Material.GOLD_NUGGET, 0, nuggetNum);
																			remaining -= nuggetNum;
																		}
																	}
																}
																if (remaining >= 1 && InventoryUtils.getAmountInInv(player.getInventory(), Material.GOLD_INGOT) >= 1){
																	InventoryUtils.removeFromPlayerInv(player, Material.GOLD_INGOT, 0, 1);
																	player.getInventory().addItem(new ItemStack(Material.GOLD_NUGGET, 9 - remaining));
																	remaining = 0;
																}
																if (remaining >= 1 && InventoryUtils.getAmountInInv(player.getInventory(), Material.GOLD_BLOCK )>= 1){
																	InventoryUtils.removeFromPlayerInv(player, Material.GOLD_BLOCK, 0, 1);
																	player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 8 - (remaining / 9)));
																	remaining -= remaining / 9;
																	player.getInventory().addItem(new ItemStack(Material.GOLD_NUGGET, 9 - remaining));
																	remaining = 0;
																}
																if (!admin){
																	InventoryUtils.removeFromInv(chestInv, buyIs.getType(), 0, buyIs.getAmount());
																	int remainder = buyPrice;
																	int newBlocks = remainder / 81;
																	remainder -= newBlocks * 81;
																	int newIngots = remainder / 9;
																	remainder -= newIngots * 9;
																	int newNuggets = remainder;
																	ItemStack addBlocks = new ItemStack(Material.GOLD_BLOCK, newBlocks);
																	ItemStack addIngots = new ItemStack(Material.GOLD_INGOT, newIngots);
																	ItemStack addNuggets = new ItemStack(Material.GOLD_NUGGET, newNuggets);
																	if (addBlocks.getAmount() > 0)
																		chestInv.addItem(new ItemStack[] {
																				addBlocks});
																	if (addIngots.getAmount() > 0){
																		chestInv.addItem(new ItemStack[] {
																				addIngots});
																	}
																	if (addNuggets.getAmount() > 0)
																		chestInv.addItem(new ItemStack[] {
																				addNuggets});
																	if (InventoryUtils.getAmountInInv(chestInv, Material.GOLD_NUGGET) >= 9){
																		int extraNuggets = InventoryUtils.getAmountInInv(chestInv, Material.GOLD_NUGGET, -1);
																		int nuggetNum = (extraNuggets / 9) * 9;
																		InventoryUtils.removeFromInv(chestInv, Material.GOLD_NUGGET, 0, nuggetNum);
																		chestInv.addItem(new ItemStack[] {
																				new ItemStack(Material.GOLD_INGOT, nuggetNum / 9)});
																	}
																	if (InventoryUtils.getAmountInInv(chestInv, Material.GOLD_INGOT) >= 9){
																		int extraIngots = InventoryUtils.getAmountInInv(chestInv, Material.GOLD_INGOT, -1);
																		int blockNum = extraIngots / 9;
																		InventoryUtils.removeFromInv(chestInv, Material.GOLD_INGOT, 0, blockNum * 9);
																		chestInv.addItem(new ItemStack[] {
																				new ItemStack(Material.GOLD_BLOCK, blockNum)});
																	}
																}
																inv.addItem(new ItemStack[] {buyIs});
																player.updateInventory();
																st.executeUpdate("INSERT INTO shoplog (shop, player, action, material, data, quantity, time) VALUES ('" + shopId + "', '" + getSafeUUID(player) + "', '0', '" + matId + "', '" + dataValue + "', '" + buyIs.getAmount() + "', '" + System.currentTimeMillis() / 1000 + "')");
																String buyPriceS = "s";
																if (buyPrice / buyMult == 1)
																	buyPriceS = "";
																String unit = "nugget";
																if (buyUnit.equals("b"))
																	unit = "block";
																else if (buyUnit.equals("i"))
																	unit = "ingot";
																player.sendMessage(ChatColor.DARK_PURPLE + "You bought " + buyAmount + " " + forMatName + " for " + buyPrice / buyMult + " golden " + unit + buyPriceS + "!");
															}
															else
																player.sendMessage(ChatColor.DARK_PURPLE + "You don't have enough open slots in your inventory!");
														}
														else
															player.sendMessage(ChatColor.DARK_PURPLE + "You don't have enough gold to buy that!");
													}
													else
														player.sendMessage(ChatColor.DARK_PURPLE + "The associated chest does not have enough " + forMatName + "!");
												}
												else
													player.sendMessage(ChatColor.DARK_PURPLE + "You may not buy from this sign!");
											}
											// sell
											else if (!pHead){
												if (player.getItemInHand().getType() == mat){
													e.setCancelled(true);
													if (sellPrice > 0 && sellAmount > 0){
														Material[] tools = new Material[]{
																Material.DIAMOND_PICKAXE,
																Material.DIAMOND_SWORD,
																Material.DIAMOND_SPADE,
																Material.DIAMOND_AXE,
																Material.DIAMOND_HOE,
																Material.DIAMOND_HELMET,
																Material.DIAMOND_CHESTPLATE,
																Material.DIAMOND_LEGGINGS,
																Material.DIAMOND_BOOTS,
																Material.IRON_PICKAXE,
																Material.IRON_SWORD,
																Material.IRON_SPADE,
																Material.IRON_AXE,
																Material.IRON_HOE,
																Material.IRON_HELMET,
																Material.IRON_CHESTPLATE,
																Material.IRON_LEGGINGS,
																Material.IRON_BOOTS,
																Material.GOLD_PICKAXE,
																Material.GOLD_SWORD,
																Material.GOLD_SPADE,
																Material.GOLD_AXE,
																Material.GOLD_HOE,
																Material.GOLD_HELMET,
																Material.GOLD_CHESTPLATE,
																Material.GOLD_LEGGINGS,
																Material.GOLD_BOOTS,
																Material.STONE_PICKAXE,
																Material.STONE_SWORD,
																Material.STONE_SPADE,
																Material.STONE_AXE,
																Material.STONE_HOE,
																Material.CHAINMAIL_HELMET,
																Material.CHAINMAIL_CHESTPLATE,
																Material.CHAINMAIL_LEGGINGS,
																Material.CHAINMAIL_BOOTS,
																Material.WOOD_PICKAXE,
																Material.WOOD_SWORD,
																Material.WOOD_SPADE,
																Material.WOOD_AXE,
																Material.WOOD_HOE,
																Material.LEATHER_HELMET,
																Material.LEATHER_CHESTPLATE,
																Material.LEATHER_LEGGINGS,
																Material.LEATHER_BOOTS,
																Material.FLINT_AND_STEEL,
																Material.SHEARS,
																Material.BOW,
																Material.FISHING_ROD,
																Material.ANVIL};
														boolean newTool = true;
														if (Arrays.asList(tools).contains(mat) && getConfig().getBoolean("selldamageditems") == false){
															if (player.getItemInHand().getDurability() != 0){
																newTool = false;
															}
														}
														if (newTool){
															boolean validSell = true;
															if (!admin)
																if (((InventoryUtils.getAmountInInv(chestInv, Material.GOLD_NUGGET, -1)) + (InventoryUtils.getAmountInInv(chestInv, Material.GOLD_INGOT, -1) * 9) + (InventoryUtils.getAmountInInv(chestInv, Material.GOLD_BLOCK, -1) * 81)) < sellPrice)
																	validSell = false;
															if (validSell){
																Inventory inv = player.getInventory();
																if (InventoryUtils.getAmountInInv(inv, mat, dataValue) >= sellAmount){
																	InventoryUtils.removeFromPlayerInv(player, sellIs.getType(), sellIs.getDurability(), sellIs.getAmount());
																	if (!admin){
																		int remaining = sellPrice;
																		int blocks = InventoryUtils.getAmountInInv(chestInv, Material.GOLD_BLOCK, -1);
																		int ingots = InventoryUtils.getAmountInInv(chestInv, Material.GOLD_INGOT, -1);
																		if (sellPrice >= 9 && blocks >= 1){
																			int remove = 0;
																			if (blocks >= remaining / 81){
																				remove = sellPrice / 81;
																			}
																			else {
																				remove = blocks;
																			}
																			InventoryUtils.removeFromInv(chestInv, Material.GOLD_BLOCK, 0, remove);
																			remaining = sellPrice - (remove * 81);
																		}
																		if (remaining >= 9 && ingots >= 1){
																			int remove = 0;
																			if (ingots * 9 >= remaining){
																				remove = remaining / 9;
																			}
																			else {
																				remove = ingots;
																			}
																			InventoryUtils.removeFromInv(chestInv, Material.GOLD_INGOT, 0, remove);
																			remaining = remaining - (remove * 9);
																		}
																		if (remaining >= 9){
																			InventoryUtils.removeFromInv(chestInv, Material.GOLD_BLOCK, 0, 1);
																			chestInv.addItem(new ItemStack[] {
																					new ItemStack(Material.GOLD_INGOT, 9 - (remaining / 9))});
																		}
																		if (remaining >= 1){
																			if (InventoryUtils.getAmountInInv(chestInv, Material.GOLD_NUGGET) >= remaining){
																				InventoryUtils.removeFromInv(chestInv, Material.GOLD_NUGGET, 0, remaining);
																				remaining = 0;
																			}
																			else {
																				InventoryUtils.removeFromInv(chestInv, Material.GOLD_NUGGET, 0, InventoryUtils.getAmountInInv(chestInv, Material.GOLD_NUGGET));
																				remaining -= InventoryUtils.getAmountInInv(chestInv, Material.GOLD_NUGGET);
																			}
																		}
																		if (remaining >= 1 && InventoryUtils.getAmountInInv(chestInv, Material.GOLD_INGOT) >= 1){
																			InventoryUtils.removeFromInv(chestInv, Material.GOLD_INGOT, 0, 1);
																			chestInv.addItem(new ItemStack(Material.GOLD_NUGGET, 9 - remaining));
																			remaining = 0;
																		}
																		if (remaining >= 1 && InventoryUtils.getAmountInInv(chestInv, Material.GOLD_BLOCK )>= 1){
																			InventoryUtils.removeFromInv(chestInv, Material.GOLD_BLOCK, 0, 1);
																			chestInv.addItem(new ItemStack(Material.GOLD_INGOT, 8 - (remaining / 9)));
																			remaining -= remaining / 9;
																			chestInv.addItem(new ItemStack(Material.GOLD_NUGGET, 9 - remaining));
																			remaining = 0;
																		}
																		chestInv.addItem(new ItemStack[] {sellIs});
																	}
																	int remainder = sellPrice;
																	int newBlocks = remainder / 81;
																	remainder -= newBlocks * 81;
																	int newIngots = remainder / 9;
																	remainder -= newIngots * 9;
																	int newNuggets = remainder;
																	ItemStack addBlocks = new ItemStack(Material.GOLD_BLOCK, newBlocks);
																	ItemStack addIngots = new ItemStack(Material.GOLD_INGOT, newIngots);
																	ItemStack addNuggets = new ItemStack(Material.GOLD_NUGGET, newNuggets);
																	if (addBlocks.getAmount() > 0)
																		inv.addItem(new ItemStack[] {
																				addBlocks});
																	if (addIngots.getAmount() > 0)
																		inv.addItem(new ItemStack[] {
																				addIngots});
																	if (addNuggets.getAmount() > 0)
																		inv.addItem(new ItemStack[] {
																				addNuggets});
																	player.updateInventory();
																	st.executeUpdate("INSERT INTO shoplog (shop, player, action, material, data, quantity, time) VALUES ('" + shopId + "', '" + getSafeUUID(player) + "', '1', '" + mat.getId() + "', '" + dataValue + "', '" + sellIs.getAmount() + "', '" + System.currentTimeMillis() / 1000 + "')");
																	String sellPriceS = "s";
																	if (sellPrice / sellMult == 1)
																		sellPriceS = "";
																	String unit = "nugget";
																	if (sellUnit.equals("b"))
																		unit = "block";
																	else if (sellUnit.equals("i"))
																		unit = "ingot";
																	player.sendMessage(ChatColor.DARK_PURPLE + "You sold " + sellAmount + " " + forMatName + " for " + (sellPrice / sellMult) + " golden " + unit + sellPriceS + "!");
																}
																else
																	player.sendMessage(ChatColor.DARK_PURPLE + "You do not have enough " + forMatName + "!");
															}
															else
																player.sendMessage(ChatColor.DARK_PURPLE + "Error: The associated chest does not have enough gold!");
														}
														else
															player.sendMessage(ChatColor.DARK_PURPLE + "You may not sell damaged tools!");
													}
													else
														player.sendMessage(ChatColor.DARK_PURPLE + "You may not sell to this sign!");
												}
												else
													player.sendMessage(ChatColor.DARK_PURPLE + "You must have gold or " + forMatName + "(s) in your hand to use this sign!");
											}
										}
										else {
											player.sendMessage(ChatColor.DARK_PURPLE + "Error: This player shop does not have an associated chest! Attempting to create one...");
											if (chestLoc.getBlock().getType() == Material.AIR)
												chestLoc.getBlock().setType(Material.CHEST);
											else
												player.sendMessage(ChatColor.DARK_PURPLE + "Could not create the chest because the block is not air! Ask the shop owner to change the block below this sign to air.");
										}
									}
									catch (Exception f){
										f.printStackTrace();
									}
									finally {
										try {
											conn.close();
											st.close();
											rs.close();
										}
										catch (Exception t){
											t.printStackTrace();
										}
									}
								}
								else
									player.sendMessage(ChatColor.RED + "You don't have permission to use this sign!");
							}
						}
					}
				}
			}
		}
		// check for left click
		if (e.getAction() == Action.LEFT_CLICK_BLOCK){
			if (e.getClickedBlock().getType() == Material.WALL_SIGN || e.getClickedBlock().getType() == Material.SIGN_POST){
				String blockWorld = e.getClickedBlock().getWorld().getName();
				int blockX = e.getClickedBlock().getX();
				int blockY = e.getClickedBlock().getY();
				int blockZ = e.getClickedBlock().getZ();
				Player player = e.getPlayer();
				String p = player.getName();
				Connection conn = null;
				Statement st = null;
				ResultSet rs = null;
				try {
					Class.forName("org.sqlite.JDBC");
					String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
					conn = DriverManager.getConnection(dbPath);
					st = conn.createStatement();
					rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE world = '" + blockWorld + "' AND x = '" + blockX + "' AND y = '" + blockY + "' AND z = '" + blockZ + "'");
					int count = 0;
					while (rs.next()){
						count = rs.getInt(1);
					}
					boolean master = false;
					rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE world = '" + blockWorld + "' AND x = '" + blockX + "' AND y = '" + blockY + "' AND z = '" + blockZ + "' AND uuid = 'MASTER'");
					int masterCount = 0;
					while (rs.next()){
						masterCount = rs.getInt(1);
					}
					if (masterCount != 0)
						master = true;
					// check if a sign is registered at the same location
					if (count != 0){
						UUID pUUID = getSafeUUID(p);
						rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE world = '" + blockWorld + "' AND x = '" + blockX + "' AND y = '" + blockY + "' AND z = '" + blockZ + "' AND uuid = '" + pUUID + "'");
						int newcount = 0;
						while (rs.next()){
							newcount = rs.getInt(1);
						}
						// verify that user has proper permissions
						if (player.hasPermission("goldbank.sign.bank.unclaim")){
							// check if player owns sign at location or if they have proper permissions to unclaim the signs of others
							if (newcount != 0 || player.hasPermission("goldbank.sign.bank.unclaim.others")){
								rs = st.executeQuery("SELECT * FROM banks WHERE world = '" + blockWorld + "' AND x = '" + blockX + "' AND y = '" + blockY + "' AND z = '" + blockZ + "'");
								UUID dpUUID;
								if (master)
									dpUUID = pUUID;
								else
									dpUUID = UUID.fromString(rs.getString("uuid"));
								Location signLoc = new Location(e.getClickedBlock().getWorld(), blockX, blockY, blockZ);
								if (signLoc.getBlock().getType() == Material.WALL_SIGN || signLoc.getBlock().getType() == Material.SIGN_POST){
									Sign sign = (Sign)signLoc.getBlock().getState();
									if (!master){
										sign.setLine(2, "§5Claim this");
										sign.setLine(3, "§5sign!");
									}
									sign.update();
									// check if sign is master or if player owns sign at location
									if (!master || newcount != 0){
										e.setCancelled(true);
										st.executeUpdate("DELETE FROM banks WHERE uuid = '" + dpUUID + "'");
										File file = new File(this.getDataFolder() + File.separator + "inventories" + File.separator + dpUUID + ".dat");
										World world = player.getWorld();
										YamlConfiguration invY = new YamlConfiguration();
										invY.load(file);
										Set<String> keys = invY.getKeys(false);
										for (String invN : keys){
											if (isInt(invN)){
												world.dropItem(player.getLocation(), invY.getItemStack(invN));
											}
										}
										file.delete();
										player.sendMessage(ChatColor.DARK_PURPLE + "Bank Sign unclaimed!");
									}
									else if (player.hasPermission("goldbank.sign.bank.destroy.master")){
										rs = st.executeQuery("SELECT * FROM banks WHERE world = '" + blockWorld + "' AND x = '" + blockX + "' AND y = '" + blockY + "' AND z = '" + blockZ + "'");
										while (rs.next()){
											String owner = rs.getString("uuid");
											File file = new File(this.getDataFolder() + File.separator + "inventories" + File.separator + owner + ".dat");
											file.delete();
										}
										st.executeUpdate("DELETE FROM banks WHERE world = '" + blockWorld + "' AND x = '" + blockX + "' AND y = '" + blockY + "' AND z = '" + blockZ + "'");
										player.sendMessage(ChatColor.DARK_PURPLE + "Master sign unregistered!");
									}
								}
							}
							else {
								if (!master)
									player.sendMessage(ChatColor.RED + "This Bank Sign does not belong to you!");
								else
									player.sendMessage(ChatColor.RED + "You have not registered a Bank Sign with this Master Sign!");
							}
						}
						else {
							player.sendMessage(ChatColor.RED + "You don't have permission to unclaim this!");
						}
					}
				}
				catch (Exception f){
					f.printStackTrace();
				}
				finally {
					try {
						conn.close();
						st.close();
						rs.close();
					}
					catch (Exception q){
						q.printStackTrace();
					}
				}
			}
		}
		if (e.getClickedBlock().getType() == Material.CHEST){
			String blockWorld = e.getClickedBlock().getWorld().getName();
			int blockX = e.getClickedBlock().getX();
			int blockY = e.getClickedBlock().getY();
			int blockZ = e.getClickedBlock().getZ();
			Player player = e.getPlayer();
			String p = player.getName();
			Connection conn = null;
			Statement st = null;
			ResultSet rs = null;
			try {
				Class.forName("org.sqlite.JDBC");
				String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
				conn = DriverManager.getConnection(dbPath);
				st = conn.createStatement();
				rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE world = '" + blockWorld + "' AND x = '" + blockX + "' AND y = '" + (blockY + 1) + "' AND z = '" + blockZ + "' AND admin = 'false'");
				int count = 0;
				while (rs.next()){
					count = rs.getInt(1);
				}
				if (count == 1){
					rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE x = '" + blockX + "' AND y = '" + (blockY + 1) + "' AND z = '" + blockZ + "' AND creator = '" + p + "' AND admin = 'false'");
					int newcount = 0;
					while (rs.next()){
						newcount = rs.getInt(1);
					}
					if (newcount > 0 || player.hasPermission("goldbank.sign.shop.destroy.*")){
						if (e.getAction() == Action.LEFT_CLICK_BLOCK){
							e.setCancelled(true);
							player.sendMessage(ChatColor.RED + "Please left-click the Shop sign to destroy this shop!");
						}
					}
					else {
						e.setCancelled(true);
						player.sendMessage(ChatColor.RED + "That chest is part of a player shop!");
					}
				}
			}
			catch (Exception f){
				f.printStackTrace();
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
	}

	// check if destroyed block is or holds GoldBank sign
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent b){
		if (b.getBlock().getType() == Material.WALL_SIGN || b.getBlock().getType() == Material.SIGN_POST){
			Sign sign = (Sign)b.getBlock().getState();
			Connection conn = null;
			Statement st = null;
			ResultSet rs = null;
			try {
				Class.forName("org.sqlite.JDBC");
				String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
				conn = DriverManager.getConnection(dbPath);
				st = conn.createStatement();
				rs = st.executeQuery("SELECT * FROM shops WHERE world = '" + b.getBlock().getWorld().getName() + "' AND x = '" + b.getBlock().getX() + "' AND y = '" + b.getBlock().getY() + "' AND z = '" + b.getBlock().getZ() + "'");
				int i = 0;
				while (rs.next())
					i = rs.getInt(1);
				if (i != 0){
					b.setCancelled(true);
					rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE world = '" + b.getBlock().getWorld().getName() + "' AND x = '" + b.getBlock().getX() + "' AND y = '" + b.getBlock().getY() + "' AND z = '" + b.getBlock().getZ() + "' AND creator = '" + getSafeUUID(b.getPlayer().getName()) + "'");
					i = 0;
					while (rs.next())
						i = rs.getInt(1);
					if (i != 0)
						if (!b.getPlayer().hasPermission("goldbank.sign.shop.destroy"))
							b.getPlayer().sendMessage(ChatColor.RED +"You don't have permission to break that block!");
						else
							b.getPlayer().sendMessage(ChatColor.RED + "Please left-click the GoldShop on this block to unregister it.");
					else if (!b.getPlayer().hasPermission("goldbank.sign.shop.destroy.*"))
						b.getPlayer().sendMessage(ChatColor.RED + "Please left-click the GoldShop on this block to unregister it.");
					else
						b.getPlayer().sendMessage(ChatColor.RED +"You don't have permission to break that block!");
				}
			}
			catch (Exception e){
				e.printStackTrace();
			}
			finally {
				try {
					rs.close();
					st.close();
					conn.close();
				}
				catch (Exception u){
					u.printStackTrace();
				}
			}
			if (sign.getLine(0).equalsIgnoreCase("§2[GoldBank]")){
				boolean master = false;
				conn = null;
				st = null;
				rs = null;
				try {
					Class.forName("org.sqlite.JDBC");
					String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
					conn = DriverManager.getConnection(dbPath);
					st = conn.createStatement();
					rs = st.executeQuery("SELECT * FROM banks WHERE world = '" + b.getBlock().getWorld().getName() + "' AND x = '" + b.getBlock().getX() + "' AND y = '" + b.getBlock().getY() + "' AND z = '" + b.getBlock().getZ() + "' AND uuid = 'MASTER'");
					int masterCount = 0;
					while (rs.next())
						masterCount = rs.getInt(1);
					if (masterCount != 0)
						master = true;
				}
				catch (Exception ex){
					ex.printStackTrace();
				}
				finally {
					try {
						rs.close();
						st.close();
						conn.close();
					}
					catch (Exception e){
						e.printStackTrace();
					}
				}
				String node = "goldbank.sign.bank.destroy";
				if (master)
					node = "goldbank.sign.bank.destroy.master";
				if (!b.getPlayer().hasPermission(node)){
					b.setCancelled(true);
					b.getPlayer().sendMessage(ChatColor.RED + "Oh noes! You don't have permission to break that sign! :(");
				}
			}
			else if (sign.getLine(0).equalsIgnoreCase("§2[GoldATM]")){
				if (!b.getPlayer().hasPermission("goldbank.sign.atm.destroy")){
					b.setCancelled(true);
					b.getPlayer().sendMessage(ChatColor.RED + "Oh noes! You don't have permission to break that sign! :(");
				}
			}
		}
		if (getAdjacentBlock(b.getBlock(), Material.WALL_SIGN) != null || getAdjacentBlock(b.getBlock(), Material.SIGN_POST) != null){
			Block adjBlock = null;
			if (getAdjacentBlock(b.getBlock(), Material.WALL_SIGN) != null){
				adjBlock = getAdjacentBlock(b.getBlock(), Material.WALL_SIGN);
			}
			else if (getAdjacentBlock(b.getBlock(), Material.SIGN_POST)!= null){
				adjBlock = getAdjacentBlock(b.getBlock(), Material.SIGN_POST);
			}
			Sign sign = (Sign)adjBlock.getState();
			Connection conn = null;
			Statement st = null;
			ResultSet rs = null;
			try {
				Class.forName("org.sqlite.JDBC");
				String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
				conn = DriverManager.getConnection(dbPath);
				st = conn.createStatement();
				rs = st.executeQuery("SELECT * FROM shops WHERE world = '" + adjBlock.getWorld().getName() + "' AND x = '" + adjBlock.getX() + "' AND y = '" + adjBlock.getY() + "' AND z = '" + adjBlock.getZ() + "'");
				int i = 0;
				while (rs.next())
					i = rs.getInt(1);
				if (i != 0){
					b.setCancelled(true);
					rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE world = '" + adjBlock.getWorld().getName() + "' AND x = '" + adjBlock.getX() + "' AND y = '" + adjBlock.getY() + "' AND z = '" + adjBlock.getZ() + "' AND creator = '" + b.getPlayer().getName() + "'");
					i = 0;
					while (rs.next())
						i = rs.getInt(1);
					if (i != 0 || b.getPlayer().hasPermission("goldbank.sign.shop.destroy.*")){
						if (!b.getPlayer().hasPermission("goldbank.sign.shop.destroy")){
							b.getPlayer().sendMessage(ChatColor.RED +"You don't have permission to break that block!");
						}
						else {
							b.getPlayer().sendMessage(ChatColor.RED + "Please left-click the GoldShop on this block to unregister it.");
						}
					}
					else {
						if (!b.getPlayer().hasPermission("goldbank.sign.shop.destroy.*")){
							b.getPlayer().sendMessage(ChatColor.RED +"You don't have permission to break that block!");
						}
						else {
							b.getPlayer().sendMessage(ChatColor.RED + "Please left-click the GoldShop on this block to unregister it.");
						}
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
			if (sign.getLine(0).equalsIgnoreCase("§2[GoldBank]")){
				b.setCancelled(true);
				if (!b.getPlayer().hasPermission("goldbank.sign.bank.destroy")){
					b.getPlayer().sendMessage(ChatColor.RED +"You don't have permission to break that block!");
				}
				else {
					/*try {
						Class.forName("org.sqlite.JDBC");
						String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
						conn = DriverManager.getConnection(dbPath);
						st = conn.createStatement();
						String checkWorld = adjBlock.getWorld().getName();
						int checkX = adjBlock.getX();
						int checkY = adjBlock.getY();
						int checkZ = adjBlock.getZ();
						rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE world = '" + checkWorld + "' AND x = '" + checkX + "' AND y = '" + checkY + "' AND z = '" + checkZ + "'");
						int i = 0;
						while (rs.next()){
							i = rs.getInt(1);
						}
						if (i > 0){
							int masterCount = 0;
							rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE world = '" + checkWorld + "' AND x = '" + checkX + "' AND y = '" + checkY + "' AND z = '" + checkZ + "' AND uuid = 'MASTER'");
							while (rs.next()){
								masterCount = rs.getInt(1);
							}
							boolean master = false;
							if (masterCount != 0){
								master = true;
								rs = st.executeQuery("SELECT * FROM banks WHERE world = '" + checkWorld + "' AND x = '" + checkX + "' AND y = '" + checkY + "' AND z = '" + checkZ + "'");
								while (rs.next()){;
									File file = new File(this.getDataFolder() + File.separator + "inventories" + File.separator + rs.getString("uuid") + ".dat");
									file.delete();
								}
								b.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "Master Sign Unregistered!");
							}
							else {
								b.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "GoldBank unregistered!");
							}
							if (!master || b.getPlayer().hasPermission("goldbank.sign.bank.destroy.master")){
								st.executeUpdate("DELETE FROM banks WHERE world = '" + checkWorld + "' AND x ='" + checkX + "' AND y = '" + checkY + "' AND z = '" + checkZ + "'");
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
					}*/
					b.getPlayer().sendMessage(ChatColor.RED + "Please left-click the GoldBank on this block to unregister it.");
				}
			}
			else if (sign.getLine(0).equalsIgnoreCase("§2[GoldATM]")){
				b.setCancelled(true);
				if (!b.getPlayer().hasPermission("goldbank.sign.atm.destroy")){
					b.getPlayer().sendMessage(ChatColor.RED + "You don't have permission to break that block!");
				}
			}
		}
		else if (getAdjacentBlock(b.getBlock(), Material.SIGN_POST) != null){
			Block adjblock = getAdjacentBlock(b.getBlock(), Material.SIGN_POST);
			Sign sign = (Sign)adjblock.getState();
			if (sign.getLine(0).equalsIgnoreCase("§2[GoldBank]")){
				b.setCancelled(true);
				if (!b.getPlayer().hasPermission("goldbank.sign.bank.destroy")){
					b.getPlayer().sendMessage(ChatColor.RED + "You don't have permission to break that block!");
				}
				else
					b.getPlayer().sendMessage(ChatColor.RED + "Please left-click the GoldBank on this block to unregister it.");
			}
			else if (sign.getLine(0).equalsIgnoreCase("§2[GoldATM]")){
				if (!b.getPlayer().hasPermission("goldbank.sign.atm.destroy")){
					b.getPlayer().sendMessage(ChatColor.RED + "You don't have permission to break that block!");
				}
				else
					b.getPlayer().sendMessage(ChatColor.RED + "Please left-click the GoldATM on this block to unregister it.");
			}
		}
		if (b.getBlock().getType() == Material.CHEST){
			String blockWorld = b.getBlock().getWorld().getName();
			int blockX = b.getBlock().getX();
			int blockY = b.getBlock().getY();
			int blockZ = b.getBlock().getZ();
			Player player = b.getPlayer();
			String p = player.getName();
			Connection conn = null;
			Statement st = null;
			ResultSet rs = null;
			try {
				Class.forName("org.sqlite.JDBC");
				String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
				conn = DriverManager.getConnection(dbPath);
				st = conn.createStatement();
				rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE world = '" + blockWorld + "' AND x = '" + blockX + "' AND y = '" + (blockY + 1) + "' AND z = '" + blockZ + "' AND admin = 'false'");
				int count = 0;
				while (rs.next()){
					count = rs.getInt(1);
				}
				if (count == 1){
					b.setCancelled(true);
					rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE x = '" + blockX + "' AND y = '" + (blockY + 1) + "' AND z = '" + blockZ + "' AND creator = '" + p + "' AND admin = 'false'");
					int newcount = 0;
					while (rs.next()){
						newcount = rs.getInt(1);
					}
					if (newcount > 0 || player.hasPermission("goldbank.sign.shop.destroy.*")){
						player.sendMessage(ChatColor.RED + "Please left-click the Shop sign to destroy this shop!");
					}
					else {
						player.sendMessage(ChatColor.RED + "That chest is part of a player shop!!");
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
	}

	// listen for block place event below player shop
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent c){
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			Class.forName("org.sqlite.JDBC");
			String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
			conn = DriverManager.getConnection(dbPath);
			st = conn.createStatement();
			rs = st.executeQuery("SELECT * FROM shops WHERE world = '" + c.getBlock().getWorld().getName() + "' AND x = '" + c.getBlock().getX() + "' AND y = '" + (c.getBlock().getY() + 1) + "' AND z = '" + c.getBlock().getZ() + "' AND admin = 'false'");
			int i = 0;
			while (rs.next()){
				i = rs.getInt(1);
			}
			if (i > 0){
				ResultSet res = st.executeQuery("SELECT * FROM shops WHERE world = '" + c.getBlock().getWorld().getName() + "' AND x = '" + c.getBlock().getX() + "' AND y = '" + (c.getBlock().getY() + 1) + "' AND z = '" + c.getBlock().getZ() + "' AND admin = 'false'");
				String creator = getSafePlayerName(UUID.fromString(res.getString("creator")));
				if (!creator.equalsIgnoreCase(c.getPlayer().getName())){
					c.setCancelled(true);
					c.getPlayer().sendMessage(ChatColor.RED + "This spot is owned by " + creator + "!");
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

	// listen for chest open
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onChestOpen(PlayerInteractEvent o){
		if (o.getAction() == Action.RIGHT_CLICK_BLOCK){
			if (o.getClickedBlock().getType() == Material.CHEST){
				Player player = o.getPlayer();
				String p = player.getName();
				Chest chest = (Chest) o.getClickedBlock().getState();
				String chestWorld = chest.getBlock().getWorld().getName();
				int chestX = chest.getBlock().getX();
				int chestY = chest.getBlock().getY();
				int chestZ = chest.getBlock().getZ();
				Connection conn = null;
				Statement st = null;
				ResultSet rs = null;
				try {
					Class.forName("org.sqlite.JDBC");
					String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
					conn = DriverManager.getConnection(dbPath);
					st = conn.createStatement();
					rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE world = '" + chestWorld + "' AND x = '" + chestX + "' AND y = '" + (chestY + 1) + "' AND z = '" + chestZ + "'");
					int count = 0;
					while (rs.next()){
						count = rs.getInt(1);
					}
					if (count == 1){
						rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE world = '" + chestWorld + "' AND x = '" + chestX + "' AND y = '" + (chestY + 1) + "' AND z = '" + chestZ + "' AND creator = '" + getSafeUUID(p) + "'");
						int seccount = 0;
						while (rs.next()){
							seccount = rs.getInt(1);
						}
						if (seccount == 0){
							if (!o.getPlayer().hasPermission("goldbank.sign.shop.access")){
								o.setCancelled(true);
								player.sendMessage(ChatColor.RED + "You don't have permission to open that GoldShop chest!");
							}
						}
					}
				}
				catch (Exception e){
					e.printStackTrace();
				}
				finally {
					try {
						rs.close();
						st.close();
						conn.close();
					}
					catch (Exception u){
						u.printStackTrace();
					}
				}
			}
		}
	}

	// watch out for TNT and creepers
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onExplosion(EntityExplodeEvent e){
		List<Block> blocks = e.blockList();
		Iterator<Block> it = blocks.iterator();
		while (it.hasNext()){
			Block block = it.next();
			if (block.getType() == Material.WALL_SIGN){
				Sign sign = (Sign)block.getState();
				if (sign.getLine(0).equalsIgnoreCase("§2[GoldBank]") || sign.getLine(0).equalsIgnoreCase("§2[GoldATM]")){
					it.remove();
				}
				String line = sign.getLine(0);
				String rline = line.replace("[", "");
				rline = rline.replace("]", "");
				rline = rline.replace("§2", "");
				rline = rline.toUpperCase();
				String[] matInfo = new String[2];
				if (rline.contains(":")){
					matInfo = rline.split(":");
					rline = matInfo[0];
				}
				boolean isValidInt = false;
				if (isInt(rline)){
					if (isMat(Integer.parseInt(rline))){
						isValidInt = true;
					}
				}
				if (isMat(rline) || isValidInt){
					it.remove();
				}
			}
			else if (block.getType() == Material.SIGN_POST){
				Sign sign = (Sign)block.getState();
				if (sign.getLine(0).equalsIgnoreCase("§2[GoldBank]") || sign.getLine(0).equalsIgnoreCase("§2[GoldATM]")){
					it.remove();
				}
				String line = sign.getLine(0);
				String rline = line.replace("[", "");
				rline = rline.replace("]", "");
				rline = rline.replace("§2", "");
				rline = rline.toUpperCase();
				String[] matInfo = new String[2];
				if (rline.contains(":")){
					matInfo = rline.split(":");
					rline = matInfo[0];
				}
				boolean isValidInt = false;
				if (isInt(rline)){
					if (isMat(Integer.parseInt(rline))){
						isValidInt = true;
					}
				}
				if (isMat(rline) || isValidInt){
					it.remove();
				}
			}
			else if (getAdjacentBlock(block, Material.WALL_SIGN) != null || getAdjacentBlock(block, Material.SIGN_POST) != null){
				Block adjBlock = null;
				if (getAdjacentBlock(block, Material.WALL_SIGN) != null){
					adjBlock = getAdjacentBlock(block, Material.WALL_SIGN);
				}
				else if (getAdjacentBlock(block, Material.SIGN_POST) != null){
					adjBlock = getAdjacentBlock(block, Material.SIGN_POST);
				}
				Sign sign = (Sign)adjBlock.getState();
				if (sign.getLine(0).equalsIgnoreCase("§2[GoldBank]")){
					it.remove();
				}
				else if (sign.getLine(0).equalsIgnoreCase("§2[GoldATM]")){
					it.remove();
				}
				if (adjBlock != null || block.getType() == Material.CHEST){
					String line = ((Sign)adjBlock.getState()).getLine(0);
					String rline = line.replace("[", "");
					rline = rline.replace("]", "");
					rline = rline.replace("§2", "");
					rline = rline.toUpperCase();
					String[] matInfo = new String[2];
					if (rline.contains(":")){
						matInfo = rline.split(":");
						rline = matInfo[0];
					}
					boolean isValidInt = false;
					if (isInt(rline)){
						if (isMat(Integer.parseInt(rline))){
							isValidInt = true;
						}
					}
					if (isMat(rline) || isValidInt){
						it.remove();
					}
				}
			}
		}
	}

	// check if placed sign meets criteria
	@SuppressWarnings({"deprecation"})
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent p){
		Player player = p.getPlayer();
		String line = p.getLine(0);
		String rline = line;
		char[] lineChar = rline.toCharArray();
		if (charKeyExists(lineChar, 0)){
			if (Character.toString(lineChar[0]).equals("[") && Character.toString(lineChar[lineChar.length - 1]).equals("]")){
				rline = rline.replace("[", "");
				rline = rline.replace("]", "");
			}
		}
		rline = rline.toUpperCase();
		if (line.equalsIgnoreCase("[GoldBank]") || line.equalsIgnoreCase("[GB]")){
			boolean master = false;
			String node = "goldbank.sign.bank.create";
			if (p.getLine(3).equalsIgnoreCase("Master")){
				master = true;
				node = "goldbank.sign.bank.create.master";
			}
			Connection conn = null;
			Statement st = null;
			ResultSet rs = null;
			try {
				Class.forName("org.sqlite.JDBC");
				String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
				conn = DriverManager.getConnection(dbPath);
				st = conn.createStatement();
				rs = st.executeQuery("SELECT * FROM banks WHERE world = '" + p.getBlock().getWorld().getName() + "' AND x = '" + p.getBlock().getX() + "' AND y = '" + p.getBlock().getY() + "' AND z = '" + p.getBlock().getZ() + "'");
				int i = 0;
				while (rs.next()){
					i = i + 1;
				}
				if (i != 0){
					p.getPlayer().sendMessage(ChatColor.RED + "Error: One or more signs were found registered at this location. Attempting to overwrite...");
					st.executeUpdate("DELETE FROM banks WHERE world = '" + p.getBlock().getWorld().getName() + "' AND x = '" + p.getBlock().getX() + "' AND y = '" + p.getBlock().getY() + "' AND z = '" + p.getBlock().getZ() + "'");
				}
			}
			catch (Exception ex){
				ex.printStackTrace();
			}
			finally {
				try {
					conn.close();
					st.close();
					rs.close();
				}
				catch (Exception exc){
					exc.printStackTrace();
				}
			}
			if (player.hasPermission(node)){
				p.setLine(0, "§2[GoldBank]");
				if (!master){
					p.setLine(2, "§5Claim this");
					p.setLine(3, "§5sign!");
				}
				else {
					p.setLine(2, "");
					p.setLine(3, "§dMaster");
				}
				int tier = 1;
				if (p.getLine(1).length() >= 5){
					if (p.getLine(1).substring(0, 4).equalsIgnoreCase("Tier") && isInt(p.getLine(1).substring(5, 6))){
						if (getConfig().isSet("tiers." + p.getLine(1).substring(0, 4) + ".size") && getConfig().isSet("tiers." + p.getLine(1).substring(0, 4) + ".fee")){
							tier = Integer.parseInt(p.getLine(1).substring(5, 6));
							p.setLine(1, "§4Tier " + p.getLine(1).substring(5, 6));
						}
						else {
							p.setLine(1, "§4Tier 1");
						}
					}
					else {
						p.setLine(1, "§4Tier 1");
					}
				}
				else if (p.getLine(1).length() >= 1){
					if (isInt(p.getLine(1).substring(0, 1))){
						if (getConfig().isSet("tiers." + Integer.parseInt(p.getLine(1).substring(0, 1)) + ".size")){
							if (getConfig().isSet("tiers." + Integer.parseInt(p.getLine(1).substring(0, 1)) + ".fee")){
								tier = Integer.parseInt(p.getLine(1).substring(0, 1));
								p.setLine(1, "§4Tier " + p.getLine(1).substring(0, 1));
							}
							else {
								p.setLine(1, "§4Tier 1");
							}
						}
					}
					else {
						p.setLine(1, "§4Tier 1");
					}
				}
				else {
					p.setLine(1, "§4Tier 1");
				}
				if (master){
					conn = null;
					st = null;
					try {
						Class.forName("org.sqlite.JDBC");
						String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
						conn = DriverManager.getConnection(dbPath);
						st = conn.createStatement();
						st.executeUpdate("INSERT INTO banks (uuid, world, x, y, z, sign, tier) VALUES ('MASTER', '" + p.getBlock().getWorld().getName() + "', '" + p.getBlock().getX() + "', '" + p.getBlock().getY() + "', '" + p.getBlock().getZ() + "', 'true', '" + Integer.toString(tier) + "')");
					}
					catch (Exception e){
						e.printStackTrace();
					}
					finally {
						try {
							conn.close();
							st.close();
						}
						catch (Exception ex){
							ex.printStackTrace();
						}
					}
				}
			}
		}
		else if (line.equalsIgnoreCase("§2[GoldBank]")){
			if (!player.hasPermission("goldbank.sign.bank.create")){
				p.setLine(0, "[GoldBank]");
			}
		}
		else if (line.equalsIgnoreCase("[GoldATM]")){
			if (player.hasPermission("goldbank.sign.atm.create")){
				p.setLine(0, "§2[GoldATM]");
			}
		}
		else if (line.equalsIgnoreCase("§2[GoldATM]")){
			if (!player.hasPermission("goldbank.sign.atm.create")){
				p.setLine(0, "[GoldATM]");
			}
		}
		String[] matInfo = new String[2];
		String data = null;
		if (rline.contains(":")){
			matInfo = rline.split(":");
			rline = matInfo[0];
			if (matInfo.length > 1)
				data = matInfo[1];
		}
		rline = rline.replace(" ", "_");
		boolean isValidInt = false;
		if (isInt(rline)){
			if (isMat(Integer.parseInt(rline))){
				isValidInt = true;
			}
		}
		boolean pHead = false;
		if (rline.equalsIgnoreCase("PlayerHead"))
			pHead = true;
		if (isMat(rline) || isValidInt || pHead){
			String mat = "";
			if (isValidInt){
				//TODO: Magic numbers again. :P
				mat = WordUtils.capitalize(Material.getMaterial(Integer.parseInt(rline)).toString().toLowerCase());
			}
			else if (isMat(rline)){
				mat = WordUtils.capitalize(rline.toLowerCase());
			}
			else if (pHead){
				mat = "PlayerHead";
			}
			if (player.hasPermission("goldbank.sign.shop.create")){
				boolean normal = true;
				if (p.getLine(3).equalsIgnoreCase("Admin") || pHead){
					normal = false;
				}
				if (player.hasPermission("goldbank.sign.shop.create.admin") || normal){
					String[] buys = new String[]{"-1", "-1"};
					String[] sells = new String[]{"-1", "-1"};
					String buy = p.getLine(1);
					String sell = p.getLine(2);
					boolean validBuy = false;
					boolean validSell = false;
					String buyUnit = "i";
					String sellUnit = "i";
					if (buy.contains(";")){
						buy = buy.replace(" ", "");
						buys = buy.split(";");
						if (buys[1].endsWith("b")){
							buys[1] = buys[1].replace("b", "");
							buyUnit = "b";
						}
						else if (buys[1].endsWith("i")){
							buys[1] = buys[1].replace("i", "");
						}
						else if (buys[1].endsWith("n")){
							buys[1] = buys[1].replace("n", "");
							buyUnit = "n";
						}
						if (isInt(buys[0]) && isInt(buys[1])){
							if (Integer.parseInt(buys[0]) > 0 && Integer.parseInt(buys[1]) > 0)
								validBuy = true;
						}
					}
					else if (buy.length() == 0)
						validBuy = true;

					if (sell.contains(";")){
						sell = sell.replace(" ", "");
						sells = sell.split(";");
						if (sells[1].endsWith("b")){
							sells[1] = sells[1].replace("b", "");
							sellUnit = "b";
						}
						else if (sells[1].endsWith("i")){
							sells[1] = sells[1].replace("i", "");
						}
						else if (sells[1].endsWith("n")){
							sells[1] = sells[1].replace("n", "");
							sellUnit = "n";
						}
						if (isInt(sells[0]) && isInt(sells[1])){
							if (Integer.parseInt(sells[0]) > 0 && Integer.parseInt(sells[1]) > 0)
								validSell = true;
						}
					}
					else if (sell.length() == 0)
						validSell = true;
					if (validBuy && validSell){
						int dataNum = 0;
						if (data != null){
							if (isInt(data)){
								dataNum = Integer.parseInt(data);
								if (mat.equalsIgnoreCase("Wool")){
									if (dataNum == 0)
										mat = "White Wool";
									else if (dataNum == 1)
										mat = "Orange Wool";
									else if (dataNum == 2)
										mat = "Magenta Wool";
									else if (dataNum == 3)
										mat = "LBlue Wool";
									else if (dataNum == 4)
										mat = "Yellow Wool";
									else if (dataNum == 5)
										mat = "Lime Wool";
									else if (dataNum == 6)
										mat = "Pink Wool";
									else if (dataNum == 7)
										mat = "Gray Wool";
									else if (dataNum == 8)
										mat = "LGray Wool";
									else if (dataNum == 9)
										mat = "Cyan Wool";
									else if (dataNum == 10)
										mat = "Purple Wool";
									else if (dataNum == 11)
										mat = "Blue Wool";
									else if (dataNum == 12)
										mat = "Brown Wool";
									else if (dataNum == 13)
										mat = "Green Wool";
									else if (dataNum == 14)
										mat = "Red Wool";
									else if (dataNum == 15)
										mat = "Black Wool";
									else {
										mat = "White Wool";
									}
								}
							}
							else {
								data = null;
							}
						}
						Connection conn = null;
						Statement st = null;
						ResultSet rs = null;
						try {
							Class.forName("org.sqlite.JDBC");
							String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
							conn = DriverManager.getConnection(dbPath);
							st = conn.createStatement();
							boolean admin = true;
							if (normal)
								admin = false;
							String world = p.getBlock().getWorld().getName();
							int x = p.getBlock().getX();
							int y = p.getBlock().getY();
							int z = p.getBlock().getZ();
							rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE world = '" + world + "' AND x = '" + x + "' AND y = '" + y + "' AND z = '" + z + "'");
							int i = 0;
							while (rs.next())
								i = rs.getInt(1);
							if (i == 0){
								if (buys[0].length() + buys[1].length() <= 5 && sells[0].length() + sells[1].length() <= 4){
									Location chestLoc = new Location(p.getBlock().getWorld(), p.getBlock().getX(), p.getBlock().getY() - 1, p.getBlock().getZ());
									if (chestLoc.getBlock().getType() == Material.AIR || admin){
										int matId = 0;
										if (isValidInt)
											matId = Integer.parseInt(rline);
										else if (isMat(rline))
											matId = Material.getMaterial(rline).getId();
										else
											matId = -2;
										st.executeUpdate("INSERT INTO shops (creator, world, x, y, z, material, data, buyamount, buyprice, sellamount, sellprice, admin, buyunit, sellunit) VALUES (" +
												"'" + getSafeUUID(player) +
												"', '" + player.getWorld().getName() +
												"', '" + x +
												"', '" + y +
												"', '" + z +
												"', '" + matId +
												"', '" + dataNum +
												"', '" + buys[0] +
												"', '" + buys[1] +
												"', '" + sells[0] +
												"', '" + sells[1] +
												"', '" + admin +
												"', '" + buyUnit +
												"', '" + sellUnit + "')");
										rs = st.executeQuery("SELECT * FROM shops WHERE world = '" + player.getWorld().getName() + "' AND x = '" + x + "' AND y = '" + y + "' AND z = '" + z + "'");
										int shopId = rs.getInt("id");
										st.executeUpdate("INSERT INTO shoplog (shop, player, action, time) VALUES ('" + shopId + "', '" + getSafeUUID(player) + "', '2', '" + System.currentTimeMillis() / 1000 + "')");
										int dataLength = 0;
										if (dataNum != 0 && Material.getMaterial(matId) != Material.WOOL){
											dataLength = Integer.toString(dataNum).length() + 1;
										}
										if ((mat.length() + dataLength) <= 11 || Material.getMaterial(matId) == Material.WOOL){
											if (dataNum == 0 || Material.getMaterial(matId) == Material.WOOL){
												String forMat = WordUtils.capitalize(mat.replace("_", " "));
												p.setLine(0, "§2[" + forMat + "]");
											}
											else {
												String forMat = WordUtils.capitalize(mat.replace("_", " "));
												p.setLine(0, "§2[" + forMat + ":" + dataNum + "]");
											}
										}
										else {
											if (dataNum == 0 || Material.getMaterial(rline) == Material.WOOL){
												p.setLine(0, "§2[" + matId + "]");
											}
											else
												p.setLine(0, "§2[" + matId + ":" + dataNum + "]");
										}
										if  (Integer.parseInt(buys[0]) != -1 && Integer.parseInt(buys[1]) != -1){
											if (buys[0].length() + buys[1].length() <= 3)
												p.setLine(1, "§5" + "Buy " + buys[0] + " for " + buys[1] + buyUnit);
											else
												p.setLine(1, "Buy " + buys[0] + " for " + buys[1] + buyUnit);
										}
										if (Integer.parseInt(sells[0]) != -1 && Integer.parseInt(sells[1]) != -1 && !pHead){
											if (sells[0].length() + sells[1].length() <= 2)
												p.setLine(2, "§5" + "Sell " + sells[0] + " for " + sells[1] + sellUnit);
											else
												p.setLine(2, "Sell " + sells[0] + " for " + sells[1] + sellUnit);
										}
										else if (pHead)
											p.setLine(2, "");
										if (normal)
											p.setLine(3, "§9" + player.getName());
										else
											p.setLine(3, "§4Admin");
										if (normal)
											chestLoc.getBlock().setType(Material.CHEST);
										player.sendMessage(ChatColor.DARK_PURPLE + "Successfully created GoldShop sign!");
									}
									else {
										player.sendMessage(ChatColor.RED + "Error: Block below sign must be air!");
									}
								}
								else {
									if (buys[0].length() + buys[1].length() > 5)
										player.sendMessage(ChatColor.RED + "Invalid sign! The length of the buy amount plus the length of the buy price must be less than or equal to 5!");
									if (sells[0].length() + sells[1].length() > 4)
										player.sendMessage(ChatColor.RED + "Invalid sign! The length of the sell amount plus the length of the sell price must be less than or equal to 4!");
								}
							}
							else {
								player.sendMessage(ChatColor.RED + "There's somehow already a sign registered at this location. Perhaps it was WorldEdited away?");
							}
						}
						catch (Exception e){
							e.printStackTrace();
							player.sendMessage(ChatColor.RED + "An error occurred while registering your sign. Please contact a server administrator.");
						}
						finally {
							try {
								rs.close();
								st.close();
								conn.close();
							}
							catch (Exception u){
								u.printStackTrace();
							}
						}
					}
					else
						player.sendMessage(ChatColor.RED + "Invalid sign! Buy and sell signs nust contain delimiter (;) or be left blank, and prices and amounts must be integers greater than 0.");
				}
			}
		}
	}

	// call the inventory filling function
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(final PlayerJoinEvent j) throws IOException {
		Calendar cal = Calendar.getInstance();
		int dow = cal.get(Calendar.DAY_OF_WEEK);
		// check value dayofweek
		boolean invalidday = false;
		// check for invalid day value
		String daycheck = getConfig().getString("dayofweek");
		if (!daycheck.equalsIgnoreCase("Sunday") && !daycheck.equalsIgnoreCase("Monday") && !daycheck.equalsIgnoreCase("Tuesday") && !daycheck.equalsIgnoreCase("Wednesday") && !daycheck.equalsIgnoreCase("Thursday") && !daycheck.equalsIgnoreCase("Friday") && !daycheck.equalsIgnoreCase("Saturday")){
			log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"dayofweek\"! We'll take care of it..." + ANSI_WHITE);
		}
		String check = "";
		if (!invalidday){
			check = getConfig().getString("dayofweek");
			int daynum = checkDay(check);
			if (dow == daynum){
				File file = new File(getDataFolder(), "filled.txt");
				String last = readFile(getDataFolder() + File.separator + "filled.txt");
				String fill;
				fill = last.replaceAll("(\\r|\\n)", "");
				int filled = Integer.parseInt(fill);
				// Fill
				if (filled == 0){
					InventoryUtils.fill();
					PrintWriter pw = new PrintWriter(file);
					pw.print("1");
					pw.close();
				}
			}
			if (dow == daynum + 2){
				File file = new File(getDataFolder(), "filled.txt");
				String last = readFile(getDataFolder() + File.separator + "filled.txt");
				String fill;
				fill = last.replaceAll("(\\r|\\n)", "");
				int filled = Integer.parseInt(fill);
				if (filled == 1){
					InventoryUtils.fill();
					PrintWriter pw = new PrintWriter(file);
					pw.print("0");
					pw.close();
				}
			}
		}

		Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable(){ // to prevent hanging the main thread
			public void run(){
				try {
					final UUID uuid = UUIDFetcher.getUUIDOf(j.getPlayer().getName());
					Bukkit.getScheduler().runTask(GoldBank.plugin, new Runnable(){ // to prevent CMEs
						public void run(){
							GoldBank.onlineUUIDs.put(j.getPlayer().getName(), uuid);
						}
					});
				}
				catch (Exception ex){
					ex.printStackTrace();
				}
			}
		});
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e){
		onlineUUIDs.remove(e.getPlayer().getName());
	}

	// commands and stuff :D
	@SuppressWarnings({"deprecation"})
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if (commandLabel.equalsIgnoreCase("gb")){
			if (args.length >= 1){
				if (args[0].equalsIgnoreCase("reload")){
					this.reloadConfig();
					Bukkit.getPluginManager().disablePlugin(this);
					Bukkit.getPluginManager().enablePlugin(this);
					log.info(ANSI_GREEN + "has been reloaded!" + ANSI_WHITE);
					if (sender instanceof Player)
						sender.sendMessage(ChatColor.DARK_AQUA + "GoldBank has been reloaded!");
				}
				// bank
				else if (args[0].equalsIgnoreCase("bank")){
					// view
					if (args[1].equalsIgnoreCase("view")){
						if (sender instanceof Player){
							if (args.length == 2 && sender.hasPermission("goldbank.view")){
								String user = sender.getName();
								UUID userUUID = getSafeUUID(user);
								File invF = new File(getDataFolder() + File.separator + "inventories", userUUID + ".dat");
								if(invF.exists()){
									YamlConfiguration invY = new YamlConfiguration();
									try {
										invY.load(invF);
										int size = invY.getInt("size");
										Set<String> keys = invY.getKeys(false);
										ItemStack[] invI = new ItemStack[size];
										for (String invN : keys){
											if (isInt(invN)){
												int i = Integer.parseInt(invN);
												invI[i] =  invY.getItemStack(invN);
											}
										}
										Inventory inv = this.getServer().createInventory(null, size, user + "'s GoldBank Sign");
										inv.setContents(invI);
										((Player)sender).openInventory(inv);
										openPlayer[nextIndex] = userUUID;
										openingPlayer[nextIndex] = userUUID;
										openType[nextIndex] = "wallet";
										nextIndex += 1;
									}
									catch (Exception ex){
										ex.printStackTrace();
									}
								}
								else
									sender.sendMessage(ChatColor.RED + "You don't have a Bank inventory!");
							}
							else if (sender.hasPermission("goldbank.view.others")){
								String user = args[2];
								UUID userUUID = getSafeUUID(user);
								File invF = new File(getDataFolder() + File.separator + "inventories", userUUID + ".dat");
								if(invF.exists()){
									YamlConfiguration invY = new YamlConfiguration();
									try {
										invY.load(invF);
										int size = invY.getInt("size");
										Set<String> keys = invY.getKeys(false);
										ItemStack[] invI = new ItemStack[size];
										for (String invN : keys){
											if (isInt(invN)){
												int i = Integer.parseInt(invN);
												invI[i] =  invY.getItemStack(invN);
											}
										}
										Inventory inv = this.getServer().createInventory(null, size, user + "'s GoldBank Sign");
										inv.setContents(invI);
										((Player)sender).openInventory(inv);
										openPlayer[nextIndex] = userUUID;
										openingPlayer[nextIndex] = getSafeUUID(sender.getName());
										openType[nextIndex] = "bank";
										nextIndex += 1;
									}
									catch (Exception ex){
										ex.printStackTrace();
									}
								}
								else
									sender.sendMessage(ChatColor.RED + "This player doesn't have a Bank inventory!");
							}
							else
								log.info(ChatColor.RED + "You don't have permission to do this!");
						}
						else
							sender.sendMessage(ChatColor.RED + "You must be an in-game player to perform this command!");
					}
					else
						sender.sendMessage(ChatColor.RED + "Invalid argument! Usage: /gb bank [command]");
				}
				// wallet
				else if (args[0].equalsIgnoreCase("wallet")){
					if (sender instanceof Player){
						if (args.length >= 4){
							// view
							if (args[1].equalsIgnoreCase("view")){
								if (sender.hasPermission("goldbank.wallet.view")){
									String user = args[2];
									if (isInt(args[3])){
										File invF = new File(getDataFolder() + File.separator + "wallets", getSafeUUID(user) + ".dat");
										if(invF.exists()){
											YamlConfiguration invY = new YamlConfiguration();
											try {
												invY.load(invF);
												if (invY.isSet(args[3])){
													int size = invY.getInt(args[3] + ".size");
													ItemStack[] invI = new ItemStack[size];
													for (int i = 0; i < invI.length; i++){
														if (!(args[3] + "." + i).equalsIgnoreCase("size")){
															invI[i] =  invY.getItemStack(args[3] + "." + i);
														}
													}
													Inventory inv = this.getServer().createInventory(null, size, user + "'s Wallet");
													inv.setContents(invI);
													((Player)sender).openInventory(inv);
													openPlayer[nextIndex] = getSafeUUID(user);
													openingPlayer[nextIndex] = getSafeUUID(sender.getName());
													openType[nextIndex] = "wallet";
													openWalletNo[nextIndex] = Integer.parseInt(args[2]);
													nextIndex += 1;
												}
												else
													sender.sendMessage(ChatColor.RED + "Error: The wallet specified does not exist!");
											}
											catch (Exception ex){
												ex.printStackTrace();
											}
										}
										else
											sender.sendMessage(ChatColor.RED + "This player doesnt have any wallets!");
									}
									else
										sender.sendMessage(ChatColor.RED + "Error: Wallet number must be an integer!");
								}
							}
							// spawn
							else if (args[1].equalsIgnoreCase("spawn")){
								if (sender.hasPermission("goldbank.wallet.spawn")){
									if (isInt(args[3])){
										ItemStack is = new ItemStack(Material.BOOK, 1);
										ItemMeta meta = is.getItemMeta();
										meta.setDisplayName("§2Wallet");
										is.setItemMeta(meta);
										try {
											File invF = new File(getDataFolder() + File.separator + "wallets", getSafeUUID(args[2]) + ".dat");
											if (!invF.exists()){
												invF.createNewFile();
												sender.sendMessage(ChatColor.DARK_PURPLE + "Specified player does not yet have a wallets file. Attempting to create...");
											}
											YamlConfiguration invY = new YamlConfiguration();
											invY.load(invF);
											if (!invY.isSet(args[3])){
												sender.sendMessage(ChatColor.DARK_PURPLE + "Specified wallet number does not yet exist. Attempting to create...");
												invY.set(args[3] + ".size", this.getConfig().getInt("walletsize"));
												invY.save(invF);
											}
										}
										catch (Exception ex){
											ex.printStackTrace();
											sender.sendMessage(ChatColor.RED + "An error occurred while creating the wallet.");
										}
										meta = is.getItemMeta();
										List<String> lore = new ArrayList<String>();
										lore.add("Owned by");
										lore.add(args[2]);
										lore.add("§9Wallet #" + args[3]);
										lore.add("§2GoldBank");
										meta.setLore(lore);
										is.setItemMeta(meta);
										((Player)sender).getInventory().addItem(is);
										((Player)sender).updateInventory();
									}
									else
										sender.sendMessage(ChatColor.RED + "Error: Wallet number must be an integer!");
								}
								else
									sender.sendMessage(ChatColor.RED + "You don't have permission to perform this command!");
							}
							else
								sender.sendMessage(ChatColor.RED + "Invalid argument! Usage: /gb wallet [command]");
						}
						else
							sender.sendMessage(ChatColor.RED + "You don't have permisison to perform this command!");
					}
					else
						sender.sendMessage(ChatColor.RED + "You must be an in-game player to perform this command!");
				}
				else if (args[0].equalsIgnoreCase("shop")){
					if (args.length >= 2){
						if (args[1].equalsIgnoreCase("log")){
							if (sender instanceof Player){
								if (sender.hasPermission("goldbank.sign.shop.log")){
									if (args.length == 2){
										shopLog.put(((Player)sender).getName(), 0);
										sender.sendMessage(ChatColor.DARK_PURPLE + "Click a GoldShop to view its history");
									}
									else if (args[2].equalsIgnoreCase("page")){
										if (args.length >= 4){
											if (isInt(args[3])){
												if (shopLog.containsKey(sender.getName())){
													if (shopLog.get(sender.getName()) > 0){
														int shopId = shopLog.get(sender.getName());
														Connection conn = null;
														Statement st = null;
														ResultSet rs = null;
														try {
															Class.forName("org.sqlite.JDBC");
															String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
															conn = DriverManager.getConnection(dbPath);
															st = conn.createStatement();
															shopLog.put(sender.getName(), shopId);
															rs = st.executeQuery("SELECT COUNT(*) FROM shoplog WHERE shop = '" + shopId + "' AND action < '2'");
															int total = 0;
															while (rs.next()){
																total = rs.getInt(1);
															}
															if (total != 0){
																int perPage = 10;
																int pages = total / perPage;
																if (pages * perPage != total)
																	pages += 1;
																if (pages >= Integer.parseInt(args[3])){
																	int thisPage = total - ((Integer.parseInt(args[3]) - 1) * perPage);
																	sender.sendMessage(ChatColor.DARK_PURPLE + "Page " + args[3] + "/" + pages);
																	rs = st.executeQuery("SELECT * FROM shoplog WHERE shop = '" + shopId + "' AND action < '2' ORDER BY id DESC");
																	for (int i = 1; i <= (Integer.parseInt(args[3]) - 1) * perPage; i++)
																		rs.next();
																	for (int i = 1; i <= perPage; i++){
																		if (i <= thisPage){
																			String action = "";
																			ChatColor actionColor = ChatColor.DARK_GREEN;
																			if (rs.getInt("action") == 0)
																				action = "bought";
																			else if (rs.getInt("action") == 1){
																				action = "sold";
																				actionColor = ChatColor.DARK_RED;
																			}
																			String data = "";
																			if (rs.getInt("data") > 0)
																				data = ":" + rs.getInt("data");
																			Calendar cal = Calendar.getInstance();
																			cal.setTimeInMillis((long)rs.getInt("time") * 1000);
																			String month = Integer.toString(cal.get(Calendar.MONTH) + 1);
																			String day = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
																			String hour = Integer.toString(cal.get(Calendar.HOUR_OF_DAY));
																			String min = Integer.toString(cal.get(Calendar.MINUTE));
																			String sec = Integer.toString(cal.get(Calendar.SECOND));
																			if (month.length() < 2)
																				month = "0" + month;
																			if (day.length() < 2)
																				day = "0" + day;
																			while (hour.length() < 2)
																				hour = "0" + hour;
																			while (min.length() < 2)
																				min = "0" + min;
																			while (sec.length() < 2)
																				sec = "0" + sec;
																			String dateStr = cal.get(Calendar.YEAR) + "-" + month + "-" + day + " " + hour + ":" + min + ":" + sec;
																			sender.sendMessage(ChatColor.DARK_PURPLE + Integer.toString(i + ((Integer.parseInt(args[3]) - 1) * perPage)) + ") " + ChatColor.DARK_AQUA +
																					dateStr + " " + ChatColor.LIGHT_PURPLE + getSafePlayerName(UUID.fromString(rs.getString("player"))) + " " + actionColor + action + " " +
																					ChatColor.GOLD + rs.getInt("quantity") + " " + Material.getMaterial(rs.getInt("material")).toString() + data);
																			rs.next();
																		}
																		else
																			break;
																	}
																	if (Integer.parseInt(args[3]) < pages)
																		sender.sendMessage(ChatColor.DARK_PURPLE + "Type " + ChatColor.DARK_GREEN + "/gb shop log page " + (Integer.parseInt(args[3]) + 1) +
																				ChatColor.DARK_PURPLE + " to view the next page");
																}
																else
																	sender.sendMessage(ChatColor.RED + "Invalid page number!");
															}
															else
																sender.sendMessage(ChatColor.RED + "Error: The selected shop does not have any logged transactions!");
														}
														catch (Exception ex){
															ex.printStackTrace();
														}
														finally {
															try {
																rs.close();
																st.close();
																conn.close();
															}
															catch (Exception exc){
																exc.printStackTrace();
															}
														}
													}
													else
														sender.sendMessage(ChatColor.RED + "Please select a shop first!");
												}
												else
													sender.sendMessage(ChatColor.RED + "Please select a shop first!");
											}
											else
												sender.sendMessage(ChatColor.RED + "Page number must be an integer!");
										}
									}
									else
										sender.sendMessage(ChatColor.RED + "Invalid arguments! Usage: /gb shop log [page]");
								}
							}
							else
								sender.sendMessage(ChatColor.RED + "You must be an in-game player to perform this command!");
						}
						else
							sender.sendMessage(ChatColor.RED + "Invalid argument! Usage: /gb shop [command]");
					}
					else
						sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /gb shop [command]");
				}
				else
					sender.sendMessage(ChatColor.RED + "Invalid argument! Usage: /gb [command] [args]");
			}
			else if (args.length < 1)
				sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /gb [command] [args]");
			return true;
		}
		else if (commandLabel.equalsIgnoreCase("wire")){
			if (sender instanceof Player){
				if (args.length >= 2){
					String pName = sender.getName();
					if (new VaultConnector().hasAccount(pName)){
						if (new VaultConnector().hasAccount(args[0])){
							if (isInt(args[1])){
								int amount = Integer.parseInt(args[1]);
								if (BankInv.getGoldInBankInv(pName) >= amount + getConfig().getInt("wire-fee")){
									if (BankInv.removeGoldFromBankInv(pName, amount + getConfig().getInt("wire-fee"))){
										if (BankInv.addGoldToBankInv(args[0], amount)){
											sender.sendMessage(ChatColor.DARK_GREEN + "[GoldBank] Successfully wired " + amount + " gold nuggets to the account of " + args[0]);
											sender.sendMessage(ChatColor.DARK_GREEN + "[GoldBank] Charged a fee of " + getConfig().getInt("wire-fee") + " gold nuggets.");
											if (getServer().getPlayer(args[0]) != null)
												getServer().getPlayer(args[0]).sendMessage(ChatColor.DARK_GREEN + "[GoldBank] " + pName + " has wired " + amount + " gold nuggets to your GoldBank account!");
										}
										else
											sender.sendMessage(ChatColor.RED + "[GoldBank] Failed to add gold to " + args[0] + "'s GoldBank account!");
									}
									else
										sender.sendMessage(ChatColor.RED + "[GoldBank] Failed to remove gold from your GoldBank account!");
								}
								else
									sender.sendMessage(ChatColor.RED + "[GoldBank] You do not have enough gold in your GoldBank account!");
							}
							else
								sender.sendMessage(ChatColor.RED + "[GoldBank] The amount specified was not an integer!");
						}
						else
							sender.sendMessage(ChatColor.RED + "[GoldBank] The specified player does not have a GoldBank account!");
					}
					else
						sender.sendMessage(ChatColor.RED + "[GoldBank] You do not have a GoldBank account!");
				}
				else
					sender.sendMessage(ChatColor.RED + "[GoldBank] Invalid arguments! Usage: /wire {player} {amount}");
			}
			else
				sender.sendMessage(ChatColor.RED + "[GoldBank] Only ingame players may use this command!");
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent c){
		boolean check = false;
		int index = -1;
		int n = 0;
		for (n = 0; n < 256; n++){
			if (getSafeUUID(c.getPlayer().getName()) == openingPlayer[n]){
				check = true;
				index = n;
				break;
			}
		}
		if (check == true){
			try {
				String dir = "inventories";
				if (openType[index].equals("bank"))
					dir = "inventories";
				else if (openType[index].equals("wallet"))
					dir = "wallets";
				File invF = new File(getDataFolder() + File.separator + dir, openPlayer[index] + ".dat");
				if (!invF.exists()){
					invF.createNewFile();
				}
				YamlConfiguration invY = new YamlConfiguration();
				invY.load(invF);
				String root = "";
				if (openType[index].equals("wallet"))
					root = openWalletNo[index] + ".";
				Inventory inv = c.getInventory();
				for (int i = 0; i < c.getInventory().getSize(); i++){
					invY.set(root + i, inv.getItem(i));
				}
				invY.save(invF);
				openPlayer[index] = null;
				openingPlayer[index] = null;
				openType[index] = null;
				nextIndex = index;
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent d){
		if (d.getEntityType() != EntityType.PLAYER){
			if (d.getCause() == DamageCause.FALL){
				if (d.getEntity().getFallDistance() >= 10)
					d.getEntity().setMetadata("disableGoldDrop", new FixedMetadataValue(this, true));
			}
			if (this.getConfig().getBoolean("disable-drops-on-external-damage")){
				if (!(d instanceof EntityDamageByEntityEvent)){
					if (d.getEntity().getMetadata("externalDamage").isEmpty())
						d.getEntity().setMetadata("externalDamage", new FixedMetadataValue(this, d.getDamage()));
					else
						d.getEntity().setMetadata("externalDamage", new FixedMetadataValue(this, d.getEntity().getMetadata("externalDamage").get(0).asInt() + d.getDamage()));
				}
				else if (((EntityDamageByEntityEvent)d).getDamager().getType() != EntityType.PLAYER){
					if (d.getEntity().getMetadata("externalDamage").isEmpty())
						d.getEntity().setMetadata("externalDamage", new FixedMetadataValue(this, d.getDamage()));
					else
						d.getEntity().setMetadata("externalDamage", new FixedMetadataValue(this, d.getEntity().getMetadata("externalDamage").get(0).asInt() + d.getDamage()));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMobDeath(EntityDeathEvent d){
		if (d.getEntity() instanceof LivingEntity && d.getEntity().getType() != EntityType.PLAYER){
			if (d.getEntity().getKiller() != null){
				if (d.getEntity().getKiller().getType() == EntityType.PLAYER){
					if (!getConfig().getList("disable-drops-in").contains(d.getEntity().getWorld().getName())){
						boolean farm = false;
						if (getConfig().getBoolean("disablefarms") == false)
							farm = true;
						boolean spawner = false;
						if (d.getEntity().hasMetadata("disableGoldDrop"))
							spawner = true;
						boolean exDamage = false;
						if (d.getEntity().hasMetadata("externalDamage")){
							if (d.getEntity().getMetadata("externalDamage").get(0).asInt() > (d.getEntity().getMaxHealth() / 2))
								exDamage = true;
						}
						if ((!spawner || farm) && !exDamage){
							Player player = d.getEntity().getKiller();
							World world = player.getWorld();
							EntityType eType = d.getEntity().getType();
							Location mobLoc = d.getEntity().getLocation();
							Location loc = new Location(world, mobLoc.getX(), mobLoc.getY() + 1, mobLoc.getZ());
							int loot = 0;
							if (player.getItemInHand().containsEnchantment(Enchantment.LOOT_BONUS_MOBS)){
								loot = player.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
							}
							HashMap<EntityType, String> cNames = new HashMap<EntityType, String>();
							cNames.put(EntityType.CREEPER, "creeper");
							cNames.put(EntityType.ZOMBIE, "zombie");
							cNames.put(EntityType.SKELETON, "skeleton");
							cNames.put(EntityType.BLAZE, "blaze");
							cNames.put(EntityType.SPIDER, "spider");
							cNames.put(EntityType.ENDERMAN, "enderman");
							cNames.put(EntityType.WITCH, "witch");
							cNames.put(EntityType.SLIME, "slime");
							cNames.put(EntityType.MAGMA_CUBE, "magmacube");
							cNames.put(EntityType.GHAST, "ghast");
							cNames.put(EntityType.CAVE_SPIDER, "cavespider");
							cNames.put(EntityType.ENDER_DRAGON, "enderdragon");
							cNames.put(EntityType.PIG_ZOMBIE, "zombiepigman");
							cNames.put(EntityType.SILVERFISH, "silverfish");
							cNames.put(EntityType.WITHER_SKULL, "witherskeleton");
							cNames.put(EntityType.WITHER, "wither");
							cNames.put(EntityType.PIG, "pig");
							cNames.put(EntityType.COW, "cow");
							cNames.put(EntityType.MUSHROOM_COW, "mooshroomcow");
							cNames.put(EntityType.CHICKEN, "chicken");
							cNames.put(EntityType.SQUID, "squid");
							cNames.put(EntityType.SHEEP, "sheep");
							cNames.put(EntityType.SNOWMAN, "snowgolem");
							cNames.put(EntityType.IRON_GOLEM, "irongolem");
							cNames.put(EntityType.OCELOT, "ocelot");
							cNames.put(EntityType.BAT, "bat");
							cNames.put(EntityType.WOLF, "wolf");
							cNames.put(EntityType.GIANT, "giant");

							dropItems(cNames.get(eType), world, loc, loot);
							if (this.getConfig().getDouble("rare-drop-rate") != 0 && this.getConfig().getInt("mobdrops." + cNames.get(eType)) != 0 && !this.getConfig().getList("disable-rare-drops-for").contains(cNames.get(eType))){
								double rand = Math.random();
								if (rand <= this.getConfig().getDouble("rare-drop-rate")){
									List<Material> rareGold = new ArrayList<Material>();
									rareGold.add(Material.GOLD_INGOT);
									rareGold.add(Material.GOLD_BLOCK);
									rareGold.add(Material.GOLD_PICKAXE);
									rareGold.add(Material.GOLD_SWORD);
									rareGold.add(Material.GOLD_SPADE);
									rareGold.add(Material.GOLD_AXE);
									rareGold.add(Material.GOLD_HOE);
									rareGold.add(Material.GOLD_HELMET);
									rareGold.add(Material.GOLD_CHESTPLATE);
									rareGold.add(Material.GOLD_LEGGINGS);
									rareGold.add(Material.GOLD_BOOTS);
									rareGold.add(Material.GOLDEN_CARROT);
									rareGold.add(Material.GOLDEN_APPLE);
									int min2 = 0;
									int max2 = rareGold.size() - 1;
									int rand2 = min2 + (int)(Math.random() * ((max2 - min2) + 1));
									boolean uberApple = false;
									if (rareGold.get(rand2) == Material.GOLDEN_APPLE){
										int min3 = 1;
										int max3 = 10;
										int rand3 = min3 + (int)(Math.random() * (max3 - min3) + 1);
										if (rand3 == 1)
											uberApple = true;
									}
									ItemStack rareDrop = new ItemStack(rareGold.get(rand2), 1);
									if (uberApple)
										rareDrop.setDurability((short)1);
									world.dropItem(loc, rareDrop);
								}
							}
						}
					}
				}
			}
		}
	}

	public void dropItems(String cName, World world, Location loc, int loot){
		int max = getConfig().getInt("mobdrops." + cName);
		if (max != 0){
			max = max + loot;
		}
		int amount = (int)(Math.random() * (max + 1));
		if (amount != 0){
			world.dropItem(loc, new ItemStack(Material.GOLD_NUGGET, amount));
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent r){
		Block block = r.getRetractLocation().getBlock();
		if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST){
			Sign sign = (Sign)block.getState();
			if (sign.getLine(0).equalsIgnoreCase("§2[GoldBank]")){
				r.setCancelled(true);
			}
			else if (sign.getLine(0).equalsIgnoreCase("§2[GoldATM]")){	
				r.setCancelled(true);
			}
			String line = sign.getLine(0);
			String rline = line.replace("[", "");
			rline = rline.replace("]", "");
			rline = rline.replace("§2", "");
			rline = rline.toUpperCase();
			String[] matInfo = new String[2];
			if (rline.contains(":")){
				matInfo = rline.split(":");
				rline = matInfo[0];
			}
			boolean isValidInt = false;
			if (isInt(rline)){
				if (isMat(Integer.parseInt(rline))){
					isValidInt = true;
				}
			}
			if (isMat(rline) || isValidInt){
				r.setCancelled(true);
			}
		}
		if (getAdjacentBlock(block, Material.WALL_SIGN) != null || getAdjacentBlock(r.getBlock(), Material.SIGN_POST) != null){
			Block adjblock = null;
			if (getAdjacentBlock(block, Material.WALL_SIGN) != null){
				adjblock = getAdjacentBlock(block, Material.WALL_SIGN);
			}
			else if (getAdjacentBlock(block, Material.SIGN_POST)!= null){
				adjblock = getAdjacentBlock(block, Material.SIGN_POST);
			}
			Sign sign = (Sign)adjblock.getState();
			if (sign.getLine(0).equalsIgnoreCase("§2[GoldBank]")){
				r.setCancelled(true);
			}
			else if (sign.getLine(0).equalsIgnoreCase("§2[GoldATM]")){
				r.setCancelled(true);
			}
			String line = sign.getLine(0);
			String rline = line.replace("[", "");
			rline = rline.replace("]", "");
			rline = rline.replace("§2", "");
			rline = rline.toUpperCase();
			String[] matInfo = new String[2];
			if (rline.contains(":")){
				matInfo = rline.split(":");
				rline = matInfo[0];
			}
			boolean isValidInt = false;
			if (isInt(rline)){
				if (isMat(Integer.parseInt(rline))){
					isValidInt = true;
				}
			}
			if (isMat(rline) || isValidInt){
				r.setCancelled(true);
			}
		}
		else if (getAdjacentBlock(block, Material.SIGN_POST) != null){
			Block adjblock = getAdjacentBlock(block, Material.SIGN_POST);
			Sign sign = (Sign)adjblock.getState();
			if (sign.getLine(0).equalsIgnoreCase("§2[GoldBank]")){
				r.setCancelled(true);
			}
			else if (sign.getLine(0).equalsIgnoreCase("§2[GoldATM]")){
				r.setCancelled(true);
			}
			String line = sign.getLine(0);
			String rline = line.replace("[", "");
			rline = rline.replace("]", "");
			rline = rline.replace("§2", "");
			rline = rline.toUpperCase();
			String[] matInfo = new String[2];
			if (rline.contains(":")){
				matInfo = rline.split(":");
				rline = matInfo[0];
			}
			boolean isValidInt = false;
			if (isInt(rline)){
				if (isMat(Integer.parseInt(rline))){
					isValidInt = true;
				}
			}
			if (isMat(rline) || isValidInt){
				r.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent s){
		if (s.getSpawnReason() == SpawnReason.SPAWNER)
			s.getEntity().setMetadata("disableGoldDrop", new FixedMetadataValue(this, true));
	}

	@EventHandler
	public void onCraftPrepare(PrepareItemCraftEvent e){
		ItemStack is = new ItemStack(Material.BOOK, 1);
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName("§2Wallet");
		is.setItemMeta(meta);
		if (e.getRecipe() instanceof ShapedRecipe){
			if (((ShapedRecipe)e.getRecipe()).getResult().equals(is)){
				if (e.getViewers().get(0).hasPermission("goldbank.wallet.craft")){
					try {
						File invF = new File(getDataFolder() + File.separator + "wallets", ((Player)e.getViewers().get(0)).getName() + ".dat");
						if (!invF.exists()){
							invF.createNewFile();
						}
						YamlConfiguration invY = new YamlConfiguration();
						invY.load(invF);
						int nextKey = 1;
						while (invY.isSet(Integer.toString(nextKey))){
							nextKey += 1;
						}
						List<String> lore = new ArrayList<String>();
						lore.add("Owned by");
						lore.add(e.getViewers().get(0).getName());
						lore.add("§9Wallet #" + nextKey);
						lore.add("§2GoldBank");
						meta.setLore(lore);
						is.setItemMeta(meta);
						e.getInventory().setResult(is);
					}
					catch (Exception ex){
						ex.printStackTrace();
						boolean cookie = this.getConfig().getBoolean("give-cookie-if-wallet-creation-fails");
						String msg = "An error occurred while loading the next available key for your wallet.";
						if (cookie){
							msg = "An error occurred while loading the next available key for your wallet. Here's a cookie to make up for it... :)";
							((Player)e.getViewers().get(0)).getInventory().addItem(new ItemStack(Material.COOKIE, 1));
						}
						((Player)e.getViewers().get(0)).sendMessage(ChatColor.RED + msg);
					}
				}
				else {
					e.getInventory().setResult(null);
					((Player)e.getViewers().get(0)).sendMessage(ChatColor.RED + "You don't have permission to craft a wallet!");
				}
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e){
		if (e instanceof CraftItemEvent){
			ItemStack is = new ItemStack(Material.BOOK, 1);
			ItemMeta meta = is.getItemMeta();
			meta.setDisplayName("§2Wallet");
			is.setItemMeta(meta);
			if (((CraftItemEvent)e).getRecipe().getResult().equals(is)){
				if (e.isShiftClick())
					e.setCancelled(true);
				else {
					try {
						File invF = new File(getDataFolder() + File.separator + "wallets", ((Player)e.getViewers().get(0)).getName() + ".dat");
						if (!invF.exists()){
							invF.createNewFile();
						}
						YamlConfiguration invY = new YamlConfiguration();
						invY.load(invF);
						int nextKey = 1;
						while (invY.isSet(Integer.toString(nextKey))){
							nextKey += 1;
						}
						invY.set(nextKey + ".size", this.getConfig().getInt("walletsize"));
						invY.save(invF);
					}
					catch (Exception ex){
						ex.printStackTrace();
						boolean cookie = this.getConfig().getBoolean("give-cookie-if-wallet-creation-fails");
						String msg = "An error occurred while creating your wallet.";
						if (cookie){
							msg = "An error occurred while creating your wallet. Here's a cookie to make up for it. :)";
							((Player)e.getViewers().get(0)).getInventory().addItem(new ItemStack(Material.COOKIE, 1));
						}
						((Player)e.getViewers().get(0)).sendMessage(ChatColor.RED + msg);
					}
				}
			}
		}
		if (!e.getViewers().isEmpty()){
			if (((Player)e.getViewers().get(0)).getGameMode() != GameMode.CREATIVE){
				if (e.getInventory().getType() == InventoryType.CHEST){
					String p = ((Player)e.getViewers().get(0)).getName();
					int index = -1;
					for (int i = 0; i < openingPlayer.length; i++){
						if (openingPlayer[i] != null){
							if (openingPlayer[i].equals(p)){
								index = i;
								break;
							}
						}
					}
					if (index != -1){
						if (openType[index].equals("wallet")){
							if (getConfig().getBoolean("only-gold-in-wallets")){
								if (!(e.getCursor().getType() == Material.GOLD_BLOCK || e.getCurrentItem().getType() == Material.GOLD_BLOCK) && 
										!(e.getCursor().getType() == Material.GOLD_INGOT || e.getCurrentItem().getType() == Material.GOLD_INGOT) && 
										!(e.getCursor().getType() == Material.GOLD_NUGGET || e.getCurrentItem().getType() == Material.GOLD_NUGGET)){
									e.setCancelled(true);
								}
							}
							if (e.getCurrentItem().getType() == Material.BOOK || e.getCursor().getType() == Material.BOOK){
								ItemStack is = null;
								if (e.getCurrentItem().getType() == Material.BOOK){
									is = e.getCurrentItem();
								}
								else if (e.getCurrentItem().getType() == Material.BOOK){
									is = e.getCursor();
								}
								ItemMeta meta = is.getItemMeta();
								if (meta.getDisplayName().equals("§2Wallet"))
									e.setCancelled(true);
							}
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onInventoryMoveItem(InventoryMoveItemEvent e){
		if (e.getSource().getType() == InventoryType.CHEST){
			if (e.getSource().getHolder() != null){
				if (e.getSource().getHolder() instanceof Chest){
					Block chest = ((Chest)e.getSource().getHolder()).getBlock();
					Location l = chest.getLocation();
					l.setY(l.getY() + 1);
					if (l.getBlock().getType() == Material.WALL_SIGN){
						Connection conn = null;
						Statement st = null;
						ResultSet rs = null;
						int i = 0;
						try {
							Class.forName("org.sqlite.JDBC");
							String dbPath = "jdbc:sqlite:" + this.getDataFolder() + File.separator + "data.db";
							conn = DriverManager.getConnection(dbPath);
							st = conn.createStatement();
							rs = st.executeQuery("SELECT COUNT(*) FROM shops WHERE world = '" + l.getWorld().getName() + "' AND x = '" + l.getX() + "' AND y = '" + l.getY() + "' AND z = '" + l.getZ() + "'");
							while (rs.next()){
								i = rs.getInt(1);
							}
						}
						catch (Exception q){
							q.printStackTrace();
						}
						finally {
							try {
								conn.close();
								st.close();
								rs.close();
							}
							catch (Exception k){
								k.printStackTrace();
							}
						}
						if (i > 0)
							e.setCancelled(true);
					}
				}
			}
		}
	}
}
