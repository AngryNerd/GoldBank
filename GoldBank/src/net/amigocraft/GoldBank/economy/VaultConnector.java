package net.amigocraft.GoldBank.economy;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import net.amigocraft.GoldBank.GoldBank;
import net.amigocraft.GoldBank.api.BankInv;
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
			String dbPath = "jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "chestdata.db";
			conn = DriverManager.getConnection(dbPath);
			st = conn.createStatement();
			rs = st.executeQuery("SELECT COUNT(*) FROM chestdata WHERE username = '" + player + "'");
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
	
}
