package com.example.servicetest

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

class MainActivity : AppCompatActivity() {

    //private var player: ExoPlayer? = null

    @OptIn(UnstableApi::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        findViewById<Button>(R.id.start).setOnClickListener {
            Intent(applicationContext, MyRadioService::class.java).also {
                it.action = MyRadioService.Actions.START.toString()
                startService(it)
            }
        }
        findViewById<Button>(R.id.stop).setOnClickListener {
            Intent(applicationContext, MyRadioService::class.java).also {
                it.action = MyRadioService.Actions.STOP.toString()
                startService(it)
            }
        }


//        player = ExoPlayer
//            .Builder(this)
//            .setMediaSourceFactory(
//                DefaultMediaSourceFactory(
//                    DefaultDataSource.Factory(
//                        this,
//                        DefaultHttpDataSource
//                            .Factory()
//                            .setAllowCrossProtocolRedirects(true)
//                    )
//                )
//            )
//            .build()
//        player?.setMediaItem(MediaItem.fromUri(Uri.parse("https://radio7.hostingradio.ru:8040/radio7128.mp3")))
//        player?.prepare()
//        player?.play()
    }




}