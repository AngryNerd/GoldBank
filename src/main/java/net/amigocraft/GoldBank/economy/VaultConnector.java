package net.amigocraft.GoldBank.economy;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.bukkit.OfflinePlayer;

import net.amigocraft.GoldBank.GoldBank;
import net.amigocraft.GoldBank.api.BankInv;
import net.amigocraft.GoldBank.util.MiscUtils;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

public class VaultConnector implements Economy {
	
	private GoldBank plugin = GoldBank.plugin;
	
	public VaultConnector(){}

	@Override
	public EconomyResponse bankBalance(String arg0){
		return new EconomyResponse(0,0, ResponseType.NOT_IMPLEMENTED, "This feature is not implemented in GoldBank.");
	}

	@Override
	public EconomyResponse bankDeposit(String arg0, double arg1){
		return new EconomyResponse(0,0, ResponseType.NOT_IMPLEMENTED, "This feature is not implemented in GoldBank.");
	}

	@Override
	public EconomyResponse bankHas(String arg0, double arg1){
		return new EconomyResponse(0,0, ResponseType.NOT_IMPLEMENTED, "This feature is not implemented in GoldBank.");
	}

	@Override
	public EconomyResponse bankWithdraw(String player, double amount){
		return new EconomyResponse(0,0, ResponseType.NOT_IMPLEMENTED, "This feature is not implemented in GoldBank.");
	}

	@Override
	public EconomyResponse createBank(String arg0, String arg1){
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "This feature is not implemented in GoldBank.");
	}

	@Override
	public boolean createPlayerAccount(String arg0){
		return false;
	}

	@Override
	public String currencyNamePlural(){
		return "gold";
	}

	@Override
	public String currencyNameSingular(){
		return "gold";
	}

	@Override
	public EconomyResponse deleteBank(String arg0){
		return new EconomyResponse(0,0, ResponseType.NOT_IMPLEMENTED, "This feature is not implemented in GoldBank.");
	}

	@Override
	public EconomyResponse depositPlayer(String player, double amount){
		boolean success = BankInv.addGoldToBankInv(player, (int)amount);
		if (success)
			return new EconomyResponse(0, 0, ResponseType.SUCCESS, "Deposited " + amount + " gold in " + player + "'s GoldBank account.");
		else
			return new EconomyResponse(0, 0, ResponseType.FAILURE, "Failed to deposit " + amount + " gold in " + player + "'s GoldBank account.");
	}

	@Override
	public String format(double amount){
		return amount + " gold";
	}

	@Override
	public int fractionalDigits(){
		return 0;
	}

	@Override
	public double getBalance(String player){
		return (double)BankInv.getGoldInBankInv(player);
	}

	@Override
	public List<String> getBanks(){
		return null;
	}

	@Override
	public String getName(){
		return "GoldBank";
	}

	@Override
	public boolean has(String player, double amount){
		int pAmount = BankInv.getGoldInBankInv(player);
		return pAmount >= amount;
	}

	@Override
	public boolean hasAccount(String player){
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			Class.forName("org.sqlite.JDBC");
			String dbPath = "jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "data.db";
			conn = DriverManager.getConnection(dbPath);
			st = conn.createStatement();
			rs = st.executeQuery("SELECT COUNT(*) FROM banks WHERE uuid = '" + MiscUtils.getSafeUUID(player) + "'");
			int i = 0;
			while (rs.next()){
				i += 1;
			}
			if (i > 0)
				return true;
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean hasBankSupport(){
		return false;
	}

	@Override
	public EconomyResponse isBankMember(String arg0, String arg1){
		 return new EconomyResponse(0,0, ResponseType.NOT_IMPLEMENTED, "This feature is not implemented in GoldBank.");
	}

	@Override
	public EconomyResponse isBankOwner(String arg0, String arg1){
		 return new EconomyResponse(0,0, ResponseType.NOT_IMPLEMENTED, "This feature is not implemented in GoldBank.");
	}

	@Override
	public boolean isEnabled(){
		return plugin.getServer().getPluginManager().isPluginEnabled(plugin);
	}

	@Override
	public EconomyResponse withdrawPlayer(String player, double amount){
		boolean success = BankInv.removeGoldFromBankInv(player, (int)amount);
		if (success)
			return new EconomyResponse(0, 0, ResponseType.SUCCESS, "Withdrew " + amount + " gold from " + player + "'s GoldBank account");
		else
			return new EconomyResponse(0, 0, ResponseType.FAILURE, "Failed to withdraw " + amount + " gold from " + player + "'s GoldBank account");
	}

	@Override
	public boolean createPlayerAccount(String player, String world){
		return createPlayerAccount(player);
	}

	@Override
	public EconomyResponse depositPlayer(String player, String world, double amount){
		return depositPlayer(player, amount);
	}

	@Override
	public double getBalance(String player, String world){
		return getBalance(player);
	}

	@Override
	public boolean has(String player, String world, double amount){
		return has(player, amount);
	}

	@Override
	public boolean hasAccount(String player, String world){
		return hasAccount(player);
	}

	@Override
	public EconomyResponse withdrawPlayer(String player, String world, double amount){
		return withdrawPlayer(player, amount);
	}

	@Override
	public EconomyResponse createBank(String bank, OfflinePlayer player){
		 return createBank(bank, player.getName());
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer player){
		return createPlayerAccount(player.getName());
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer player, String world){
		return createPlayerAccount(player.getName(), world);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, double amount){
		return depositPlayer(player.getName(), amount);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount){
		return depositPlayer(player.getName(), world, amount);
	}

	@Override
	public double getBalance(OfflinePlayer player){
		return getBalance(player.getName());
	}

	@Override
	public double getBalance(OfflinePlayer player, String world){
		return getBalance(player.getName(), world);
	}

	@Override
	public boolean has(OfflinePlayer player, double amount){
		return has(player.getName(), amount);
	}

	@Override
	public boolean has(OfflinePlayer player, String world, double amount){
		return has(player.getName(), world, amount);
	}

	@Override
	public boolean hasAccount(OfflinePlayer player){
		return hasAccount(player.getName());
	}

	@Override
	public boolean hasAccount(OfflinePlayer player, String world){
		return hasAccount(player.getName(), world);
	}

	@Override
	public EconomyResponse isBankMember(String bank, OfflinePlayer player){
		 return isBankMember(bank, player.getName());
	}

	@Override
	public EconomyResponse isBankOwner(String bank, OfflinePlayer player){
		 return isBankOwner(bank, player.getName());
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount){
		return withdrawPlayer(player.getName(), amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount){
		return withdrawPlayer(player.getName(), world, amount);
	}
	
}
