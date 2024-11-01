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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
								LazyListWithScrollStateControl(listOf(shortTermTracks, mediumTermTracks, longTermTracks))
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

val defaultModification = Modifier
	.fillMaxSize()
	.background(Color(30, 31, 34))


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

@Composable
fun LazyListWithScrollStateControl(pageListList: List<List<Page>>) {
	val lazyListState = rememberLazyListState()
	val coroutineScope = rememberCoroutineScope()
	val scrollOffset by remember { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }
	val firstItemVisible by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
	val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }


	LaunchedEffect(lazyListState.isScrollInProgress) {
		if (!lazyListState.isScrollInProgress && scrollOffset != 0) {
			if (scrollOffset < (screenWidthPx/1.78)) {
				lazyListState.animateScrollToItem(firstItemVisible)
			} else {
				lazyListState.animateScrollToItem(firstItemVisible + 1)
			}
		}
	}

	LazyRow(state = lazyListState) {
		items(pageListList) { item: List<Page> ->
			PageListView(item)
		}
	}
}


@Composable
fun PageListView(pageList: List<Page>) {
	LazyColumn(
		modifier = Modifier
			.width(LocalConfiguration.current.screenWidthDp.dp)
			.background(Color(24, 24, 28)) // Consistent dark background
			.padding(horizontal = 4.dp)
	) {
		itemsIndexed(pageList) { index, item: Page ->
			Box(
				modifier = Modifier
					.padding(vertical = 4.dp) // Reduced padding for a more unified look
					.fillMaxWidth()
					.clip(RoundedCornerShape(10.dp)) // Softer corner radius
					.background(Color(35, 35, 40)) // Single background color for all items
			) {
				Row(
					modifier = Modifier
						.padding(start = if (index >= 9) 8.dp else 19.dp, 12.dp, 12.dp,12.dp)
						.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically // Consistent alignment
				) {
					Text(
						text = "${index + 1}",
						color = Color(180, 180, 200),
						fontSize = 20.sp,
						fontWeight = FontWeight.Medium,
						modifier = Modifier.padding(end = if (index >= 9) 12.dp else 14.dp) // Adjust spacing for alignment
					)
					Image(
						painter = rememberAsyncImagePainter(model = item.imageURL),
						contentDescription = item.title,
						modifier = Modifier
							.size(100.dp)
							.clip(RoundedCornerShape(8.dp)) // Soft corner radius for image
							.background(Color(50, 50, 58)) // Darker border for subtle contrast
					)

					Spacer(modifier = Modifier.width(16.dp))

					Column(
						modifier = Modifier
							.fillMaxWidth()
					) {
						Text(
							text = item.title,
							color = Color(235, 235, 245), // Light text for contrast
							fontSize = 20.sp,
							fontWeight = FontWeight.Bold,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
						)
						Spacer(modifier = Modifier.height(4.dp))
						Text(
							text = item.description,
							color = Color(200, 200, 215), // Slightly muted light color for secondary text
							fontSize = 16.sp,
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
							lineHeight = 20.sp
						)
					}
				}
			}
		}
	}
}





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
	LazyListWithScrollStateControl(listOf(listOf(Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg")), listOf(Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg")), listOf(Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"),Page("title", "description", "https://canto-wp-media.s3.amazonaws.com/app/uploads/2019/08/19194138/image-url-3.jpg"))))
}