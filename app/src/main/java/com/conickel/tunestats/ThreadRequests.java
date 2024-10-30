package com.conickel.tunestats;

import static com.conickel.tunestats.CONSTANTS.ItemType.*;
import static com.conickel.tunestats.CONSTANTS.TimeFrame.*;

import org.json.JSONException;

import java.io.IOException;

class exchangeAuthCode extends Thread {
	private static String authCode;
	private static SpotifyAPI spotifyAPI;

	@Override
	public void run() {
		try {
			spotifyAPI.exchangeAuthCodeForAccessToken(spotifyAPI);
			spotifyAPI.sendUserDetailsToPYListeningScript();
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
			spotifyAPI.getTop(tracks, short_term);spotifyAPI.getTop(artists, short_term);
			spotifyAPI.getTop(tracks, medium_term);spotifyAPI.getTop(artists, medium_term);
			spotifyAPI.getTop(tracks, long_term);spotifyAPI.getTop(artists, long_term);
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
