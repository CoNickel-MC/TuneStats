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

class getTopAll extends Thread {
	private static SpotifyAPI spotifyAPI;

	@Override
	public void run() {
		try {
			spotifyAPI.getTop(CONSTANTS.ItemType.tracks);spotifyAPI.getTop(CONSTANTS.ItemType.artists);
			MainActivity.Companion.setUiState(MainActivity.UiState.TokenReceived);
		}
		catch (IOException | JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public static void startRequest(SpotifyAPI spotifyAPI) {
		getTopAll.spotifyAPI = spotifyAPI;
		new getTopAll().start();
	}
}
