package com.conickel.tunestats;

import static com.conickel.tunestats.CONSTANTS.*;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;


public class SpotifyAPI {
	private String authCode;
	private String accessToken;
	private String refreshToken;

	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	}

	public void getAuthCodeFromAPI(Context context) {

		String spotifyAuthUrl = "https://accounts.spotify.com/authorize?" +
				"client_id=" + clientId +
				"&response_type=code" +
				"&redirect_uri=" + redirectURL +
				"&scope=" + scopes;

		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(spotifyAuthUrl)));
	}

	public void sendUserDetailsToPYListeningScript() throws IOException, JSONException {
		String name;
		String emailId;
		HttpURLConnection conn = (HttpURLConnection) new URL("https://api.spotify.com/v1/me").openConnection();

		conn.setRequestMethod("GET");
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);

		int responseCode = conn.getResponseCode();

		if (responseCode == HttpsURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			conn.disconnect();
			JSONObject jsonResponse = new JSONObject(response.toString());
			name = jsonResponse.getString("display_name");
			emailId = jsonResponse.getString("email");


			HttpURLConnection connection = (HttpURLConnection) new URL("https://spotifylisteningtimescript.onrender.com/addUser").openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type","application/json");

			String payload = String.format(
					"{\"name\":\"%s\",\"emailId\":\"%s\",\"currentAccessToken\":\"%s\",\"refreshToken\":\"%s\",\"listenTime\":0,\"lastCheckTime\":0}",
					name, emailId, accessToken, refreshToken
			);

			byte[] out = payload.getBytes(StandardCharsets.UTF_8);
			OutputStream stream = connection.getOutputStream();
			stream.write(out);

			connection.disconnect();
		}
	}

	public void exchangeAuthCodeForAccessToken(SpotifyAPI spotifyAPI) throws IOException, JSONException {
		URL accessTokenURL = new URL("https://accounts.spotify.com/api/token");
		HttpURLConnection conn = (HttpURLConnection) accessTokenURL.openConnection();

		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		String clientCredentials = clientId + ":" + clientSecret;
		String basicAuth = "Basic " + Base64.getEncoder().encodeToString(clientCredentials.getBytes());
		conn.setRequestProperty("Authorization", basicAuth);

		OutputStream outputStream = conn.getOutputStream();

		String requestBody = "grant_type=authorization_code" +
				"&code=" + authCode +
				"&redirect_uri=" + redirectURL;
		byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
		outputStream.write(requestBodyBytes);


		int responseCode = conn.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String responseLine;
			StringBuilder response = new StringBuilder();

			while ((responseLine = bufferedReader.readLine()) != null) {
				response.append(responseLine);
			}

			JSONObject jsonResponse = new JSONObject(response.toString());
			accessToken = jsonResponse.getString("access_token");
			refreshToken = jsonResponse.getString("refresh_token");
			conn.disconnect();
			getTopAll.startRequest(spotifyAPI);
		}
	}

	public void getTop(ItemType itemType, TimeFrame timeFrame) throws IOException, JSONException {
		URL topArtistsURL = new URL("https://api.spotify.com/v1/me/top/"+itemType+"?time_range="+timeFrame+"&limit=15&offset=0");

		HttpsURLConnection conn = (HttpsURLConnection) topArtistsURL.openConnection();
		conn.setRequestMethod("GET");

		conn.setRequestProperty("Authorization", "Bearer " + accessToken);
		conn.setRequestProperty("Content-Type", "application/json");

		int responseCode = conn.getResponseCode();

		if (responseCode == HttpsURLConnection.HTTP_OK) {

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			conn.disconnect();
			switch (itemType) {
				case artists:
					getTopArtists(response, timeFrame);
					break;
				case tracks:
					getTopTracks(response, timeFrame);
					break;
			}

		}
	}
	private static void getTopArtists(StringBuilder response, TimeFrame timeFrame) throws JSONException {
		List<Page> pageList = new ArrayList<>();


		JSONArray fullResponse = new JSONObject(response.toString()).getJSONArray("items");
		for (int i = 0; i < fullResponse.length(); i++) {
			StringBuilder genres = new StringBuilder();
			JSONObject temp = fullResponse.getJSONObject(i);
			JSONArray genresArray = temp.getJSONArray("genres");
			for (int j = 0; j < genresArray.length(); j++) {
				genres.append(genresArray.getString(j));
				genres.append(", ");
			}
			genres.substring(0, genres.length()-2);

			pageList.add(new Page(temp.getString("name"), genres.toString(), temp.getJSONArray("images").getJSONObject(0).getString("url")));
		}
		switch (timeFrame) {
			case short_term:
				MainActivityKt.setShortTermArtists(pageList);
				break;
			case medium_term:
				MainActivityKt.setMediumTermArtists(pageList);
				break;
			case long_term:
				MainActivityKt.setLongTermArtists(pageList);
				break;
		}
	}

	private static void getTopTracks(StringBuilder response, TimeFrame timeFrame) throws JSONException {
		List<Page> pageList = new ArrayList<>();


		JSONArray fullResponse = new JSONObject(response.toString()).getJSONArray("items");
		for (int i = 0; i < fullResponse.length(); i++) {
			JSONObject temp = fullResponse.getJSONObject(i);

			pageList.add(new Page(temp.getString("name"),temp.getJSONArray("artists").getJSONObject(0).getString("name") , temp.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url")));
		}
		switch (timeFrame) {
			case short_term:
				MainActivityKt.setShortTermTracks(pageList);
				break;
			case medium_term:
				MainActivityKt.setMediumTermTracks(pageList);
				break;
			case long_term:
				MainActivityKt.setLongTermTracks(pageList);
				break;
		}

	}

}

