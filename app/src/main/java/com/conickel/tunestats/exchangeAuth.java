package com.conickel.tunestats;

import android.util.Log;

import java.io.IOException;

class exchangeAuthCode extends Thread {
	private static String authCode;

	@Override
	public void run() {
		try {
			SpotifyAPI.exchangeAuthCodeForAccessToken();
		} catch (IOException e) {
			Log.d("b", "B BOY");
			throw new RuntimeException(e);
		}
	}

	public static void startAccessCodeRequest(String authCode) {
		SpotifyAPI.setAuthCode(authCode);
		new exchangeAuthCode().start();
	}
}
