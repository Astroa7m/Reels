@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalSnapperApi::class
)
@file:UnstableApi

package com.example.reels

// TODO: fix performance issue

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun ReelsScreen(modifier: Modifier = Modifier, exoPlayer: ExoPlayer) {
    val lazyState = rememberLazyListState()
    val currentlyPlayingItem = determineCurrentlyPlayingItem(lazyState, list)
    UpdateCurrentReel(currentlyPlayingItem, exoPlayer)
    LazyColumn(
        modifier = modifier,
        state = lazyState,
        flingBehavior = rememberSnapperFlingBehavior(
            lazyListState = lazyState,
            snapIndex = { _, startIndex, targetIndex ->
                targetIndex.coerceIn(startIndex - 1, startIndex + 1)
            }
        )
    ) {

        items(list) {
            ReelItem(
                currentPlayerItem = currentlyPlayingItem,
                reel = it,
                onIconClicked = {
                    // TODO:  
                },
                modifier = Modifier
                    .fillParentMaxSize(),
                exoPlayer = exoPlayer
            )
        }
    }
}

private fun determineCurrentlyPlayingItem(listState: LazyListState, items: List<Reel>): Reel? {
    val layoutInfo = listState.layoutInfo
    val visibleTweets = layoutInfo.visibleItemsInfo.map { items[it.index] }.ifEmpty { return null }
    return if (visibleTweets.size == 1) {
        visibleTweets.first()
    } else {
        val midPoint = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val itemsFromCenter =
            layoutInfo.visibleItemsInfo.sortedBy { abs((it.offset + it.size / 2) - midPoint) }
        itemsFromCenter.map { items[it.index] }.first()
    }
}


@Composable
fun ObserveLifeCycleForExoPlayer(exoPlayer: ExoPlayer) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.playWhenReady = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.playWhenReady = true
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.run {
                        stop()
                        release()
                    }
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun UpdateCurrentReel(reel: Reel?, exoPlayer: ExoPlayer) {
    reel?.let {
        LaunchedEffect(reel) {
            val defaultDataSource = DefaultHttpDataSource.Factory()
            exoPlayer.apply {
                val source = ProgressiveMediaSource.Factory(defaultDataSource)
                    .createMediaSource(MediaItem.fromUri(reel.reelUrl))
                setMediaSource(source)
                playWhenReady = true
            }
        }
    }
}

@Composable
fun ReelItem(
    currentPlayerItem: Reel?,
    reel: Reel,
    modifier: Modifier = Modifier,
    onIconClicked: (Icon) -> Unit,
    exoPlayer: ExoPlayer
) {
    var isMuted by remember {
        mutableStateOf(false)
    }
    Box(modifier = modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
    ) {
        if (isMuted.not()) {
            exoPlayer.volume = 0f
            isMuted = true
        } else {
            exoPlayer.volume = 1f
            isMuted = false
        }
    }) {

        var volumeIconVisibility by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(key1 = isMuted) {
            volumeIconVisibility = true
            delay(800)
            volumeIconVisibility = false
        }

        if (currentPlayerItem == reel) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        hideController()
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(0.5f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(PaddingValues(8.dp, 16.dp)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reels",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                IconButton(onClick = { onIconClicked(Icon.CAMERA) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(30.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
            ) {

                ReelsInfoItems(
                    reel.reelInfo
                ){
                    onIconClicked(it)
                }

            }
        }

        if (volumeIconVisibility) {
            Icon(
                painter = painterResource(id = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on),
                contentDescription = null,
                tint = Color.White.copy(0.5f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(100.dp)
            )
        }
    }
}

@Composable
fun ReelsInfoItems(
    reelInfo: ReelInfo,
    onIconClicked: (Icon) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(3f)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            ReelsBottomItems(
                modifier = Modifier.fillMaxWidth(.8f),
                reelInfo = reelInfo
            )
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.5f),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ReelsColumnIcons(
                reelInfo = reelInfo,
                onIconClicked = onIconClicked
            )
        }
    }
}

