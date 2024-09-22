package com.conickel.tunestats;

import static com.conickel.tunestats.CONSTANTS.clientId;
import static com.conickel.tunestats.CONSTANTS.clientSecret;
import static com.conickel.tunestats.CONSTANTS.redirectURL;
import static com.conickel.tunestats.CONSTANTS.scopes;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SpotifyAPI {
	private static String accessToken;

	public static void setAuthCode(String authCode) {
		SpotifyAPI.authCode = authCode;
	}

	private static String authCode;

	private static final String baseAPIURL = "https://api.spotify.com/";

	public static void getAuthCodeFromAPI(Context context) {

		Log.d("MainActivity","Started");

		String spotifyAuthUrl = "https://accounts.spotify.com/authorize?" +
				"client_id=" + clientId +
				"&response_type=code" +
				"&redirect_uri=" + redirectURL +
				"&scope=" + scopes;

		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(spotifyAuthUrl)));
	}

	public static void exchangeAuthCodeForAccessToken() throws IOException {
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
		System.out.println("Response: " + response.toString());
		} else {
			System.out.println("Failed to get token, response code: " + responseCode);
		}

	}
}

