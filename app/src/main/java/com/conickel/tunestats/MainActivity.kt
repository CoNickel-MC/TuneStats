package com.conickel.tunestats

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.conickel.tunestats.ui.theme.TuneStatsTheme
import kotlinx.coroutines.launch
import kotlin.math.abs


var shortTermArtists: List<Page> = listOf()
var shortTermTracks: List<Page> = listOf()
var mediumTermArtists: List<Page> = listOf()
var mediumTermTracks: List<Page> = listOf()
var longTermArtists: List<Page> = listOf()
var longTermTracks: List<Page> = listOf()


class MainActivity : ComponentActivity() {
	companion object {
		var uiState by mutableStateOf(UiState.Initial)
	}

	private var spotifyAPI: SpotifyAPI = SpotifyAPI()


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)


		enableEdgeToEdge()

		setContent {
			TuneStatsTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
					Column(modifier = Modifier.padding(paddingValues)) {
						when (uiState) {
							UiState.Initial -> InitPage { spotifyAPI.getAuthCodeFromAPI(this@MainActivity) }
							UiState.AuthCodeReceived -> Text("Token received")
							UiState.TokenReceived -> {
								Column(Modifier.fillMaxSize()) {
									SlidingBox(shortTermTracks, Modifier)
									SlidingBox(shortTermArtists, Modifier)
									SlidingBox(mediumTermTracks, Modifier)
									SlidingBox(mediumTermArtists, Modifier)
									SlidingBox(longTermTracks, Modifier)
									SlidingBox(longTermArtists, Modifier)
								}
							}

							UiState.Error -> Text("Error occurred")
						}
					}
				}
			}
		}

	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)

		intent.data?.let { uri ->

			handleAuthResponse(uri)
		}
	}


	private fun handleAuthResponse(uri: Uri) {
		val authCode = uri.getQueryParameter("code")

		if (authCode != null) {
			uiState = UiState.AuthCodeReceived
			exchangeAuthCode.startAccessCodeRequest(authCode, spotifyAPI)
		} else {
			uiState = UiState.Error
		}
	}

	enum class UiState {
		Initial, AuthCodeReceived, TokenReceived, Error
	}
}

@Composable
fun SlidingBox(items: List<Page>, modifier: Modifier) {
	val listState = rememberLazyListState()
	val scope = rememberCoroutineScope()

	Box(
		modifier = Modifier
			.fillMaxSize()
			.then(modifier), contentAlignment = Alignment.Center
	) {
		LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
			itemsIndexed(items) { _, item ->
				Box(
					modifier = Modifier
						.width(LocalConfiguration.current.screenWidthDp.dp)
						.padding(16.dp), contentAlignment = Alignment.Center
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
							Text(
								text = item.title, style = MaterialTheme.typography.headlineLarge
							)
							Text(
								text = item.description,
								style = MaterialTheme.typography.headlineMedium
							)
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
						abs(it.offset + it.size / 2 - listState.layoutInfo.viewportEndOffset / 2)
					}?.index ?: return@LaunchedEffect
					scope.launch {
						listState.animateScrollToItem(middleItem)
					}
				}
			}
		}
	}
}


val defaultModification = Modifier
	.fillMaxSize()
	.background(Color(30, 31, 34))


@Composable
fun RedirectButton(onClickFunction: () -> Unit, modifier: Modifier) {
	Box(
		modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
	) {
		Button(colors = ButtonDefaults.buttonColors(containerColor = Color(50, 200, 40, 130)), onClick = onClickFunction) {
			Text(text = "Login with Spotify")
		}
	}
}

@Composable
fun InitPage(onClickFunction: () -> Unit) {
	Column(
		modifier = defaultModification,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
	) {
		Row(modifier = Modifier.weight(1.0f)){}
		Text(
			buildAnnotatedString {
				withStyle(style = SpanStyle(color = Color(200, 200, 200, 255))) {
					append("Welcome to\n")
				}
				withStyle(
					style = SpanStyle(
						color = Color(30, 215, 96),
						fontSize = 30.sp,
						fontFamily = FontFamily(Font(R.font.roboto_medium))
					)
				) {
					append("TuneStats")
				}
			}, textAlign = TextAlign.Center, modifier = Modifier.weight(0.15f)
		)
		RedirectButton(onClickFunction, Modifier.weight(0.15f))
		Row(modifier = Modifier.weight(1.0f)){}
	}
}


@Preview(showBackground = true)
@Composable
fun MainPage() {
	Column(modifier = defaultModification) {

		Text(
			text = "TuneStats",
			color = Color(30, 215, 96),
			fontSize = 30.sp,
			fontFamily = FontFamily(Font(R.font.roboto_medium)),
			modifier = Modifier
				.weight(0.5f)
				.padding(5.dp)
		)

		Row(
			Modifier
				.weight(1.0f)
				.padding(2.5.dp)
		) {
			ButtonForMainPage("Insights", Modifier.weight(1.0f)) {}
			ButtonForMainPage("WOW", Modifier.weight(1.0f)) {}
		}


		Row(
			Modifier
				.weight(1.0f)
				.padding(2.5.dp)
		) {
			ButtonForMainPage("Hello", Modifier.weight(1.0f)) {}
			ButtonForMainPage("Hello", Modifier.weight(1.0f)) {}
		}

		Row(modifier = defaultModification.weight(2.0f)) { }

	}

}


@Composable
fun ButtonForMainPage(text: String, modifier: Modifier, onClickFunction: () -> Unit) {
	Surface(
		modifier = modifier
			.fillMaxSize()
			.padding(5.dp),
		shape = RoundedCornerShape(20.dp),
		onClick = onClickFunction
	) {

		Text(
			text = text,
			color = Color(255, 255, 255),
			textAlign = TextAlign.Center,
			fontSize = 15.sp,
			modifier = Modifier
				.background(Color(43, 45, 48))
				.wrapContentHeight(align = Alignment.CenterVertically)
		)
	}
}


@Preview
@Composable
fun TestTab() {
	InitPage { println("hello") }
}