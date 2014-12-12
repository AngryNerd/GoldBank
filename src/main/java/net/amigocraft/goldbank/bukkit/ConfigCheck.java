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

import java.io.File;
import java.io.InputStream;

import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigCheck {
	static GoldBank plugin = GoldBank.plugin;
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static String header = "########################## #\n# GoldBank Configuration # #\n########################## #";
	@SuppressWarnings("deprecation")
	public static void check(){
		// create the default config
		if(!(new File(plugin.getDataFolder(), "config.yml")).exists()) {
			plugin.saveDefaultConfig();
		}

		YamlConfiguration y = new YamlConfiguration();
		try {
			y.load(new File(plugin.getDataFolder() + File.separator + "config.yml"));
			YamlConfiguration defConfig = new YamlConfiguration();
			InputStream defStream = plugin.getResource("config.yml");
			if(defStream != null){
				defConfig = YamlConfiguration.loadConfiguration(defStream); // I have no idea what this does so I'm going to leave the deprecated method
				for (String key : y.getKeys(true)){
					if (!plugin.getConfig().isSet(key) || !validate(key)){
						plugin.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"" + key + "\"! We'll take care of it..." + ANSI_WHITE);
						plugin.getConfig().set(key, defConfig.get(key));
						plugin.getConfig().options().header(header);
						plugin.getConfig().options().copyHeader(false);
						plugin.saveConfig();
					}
				}
			}
			else
				plugin.log.info(GoldBank.ANSI_GREEN + "[GoldBank] " + ANSI_RED + "Error checking config values!" + ANSI_WHITE);
		}
		catch (Exception e){
			plugin.log.info(ANSI_RED + "Error checking config values!" + ANSI_WHITE);
		}
	}

	@SuppressWarnings("deprecation")
	public static boolean validate(String key){
		if (key.equals("interest")){
			double interest = plugin.getConfig().getDouble("interest");
			if (interest < 0 || interest > 1){
				return false;
			}
		}
		// check value dayofweek
		else if (key.equals("dayofweek")){
			String daycheck = plugin.getConfig().getString("dayofweek");
			if (!daycheck.equalsIgnoreCase("Sunday") && !daycheck.equalsIgnoreCase("Monday") && !daycheck.equalsIgnoreCase("Tuesday") && !daycheck.equalsIgnoreCase("Wednesday") && !daycheck.equalsIgnoreCase("Thursday") && !daycheck.equalsIgnoreCase("Friday") && !daycheck.equalsIgnoreCase("Saturday")){
				return false;
			}
		}
		// check values in tiers
		if (key.length() >= 5)
			if (key.substring(0, 4).equals("tiers")){
				int fee = plugin.getConfig().getInt("tiers.1.fee");
				if (fee < 0 || fee > 64){
					return false;
				}
				fee = plugin.getConfig().getInt("tiers.2.fee");
				if (fee < 0 || fee > 64){
					return false;
				}
				fee = plugin.getConfig().getInt("tiers.3.fee");
				if (fee < 0 || fee > 64){
					return false;
				}
				int size = plugin.getConfig().getInt("tiers.1.size");
				if (size < 9 || size > 54 || size / 9 != Math.round(size / 9)){
					return false;
				}
				size = plugin.getConfig().getInt("tiers.2.size");
				if (size < 9 || size > 54 || size / 9 != Math.round(size / 9)){
					return false;
				}
				size = plugin.getConfig().getInt("tiers.3.size");
				if (size < 9 || size > 54 || size / 9 != Math.round(size / 9)){
					return false;
				}
			}
			else if (key.equals("atmfee")){
				int atmfee = plugin.getConfig().getInt("atmfee");
				if (atmfee < 0){
					return false;
				}
			}
			else if (key.equals("walletsize")){
				int walletSize = plugin.getConfig().getInt("walletsize");
				if (walletSize < 9 || walletSize > 54 || walletSize / 9 != Math.round(walletSize / 9)){
					return false;
				}
			}
			else if (key.equals("rare-drop-rate")){
				double raredroprate = plugin.getConfig().getDouble("rare-drop-rate");
				if (raredroprate < 0 || raredroprate > 1){
					return false;
				}
			}
			else if (key.equals("disable-rare-drops-for")){
				if (!plugin.getConfig().isList("disable-rare-drops-for") || !plugin.getConfig().isSet("disable-rare-drops-for")){
					return false;
				}
			}
			else if (key.equals("wire-fee")){
				if (!plugin.getConfig().isInt("wire-fee")){
					return false;
				}
			}

		YamlConfiguration defConfig = null;
		InputStream defStream = plugin.getResource("config.yml");
		if(defStream != null){
			defConfig = YamlConfiguration.loadConfiguration(defStream);
			if (!plugin.getConfig().isBoolean(key) && defConfig.isBoolean(key))
				return false;
			if (!plugin.getConfig().isInt(key) && defConfig.isInt(key))
				return false;
			if (!plugin.getConfig().isList(key) && defConfig.isList(key))
				return false;
			if (!plugin.getConfig().isDouble(key) && defConfig.isDouble(key) && !plugin.getConfig().isInt(key))
				return false;
		}
		return true;
	}

	public static boolean isInt(String i){
		try {
			Integer.parseInt(i);
			return true;
		}
		catch(NumberFormatException nfe){
			return false;
		}
	}

	public static boolean isBool(String b){
		try {
			Boolean.parseBoolean(b);
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}