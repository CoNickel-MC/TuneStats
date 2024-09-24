package com.conickel.tunestats;

import org.json.JSONException;

import java.io.IOException;

class exchangeAuthCode extends Thread {
	private static String authCode;
	private static SpotifyAPI spotifyAPI;

	@Override
	public void run() {
		try {
			spotifyAPI.exchangeAuthCodeForAccessToken(spotifyAPI);
		} catch (IOException | JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public static void startAccessCodeRequest(String authCode, SpotifyAPI spotifyAPI) {
		exchangeAuthCode.spotifyAPI = spotifyAPI;
		spotifyAPI.setAuthCode(authCode);
		new exchangeAuthCode().start();
	}
}

class getTopArtists extends Thread {
	private static SpotifyAPI spotifyAPI;

	@Override
	public void run() {
		try {
			spotifyAPI.getTopArtists();
		}
		catch (IOException | JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public static void startRequest(SpotifyAPI spotifyAPI) {
		getTopArtists.spotifyAPI = spotifyAPI;
		new getTopArtists().start();
	}
}
