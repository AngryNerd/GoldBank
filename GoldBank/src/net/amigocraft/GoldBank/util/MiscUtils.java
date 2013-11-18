package net.amigocraft.GoldBank.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import net.amigocraft.GoldBank.GoldBank;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class MiscUtils {
	
	public static GoldBank plugin = GoldBank.plugin;
	
	public static boolean isInt(String i){
		try {
			Integer.parseInt(i);
			return true;
		}
		catch(NumberFormatException nfe){
			return false;
		}
	}

	public static boolean isMat(String m){
		if (Material.getMaterial(m) != null)
			return true;
		else
			return false;
	}
	
	@SuppressWarnings("deprecation")
	public static boolean isMat(int m){
		//TODO: Magic numbers. Because Bukkit said so.
		if (Material.getMaterial(m) != null)
			return true;
		else
			return false;
	}

	public static boolean isBool(String b){
		try {
			Boolean.parseBoolean(b);
			return true;
		}
		catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}

	public static boolean charKeyExists(char[] array, int key){
		try {
			Character.toString(array[key]);
			return true;
		}
		catch (Exception e){
			return false;
		}
	}

	public static Block getAdjacentBlock(Block block, Material material){
		BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP};
		for (BlockFace face : faces){
			Block adjBlock = block.getRelative(face);
			if (adjBlock.getType() == material){
				if (face != BlockFace.UP){
					@SuppressWarnings("deprecation")
					//TODO: This doesn't even make any sense. How the hell is this a "magic number?"
					byte data = adjBlock.getData();
					byte north = 0x2;
					byte south = 0x3;
					byte west = 0x4;
					byte east = 0x5;
					BlockFace attached = null;
					if (data == east){
						attached = BlockFace.WEST;
					}
					else if (data == west){
						attached = BlockFace.EAST;
					}
					else if (data == north){
						attached = BlockFace.SOUTH;
					}
					else if (data == south){
						attached = BlockFace.NORTH;
					}
					if (adjBlock.getType() == Material.SIGN_POST){
						attached = BlockFace.DOWN;
					}
					// I had to be a bit creative with the comparison...
					if (block.getX() == adjBlock.getRelative(attached).getX() && block.getY() == 
							adjBlock.getRelative(attached).getY() && block.getZ() == adjBlock.getRelative(attached).getZ()){
						return adjBlock;
					}
				}
				else if (material == Material.SIGN_POST){
					return adjBlock;
				}
			}
		}
		return null;
	}

	// read the text file
	public static String readFile(String fileName){
		File file = new File(fileName);
		char[] buffer = null;
		try {
			BufferedReader bufferedReader = new BufferedReader(
					new FileReader(file));
			buffer = new char[(int)file.length()];
			int i = 0;
			int c = bufferedReader.read();
			while (c != -1){
				buffer[i++] = (char)c;
				c = bufferedReader.read();
			}
			bufferedReader.close();
		}
		catch (FileNotFoundException e){
			e.printStackTrace();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		return new String(buffer);
	}

	// get the number of a day of the week
	public static int checkDay(String day){
		Map<String,Integer> mp=new HashMap<String,Integer>();
		mp.put("Sunday",1);
		mp.put("Monday",2);
		mp.put("Tuesday",3);
		mp.put("Wednesday",4);
		mp.put("Thursday",5);
		mp.put("Friday",6);
		mp.put("Saturday",7);
		return mp.get(day).intValue();
	}

	public static boolean colExists(String table, String col){
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			Class.forName("org.sqlite.JDBC");
			String dbPath = "jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "chestdata.db";
			conn = DriverManager.getConnection(dbPath);
			st = conn.createStatement();
			rs = st.executeQuery("SELECT " + col + " FROM " + table + " LIMIT 1");
			return true;
		}
		catch (Exception e){
			return false;
		}
		finally {
			try {
				conn.close();
				st.close();
				rs.close();
			}
			catch (Exception n){
				n.printStackTrace();
			}
		}
	}
	
	public String escape(String s){
		s = s.replace("'", "''");
		return s;
	}
}
