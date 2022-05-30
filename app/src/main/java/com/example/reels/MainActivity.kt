package com.example.reels

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.reels.ui.theme.ReelsTheme
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity() {
    override fun  onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReelsTheme {
                val context = LocalContext.current
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                        repeatMode = Player.REPEAT_MODE_ONE
                        prepare()
                    }
                }
                ObserveLifeCycleForExoPlayer(exoPlayer)
                Surface(
                    color = Color.Black
                ) {
                    ReelsScreen(exoPlayer = exoPlayer, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
