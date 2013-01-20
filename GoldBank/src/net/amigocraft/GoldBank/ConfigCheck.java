package net.amigocraft.GoldBank;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
		if (!plugin.getConfig().isSet("interest")){
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

		// check ALL of the mobdrop values *groan*
		if (!plugin.getConfig().isSet("mobdrops.creeper") || !isInt(plugin.getConfig().getString("mobdrops.creeper")) || plugin.getConfig().getInt("mobdrops.creeper") > 256 || plugin.getConfig().getInt("mobdrops.creeper") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.creeper\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.creeper", 3);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.skeleton") || !isInt(plugin.getConfig().getString("mobdrops.skeleton")) || plugin.getConfig().getInt("mobdrops.skeleton") > 256 || plugin.getConfig().getInt("mobdrops.skeleton") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.skeleton\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.skeleton", 2);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.zombie") || !isInt(plugin.getConfig().getString("mobdrops.zombie")) || plugin.getConfig().getInt("mobdrops.zombie") > 256 || plugin.getConfig().getInt("mobdrops.zombie") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.zombie\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.zombie", 2);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.spider") || !isInt(plugin.getConfig().getString("mobdrops.spider")) || plugin.getConfig().getInt("mobdrops.spider") > 256 || plugin.getConfig().getInt("mobdrops.spider") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.spider\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.spider", 2);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.blaze") || !isInt(plugin.getConfig().getString("mobdrops.blaze")) || plugin.getConfig().getInt("mobdrops.blaze") > 256 || plugin.getConfig().getInt("mobdrops.blaze") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.blaze\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.blaze", 3);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.enderman") || !isInt(plugin.getConfig().getString("mobdrops.enderman")) || plugin.getConfig().getInt("mobdrops.enderman") > 256 || plugin.getConfig().getInt("mobdrops.enderman") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.enderman\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.enderman", 3);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.witch") || !isInt(plugin.getConfig().getString("mobdrops.witch")) || plugin.getConfig().getInt("mobdrops.witch") > 256 || plugin.getConfig().getInt("mobdrops.witch") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.witch\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.witch", 32);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.slime") || !isInt(plugin.getConfig().getString("mobdrops.slime")) || plugin.getConfig().getInt("mobdrops.slime") > 256 || plugin.getConfig().getInt("mobdrops.slime") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.slime\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.slime", 1);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.magmacube") || !isInt(plugin.getConfig().getString("mobdrops.magmacube")) || plugin.getConfig().getInt("mobdrops.magmacube") > 256 || plugin.getConfig().getInt("mobdrops.magmacube") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.magmacube\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.magmacube", 1);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.ghast") || !isInt(plugin.getConfig().getString("mobdrops.ghast")) || plugin.getConfig().getInt("mobdrops.ghast") > 256 || plugin.getConfig().getInt("mobdrops.ghast") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.ghast\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.ghast", 9);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.cavespider") || !isInt(plugin.getConfig().getString("mobdrops.cavespider")) || plugin.getConfig().getInt("mobdrops.cavespider") > 256 || plugin.getConfig().getInt("mobdrops.cavespider") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.cavespider\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.cavespider", 3);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.enderdragon") || !isInt(plugin.getConfig().getString("mobdrops.enderdragon")) || plugin.getConfig().getInt("mobdrops.enderdragon") > 256 || plugin.getConfig().getInt("mobdrops.enderdragon") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.enderdragon\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.enderdragon", 128);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.zombiepigman") || !isInt(plugin.getConfig().getString("mobdrops.zombiepigman")) || plugin.getConfig().getInt("mobdrops.zombiepigman") > 256 || plugin.getConfig().getInt("mobdrops.zombiepigman") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.zombiepigman\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.zombiepigman", 1);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.silverfish") || !isInt(plugin.getConfig().getString("mobdrops.silverfish")) || plugin.getConfig().getInt("mobdrops.silverfish") > 256 || plugin.getConfig().getInt("mobdrops.silverfish") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.silverfish\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.silverfish", 2);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.witherskeleton") || !isInt(plugin.getConfig().getString("mobdrops.witherskeleton")) || plugin.getConfig().getInt("mobdrops.witherskeleton") > 256 || plugin.getConfig().getInt("mobdrops.witherskeleton") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.witherskeleton\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.witherskeleton", 2);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.wither") || !isInt(plugin.getConfig().getString("mobdrops.wither")) || plugin.getConfig().getInt("mobdrops.wither") > 256 || plugin.getConfig().getInt("mobdrops.wither") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.wither\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.wither", 128);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.pig") || !isInt(plugin.getConfig().getString("mobdrops.pig")) || plugin.getConfig().getInt("mobdrops.pig") > 256 || plugin.getConfig().getInt("mobdrops.pig") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.pig\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.pig", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.cow") || !isInt(plugin.getConfig().getString("mobdrops.cow")) || plugin.getConfig().getInt("mobdrops.cow") > 256 || plugin.getConfig().getInt("mobdrops.cow") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.cow\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.cow", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.mooshroom") || !isInt(plugin.getConfig().getString("mobdrops.mooshroom")) || plugin.getConfig().getInt("mobdrops.mooshroom") > 256 || plugin.getConfig().getInt("mobdrops.mooshroom") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.mooshroom\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.mooshroom", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.chicken") || !isInt(plugin.getConfig().getString("mobdrops.chicken")) || plugin.getConfig().getInt("mobdrops.chicken") > 256 || plugin.getConfig().getInt("mobdrops.chicken") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.chicken\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.chicken", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.squid") || !isInt(plugin.getConfig().getString("mobdrops.squid")) || plugin.getConfig().getInt("mobdrops.squid") > 256 || plugin.getConfig().getInt("mobdrops.squid") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.squid\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.squid", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.sheep") || !isInt(plugin.getConfig().getString("mobdrops.sheep")) || plugin.getConfig().getInt("mobdrops.sheep") > 256 || plugin.getConfig().getInt("mobdrops.sheep") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.sheep\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.sheep", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.snowgolem") || !isInt(plugin.getConfig().getString("mobdrops.snowgolem")) || plugin.getConfig().getInt("mobdrops.snowgolem") > 256 || plugin.getConfig().getInt("mobdrops.snowgolem") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.snowgolem\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.snowgolem", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.irongolem") || !isInt(plugin.getConfig().getString("mobdrops.irongolem")) || plugin.getConfig().getInt("mobdrops.irongolem") > 256 || plugin.getConfig().getInt("mobdrops.irongolem") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.irongolem\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.irongolem", 5);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.ocelot") || !isInt(plugin.getConfig().getString("mobdrops.ocelot")) || plugin.getConfig().getInt("mobdrops.ocelot") > 256 || plugin.getConfig().getInt("mobdrops.ocelot") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.ocelot\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.ocelot", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.bat") || !isInt(plugin.getConfig().getString("mobdrops.bat")) || plugin.getConfig().getInt("mobdrops.bat") > 256 || plugin.getConfig().getInt("mobdrops.bat") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.bat\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.bat", 0);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.wolf") || !isInt(plugin.getConfig().getString("mobdrops.wolf")) || plugin.getConfig().getInt("mobdrops.wolf") > 256 || plugin.getConfig().getInt("mobdrops.wolf") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.wolf\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.wolf", 1);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("mobdrops.giant") || !isInt(plugin.getConfig().getString("mobdrops.giant")) || plugin.getConfig().getInt("mobdrops.giant") > 256 || plugin.getConfig().getInt("mobdrops.giant") < 0){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"mobdrops.giant\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("mobdrops.giant", 32);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("disablefarms") || !isBool(plugin.getConfig().getString("disablefarms"))){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"disablefarms\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("disablefarms", true);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("selldamageditems") || !isBool(plugin.getConfig().getString("selldamageditems"))){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"selldamageditems\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("selldamageditems", false);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isSet("give-cookie-if-wallet-creation-fails") || !isBool(plugin.getConfig().getString("give-cookie-if-wallet-creation-fails"))){
			GoldBank.log.info(ANSI_RED + "Error detected in config value \"give-cookie-if-wallet-creation-fails\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("give-cookie-if-wallet-creation-fails", false);
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
		if (!plugin.getConfig().isBoolean("disable-drops-on-external-damage") || !plugin.getConfig().isSet("disable-drops-on-external-damage")){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"disable-drops-on-external-damage\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("disable-drops-on-external-damage", false);
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
		}
		if (!plugin.getConfig().isBoolean("enable-auto-update") || !plugin.getConfig().isSet("enable-auto-update")){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"enable-auto-update\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("enable-auto-update", true);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
		}
		if (!plugin.getConfig().isBoolean("enable-metrics") || !plugin.getConfig().isSet("enable-metrics")){
			GoldBank.log.warning(ANSI_RED + "[GoldBank] Error detected in config value \"enable-metrics\"! We'll take care of it..." + ANSI_WHITE);
			plugin.getConfig().set("enable-metrics", true);
			plugin.getConfig().options().header(header);
			plugin.getConfig().options().copyHeader(false);
			plugin.saveConfig();
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