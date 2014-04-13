package net.amigocraft.GoldBank.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class NameFetcher {

	private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
	private static final JSONParser jsonParser = new JSONParser();

	public static String getUsername(UUID uuid) throws Exception {
		HttpURLConnection connection = (HttpURLConnection) new URL(PROFILE_URL+uuid.toString().replace("-", "")).openConnection();
		JSONObject response = (JSONObject) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
		String name = (String) response.get("name");
		String cause = (String) response.get("cause");
		String errorMessage = (String) response.get("errorMessage");
		if (cause != null && cause.length() > 0) {
			throw new IllegalStateException(errorMessage);
		}
		return name;
	}
}