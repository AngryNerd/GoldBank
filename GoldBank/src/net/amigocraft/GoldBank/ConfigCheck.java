package net.amigocraft.GoldBank;

import java.io.File;
import java.io.InputStream;

import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigCheck {
	static GoldBank plugin = GoldBank.plugin;
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static String header = "########################## #\n# GoldBank Configuration # #\n########################## #";
	public static void check(){
		// create the default config
		if(!(new File(plugin.getDataFolder(), "config.yml")).exists()) {
			plugin.saveDefaultConfig();
		}

		// check if values are set
		/*if (!plugin.getConfig().isSet("interest")){
			GoldBank.log.info(ANSI_RED + "[GoldBank] Config value \"interest\" not found. Automatically adding..." + ANSI_WHITE);
			plugin.getConfig().set("interest", 0.03);
			plugin.getConfig().options().header(header);
			GoldBank.plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("dayofweek")){
			GoldBank.log.info(ANSI_RED + "[GoldBank] Config value \"dayofweek\" not found. Automatically adding..." + ANSI_WHITE);
			plugin.getConfig().set("dayofweek", "Sunday");
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}

		// check value interest
		double interest = plugin.getConfig().getDouble("interest");
		if (interest < 0 || interest > 1){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"interest\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("interest", 0.03);
			plugin.getConfig().options().header(header);

			plugin.getConfig().options().copyHeader(false);plugin.saveConfig();
		}
		// check value dayofweek
		String daycheck = plugin.getConfig().getString("dayofweek");
		if (!daycheck.equalsIgnoreCase("Sunday") && !daycheck.equalsIgnoreCase("Monday") && !daycheck.equalsIgnoreCase("Tuesday") && !daycheck.equalsIgnoreCase("Wednesday") && !daycheck.equalsIgnoreCase("Thursday") && !daycheck.equalsIgnoreCase("Friday") && !daycheck.equalsIgnoreCase("Saturday")){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"dayofweek\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("dayofweek", "Sunday");
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		// check values in tiers
		int fee = plugin.getConfig().getInt("tiers.1.fee");
		if (!plugin.getConfig().isSet("tiers.1.fee") || fee < 0 || fee > 64){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"tiers.1.fee\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("tiers.1.fee", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		fee = plugin.getConfig().getInt("tiers.2.fee");
		if (!plugin.getConfig().isSet("tiers.2.fee") || fee < 0 || fee > 64){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"tiers.2.fee\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("tiers.2.fee", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		fee = plugin.getConfig().getInt("tiers.3.fee");
		if (!plugin.getConfig().isSet("tiers.3.fee") || fee < 0 || fee > 64){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"tiers.3.fee\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("tiers.3.fee", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		int size = plugin.getConfig().getInt("tiers.1.size");
		if (size < 9 || size > 54 || size / 9 != Math.round(size / 9)){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"tiers.1.size\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("tiers.1.size", 18);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		size = plugin.getConfig().getInt("tiers.2.size");
		if (size < 9 || size > 54 || size / 9 != Math.round(size / 9)){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"tiers.2.size\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("tiers.2.size", 36);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		size = plugin.getConfig().getInt("tiers.3.size");
		if (size < 9 || size > 54 || size / 9 != Math.round(size / 9)){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"tiers.3.size\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("tiers.3.size", 54);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}

		int atmfee = plugin.getConfig().getInt("atmfee");
		if (!plugin.getConfig().isSet("atmfee") || atmfee < 0){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"atmfee\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("atmfee", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		int walletSize = plugin.getConfig().getInt("walletsize");
		if (walletSize < 9 || walletSize > 54 || walletSize / 9 != Math.round(walletSize / 9)){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"walletsize\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("walletsize", 9);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		double raredroprate = plugin.getConfig().getDouble("rare-drop-rate");
		if (raredroprate < 0 || raredroprate > 1 || !plugin.getConfig().isSet("rare-drop-rate")){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"rare-drop-rate\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("rare-drop-rate", 0.01);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isList("disable-rare-drops-for") || !plugin.getConfig().isSet("disable-rare-drops-for")){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"disable-rare-drops-for\"! We'll take care of it..." + ANSI_WHITE);
			List<String> exList = new ArrayList<String>();
			exList.add("mob1");
			exList.add("mob2");
			plugin.getConfig().set("disable-rare-drops-for", exList);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}*/
		YamlConfiguration y = new YamlConfiguration();
		try {
			y.load(new File(plugin.getDataFolder() + File.separator + "config.yml"));
			YamlConfiguration defConfig = new YamlConfiguration();
			InputStream defStream = plugin.getResource("config.yml");
			if(defStream != null){
				defConfig = YamlConfiguration.loadConfiguration(defStream);
				for (String key : y.getKeys(true)){
					boolean invalid = false;
					if (isBool(defConfig.getString(key)) && !isBool(plugin.getConfig().getString(key)))
						invalid = true;
					if (!plugin.getConfig().isSet(key) || invalid){
						GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"" + key + "\"! We'll take care of it..." + ANSI_WHITE);
						plugin.getConfig().set(key, defConfig.get(key));
						plugin.getConfig().options().header(header);
						plugin.getConfig().options().copyHeader(false);
						plugin.saveConfig();
					}
				}
			}
			else
				GoldBank.log.info(GoldBank.ANSI_GREEN + "[GoldBank] " + ANSI_RED + "Error checking config values!" + ANSI_WHITE);
		}
		catch (Exception e){
			GoldBank.log.info(ANSI_RED + "Error checking config values!" + ANSI_WHITE);
		}
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