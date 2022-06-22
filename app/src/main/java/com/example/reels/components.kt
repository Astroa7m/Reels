@file:OptIn(ExperimentalAnimationApi::class)

package com.example.reels

// TODO: change to clean data
// TODO: check reel item

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ReelsScreen() {
    val pagerState = rememberPagerState()
    var isMuted by remember {
        mutableStateOf(false)
    }
    val onLiked = remember {
        { index: Int, liked: Boolean ->
            reels[index] =
                reels[index].copy(reelInfo = reels[index].reelInfo.copy(isLiked = liked))
        }
    }
    val isFirstItem by remember(pagerState) {
        derivedStateOf {
            pagerState.currentPage == 0
        }
    }

    LaunchedEffect(key1 = pagerState) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
            pagerState.animateScrollToPage(page)
        }
    }

    Box {
        VerticalPager(
            count = reels.size,
            state = pagerState,
            horizontalAlignment = Alignment.CenterHorizontally,
            itemSpacing = 10.dp
        ) { index ->
            val shouldPlay by remember(pagerState) {
                derivedStateOf {
                    (abs(currentPageOffset) < .5 && currentPage == index) || (abs(
                        currentPageOffset
                    ) > .5 && pagerState.targetPage == index)
                }
            }
            ReelPlayer(
                reel = reels[index],
                shouldPlay = shouldPlay,
                isMuted = isMuted,
                isScrolling = pagerState.isScrollInProgress,
                onMuted = {
                    isMuted = it
                },
                onDoubleTap = {
                    onLiked(index, it)
                }
            )
            ReelItem(
                reel = reels[index],
                onIconClicked = { icon ->
                    when (icon) {
                        Icon.CAMERA -> {
                            //:TODO
                        }
                        Icon.SHARE -> {
                            //:TODO
                        }
                        Icon.MORE_OPTIONS -> {
                            //:TODO
                        }
                        Icon.AUDIO -> {
                            //:TODO
                        }
                        Icon.LIKE -> {
                            onLiked(index, !reels[index].reelInfo.isLiked)
                        }
                        Icon.COMMENT -> {
                            //:TODO
                        }
                    }
                }
            )
            ReelHeader(
                isFirstItem = isFirstItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {

            }
        }
    }
}

@Composable
fun rememberExoPlayerWithLifecycle(
    reelUrl: String
): ExoPlayer {

    val context = LocalContext.current
    val exoPlayer = remember(reelUrl) {
        ExoPlayer.Builder(context).build().apply {
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            repeatMode = Player.REPEAT_MODE_ONE
            setHandleAudioBecomingNoisy(true)
            val defaultDataSource = DefaultHttpDataSource.Factory()
            val source = ProgressiveMediaSource.Factory(defaultDataSource)
                .createMediaSource(MediaItem.fromUri(reelUrl))
            setMediaSource(source)
            prepare()
        }
    }
    var appInBackground by remember {
        mutableStateOf(false)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, appInBackground) {
        val lifecycleObserver = getExoPlayerLifecycleObserver(exoPlayer, appInBackground) {
            appInBackground = it
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return exoPlayer
}

@Composable
fun ReelHeader(
    modifier: Modifier = Modifier,
    isFirstItem: Boolean,
    onCameraIconClicked: (Icon) -> Unit
) {
    Box(
        modifier = modifier
            .padding(PaddingValues(8.dp, 16.dp)),
    ) {
        AnimatedVisibility(
            visible = isFirstItem,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = "Reels",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
        }

        IconButton(
            onClick = { onCameraIconClicked(Icon.CAMERA) },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(30.dp)
            )
        }
    }
}

fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit
): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (wasAppInBackground)
                    exoPlayer.playWhenReady = true
                setWasAppInBackground(false)
            }
            Lifecycle.Event.ON_PAUSE -> {
                exoPlayer.playWhenReady = false
                setWasAppInBackground(true)
            }
            Lifecycle.Event.ON_STOP -> {
                exoPlayer.playWhenReady = false
                setWasAppInBackground(true)
            }
            Lifecycle.Event.ON_DESTROY -> {
                exoPlayer.release()
            }
            else -> {}
        }
    }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ReelPlayer(
    reel: Reel,
    shouldPlay: Boolean,
    isMuted: Boolean,
    onMuted: (Boolean) -> Unit,
    onDoubleTap: (Boolean) -> Unit,
    isScrolling: Boolean
) {
    val exoPlayer = rememberExoPlayerWithLifecycle(reel.reelUrl)
    val playerView = rememberPlayerView(exoPlayer)
    var volumeIconVisibility by remember { mutableStateOf(false) }
    var likeIconVisibility by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box {
        AndroidView(
            factory = { playerView },
            modifier = Modifier
                .pointerInput(reel.reelInfo.isLiked, isMuted) {
                    detectTapGestures(
                        onDoubleTap = {
                            onDoubleTap(true)
                            coroutineScope.launch {
                                likeIconVisibility = true
                                delay(800)
                                likeIconVisibility = false
                            }
                        },
                        onTap = {
                            if (exoPlayer.playWhenReady) {
                                if (isMuted.not()) {
                                    exoPlayer.volume = 0f
                                    onMuted(true)
                                } else {
                                    exoPlayer.volume = 1f
                                    onMuted(false)
                                }
                                coroutineScope.launch {
                                    volumeIconVisibility = true
                                    delay(800)
                                    volumeIconVisibility = false
                                }
                            }
                        },
                        onPress = {
                            if (!isScrolling) {
                                exoPlayer.playWhenReady = false
                                awaitRelease()
                                exoPlayer.playWhenReady = true
                            }
                        },
                        onLongPress = {}
                    )
                },
            update = {
                exoPlayer.volume = if (isMuted) 0f else 1f
                exoPlayer.playWhenReady = shouldPlay
            }
        )

        AnimatedVisibility(
            visible = likeIconVisibility,
            enter = scaleIn(
                spring(Spring.DampingRatioMediumBouncy)
            ),
            exit = scaleOut(tween(150)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White.copy(0.90f),
                modifier = Modifier
                    .size(100.dp)
            )
        }

        if (volumeIconVisibility) {
            Icon(
                painter = painterResource(id = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on),
                contentDescription = null,
                tint = Color.White.copy(0.75f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(100.dp)
            )
        }

    }

    DisposableEffect(key1 = true) {
        onDispose {
            exoPlayer.release()
        }
    }
}

@Composable
fun rememberPlayerView(exoPlayer: ExoPlayer): PlayerView {
    val context = LocalContext.current
    val playerView = remember {
        PlayerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            player = exoPlayer
            setShowBuffering(SHOW_BUFFERING_ALWAYS)
        }
    }
    DisposableEffect(key1 = true) {
        onDispose {
            playerView.player = null
        }
    }
    return playerView
}

@Composable
fun ReelItem(
    reel: Reel,
    onIconClicked: (Icon) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
        ) {

            ReelsInfoItems(
                reel.reelInfo
            ) {
                onIconClicked(it)
            }

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

    TextedIcon(
        iconVector = if (!reelInfo.isLiked) Icons.Outlined.FavoriteBorder else Icons.Filled.Favorite,
        text = reelInfo.likes.toString(),
        modifier = Modifier.size(30.dp),
        tint = if (reelInfo.isLiked) Color.Red else Color.White,
        onIconClicked = {
            onIconClicked(Icon.LIKE)
//            reelInfo.isLiked = !reelInfo.isLiked
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
