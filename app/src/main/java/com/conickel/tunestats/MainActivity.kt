package com.conickel.tunestats

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.conickel.tunestats.ui.theme.TuneStatsTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
	private var uiState by mutableStateOf(UiState.Initial)



	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		setContent {
			TuneStatsTheme {
				Scaffold(
					modifier = Modifier.fillMaxSize(),
					content = { paddingValues ->
						Column(modifier = Modifier.padding(paddingValues)) {
							when (uiState) {
								UiState.Initial -> RedirectButton(this@MainActivity)
								UiState.TokenReceived -> Text("Token received")
								UiState.Error -> Text("Error occurred")
							}
						}
					}
				)
			}
		}

	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
		Log.d("MainActivity", "JAMPACKED")


		intent.data?.let { uri ->
			Log.d("MainActivity", "Received redirect URI: $uri")
			handleAuthResponse(uri)
		}
	}


	private fun handleAuthResponse(uri: Uri) {
		val authCode = uri.getQueryParameter("code")
		Log.d("MainActivity", "Auth code: $authCode")

		if (authCode != null) {
			uiState = UiState.TokenReceived
			exchangeAuthCode.startAccessCodeRequest(authCode)
		} else {
			uiState = UiState.Error

			val error = uri.getQueryParameter("error")
		}
	}
	enum class UiState {
		Initial,
		TokenReceived,
		Error
	}
}

@Composable
fun RedirectButton(context: Context) {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Button(onClick = { SpotifyAPI.getAuthCodeFromAPI(context) }) {
			Text(text = "Login with Spotify")
		}
	}
}


@Composable
fun SlidingBox(items: List<Page>) {
	val listState = rememberLazyListState()
	val scope = rememberCoroutineScope()

	Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
			itemsIndexed(items) { index, item ->
				Box(
					modifier = Modifier
						.width(400.dp)
						.padding(16.dp),
					contentAlignment = Alignment.Center
				) {
					Row {
						Image(
							painter = rememberAsyncImagePainter(model = item.imageURL),
							contentDescription = item.title,
							modifier = Modifier
								.size(64.dp)
								.padding(end = 8.dp)
						)
						Column {
							Text(text = item.title, style = MaterialTheme.typography.headlineLarge)
							Text(text = item.description, style = MaterialTheme.typography.headlineMedium)
						}
					}
				}
			}
		}

		//Pages snapping
		LaunchedEffect(listState.isScrollInProgress) {
			if (!listState.isScrollInProgress) {
				val visibleItemInfo = listState.layoutInfo.visibleItemsInfo
				if (visibleItemInfo.isNotEmpty()) {
					val middleItem = visibleItemInfo.minByOrNull {
						Math.abs(it.offset + it.size / 2 - listState.layoutInfo.viewportEndOffset / 2)
					}?.index ?: return@LaunchedEffect
					scope.launch {
						listState.animateScrollToItem(middleItem)
					}
				}
			}
		}
	}
}