@Composable
fun ReelsBottomItems(
    modifier: Modifier = Modifier,
    reelInfo: ReelInfo
) {

    var isFollowed by remember {
        mutableStateOf(false)
    }

    var expandedDesc by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(reelInfo.profilePicUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = reelInfo.username,
                fontSize = 10.sp,
                color = Color.White,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .border(
                        BorderStroke(
                            1.dp,
                            Color.White
                        ),
                        shape = MaterialTheme.shapes.small
                    )
                    .clickable {
                        isFollowed = !isFollowed
                    }
                    .animateContentSize()

            ) {
                Text(
                    text = if (isFollowed) "Followed" else "Follow",
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        val scrollState = rememberScrollState()
        val interactionSource = remember { MutableInteractionSource() }
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            reelInfo.description?.let { desc ->
                Text(
                    text = desc,
                    fontSize = 10.sp,
                    maxLines = if (expandedDesc) Int.MAX_VALUE else 1,
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            expandedDesc = !expandedDesc
                        }
                        .animateContentSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ReelsExtraBottomItems(
                modifier = Modifier.fillMaxWidth(),
                reelInfo
            )
        }

    }
}

@Composable
fun ReelsExtraBottomItems(
    modifier: Modifier = Modifier,
    reelInfo: ReelInfo
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReelsExtraBottomItem(
            modifier = Modifier.weight(1f),
            value = reelInfo.audio,
            R.drawable.ic_music
        )
        Spacer(modifier = Modifier.width(8.dp))
        reelInfo.filter?.let {
            ReelsExtraBottomItem(
                modifier = Modifier.weight(1f),
                value = it,
                R.drawable.flare_ic
            )
            Spacer(modifier = Modifier.width(8.dp))
        } ?: run {
            reelInfo.location?.let {
                ReelsExtraBottomItem(
                    modifier = Modifier.weight(1f),
                    value = it,
                    Icons.Default.LocationOn
                )
                Spacer(modifier = Modifier.width(8.dp))
            } ?: run {
                if (reelInfo.taggedPeople?.isNotEmpty() == true) {
                    if (reelInfo.taggedPeople.size == 1) {
                        ReelsExtraBottomItem(
                            modifier = Modifier.weight(1f),
                            value = reelInfo.taggedPeople[0],
                            Icons.Default.Person
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        ReelsExtraBottomItem(
                            modifier = Modifier.weight(1f),
                            value = reelInfo.taggedPeople.size.toString(),
                            iconVector = Icons.Default.Person
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReelsExtraBottomItem(
    modifier: Modifier = Modifier,
    value: String,
    @DrawableRes iconRes: Int,
    contentDescription: String? = null
) {

    val scrollState = rememberScrollState()
    var shouldAnimated by remember {
        mutableStateOf(true)
    }
    LaunchedEffect(key1 = shouldAnimated) {

        scrollState.animateScrollTo(
            scrollState.maxValue,
            animationSpec = tween(10000, easing = CubicBezierEasing(0f, 0f, 0f, 0f))
        )
        scrollState.scrollTo(0)
        shouldAnimated = !shouldAnimated
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { }
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier.horizontalScroll(scrollState, false)
        )
    }
}

@Composable
fun ReelsExtraBottomItem(
    modifier: Modifier = Modifier,
    value: String,
    iconVector: ImageVector,
    contentDescription: String? = null
) {

    val scrollState = rememberScrollState()
    var shouldAnimated by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(key1 = shouldAnimated) {
        scrollState.animateScrollTo(
            scrollState.maxValue,
            animationSpec = tween(10000, easing = CubicBezierEasing(0f, 0f, 0f, 0f))
        )
        scrollState.scrollTo(0)
        shouldAnimated = !shouldAnimated
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { }
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = value,
            fontSize = 10.sp,
            maxLines = 1,
            color = Color.White,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.horizontalScroll(scrollState)
        )
    }
}

@Composable
fun ReelsColumnIcons(
    reelInfo: ReelInfo,
    onIconClicked: (Icon) -> Unit
) {

    var isLiked by remember {
        mutableStateOf(false)
    }

    TextedIcon(
        iconVector = if (!isLiked) Icons.Outlined.FavoriteBorder else Icons.Filled.Favorite,
        text = reelInfo.likes.toString(),
        modifier = Modifier.size(30.dp),
        tint = if (isLiked) Color.Red else Color.White,
        onIconClicked = {
            onIconClicked(Icon.LIKE)
            isLiked = !isLiked
        }
    )
    TextedIcon(
        iconRes = R.drawable.ic_chat_bubble,
        text = reelInfo.comments.toString(),
        modifier = Modifier.size(30.dp),
        onIconClicked = {
            onIconClicked(Icon.COMMENT)
        }
    )

    IconButton(onClick = { onIconClicked(Icon.SHARE) }) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
    IconButton(onClick = { onIconClicked(Icon.MORE_OPTIONS) }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(30.dp)
                .rotate(90f),
        )
    }
    IconButton(onClick = { onIconClicked(Icon.AUDIO) }) {
        Image(
            painter = rememberAsyncImagePainter(reelInfo.audioPicUrl),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun TextedIcon(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int,
    text: String,
    tint: Color = Color.White,
    contentDescription: String? = null,
    onIconClicked: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = {
            onIconClicked()
        }) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier
            )
        }
        Text(
            text = text,
            color = Color.White
        )
    }
}

@Composable
fun TextedIcon(
    modifier: Modifier = Modifier,
    iconVector: ImageVector,
    text: String,
    tint: Color = Color.White,
    contentDescription: String? = null,
    onIconClicked: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = {
            onIconClicked()
        }) {
            Icon(
                imageVector = iconVector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier
            )
        }
        Text(
            text = text,
            color = Color.White
        )
    }
}

/*

@Preview
@Composable
fun PreviewReelItem() {
    ReelItem(
        reel = Reel(
            "www",
            false,
            ReelInfo(
                "Astro",
                "www",
                "Look at my brand new reel, Omg It is so Awesome man. Please like share and comment for a better reach",
                100,
                100,
                audio = "Look at my brand new reel, Omg It is so Awesome man. Please like share and comment for a better reach",
                //filter = "Some long filter name dahibafiguafeugoqeugi fqhiqfuiw",
                location = "Muscat, Oman",
                taggedPeople = listOf("Delwar", "Kruger")
            )
        ), onIconClicked = {},
        modifier = Modifier.fillMaxSize()
    )
}
*/



@Preview(device = Devices.NEXUS_5)
@Composable
fun PreviewReelsScreen() {
    //ReelsScreen(player = player)
}

