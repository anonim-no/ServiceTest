package com.example.servicetest

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory


@UnstableApi
class MyRadioService : Service() {

    private lateinit var player: ExoPlayer
    private lateinit var selectedRadio: Radio
    private lateinit var mediaSession: MediaSessionCompat

    private val stateBuilder = PlaybackStateCompat.Builder().setActions(
        PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    )

    private var currentPlayBackState = PlaybackStateCompat.STATE_STOPPED

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> stop()
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.toString() -> start()
            Actions.STOP.toString() -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "RadioService")
    }

    private fun stop() {
        player.release()
        stopSelf()
    }


    private fun getNotification(playbackState: Int): Notification {
        val builder: NotificationCompat.Builder = MediaStyleHelper.from(
            this,
            mediaSession,
            MY_RADIO_CHANNEL_ID
        )
        if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_pause,
                    null,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PAUSE
                    )
                )
            )
        } else {
            builder.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_play,
                    null,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
        }
        builder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_previous,
                null,
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            )
        )
        builder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_next,
                null,
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )
        )

        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setShowCancelButton(true)
                .setCancelButtonIntent(getStopServiceIntent())
                .setMediaSession(mediaSession.sessionToken)
        ) // setMediaSession требуется для Android Wear

        builder.setDeleteIntent(getStopServiceIntent())

        builder.setSmallIcon(R.drawable.ic_radio)
        builder.setShowWhen(false)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setChannelId(MY_RADIO_CHANNEL_ID)
        return builder.build()
    }

    private fun getStopServiceIntent(): PendingIntent? {
        return PendingIntent.getService(
            this,
            0,
            Intent(
                this,
                MyRadioService::class.java
            ).setAction(Actions.STOP.toString()),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun start() {



        val notification = getNotification(currentPlayBackState)

        startForeground(1, notification)



        player = ExoPlayer
            .Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    DefaultDataSource.Factory(
                        this,
                        DefaultHttpDataSource
                            .Factory()
                            .setAllowCrossProtocolRedirects(true)
                    )
                )
            )
            .build()


        player.addListener(
            object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_IDLE, Player.STATE_ENDED -> {
                            radioList[radioList.indexOf(selectedRadio)].isPlaying = false
                            radioList[radioList.indexOf(selectedRadio)].isLoading = false
                            // остановлено
                            currentPlayBackState = PlaybackStateCompat.STATE_STOPPED
                        }

                        Player.STATE_BUFFERING -> {
                            radioList[radioList.indexOf(selectedRadio)].isLoading = true
                            radioList[radioList.indexOf(selectedRadio)].isPlaying = false
                            // подгрузка
                            currentPlayBackState = PlaybackStateCompat.STATE_PLAYING
                        }

                        Player.STATE_READY -> {
                            radioList[radioList.indexOf(selectedRadio)].isPlaying = true
                            radioList[radioList.indexOf(selectedRadio)].isLoading = false
                            // остановлено
                            currentPlayBackState = PlaybackStateCompat.STATE_STOPPED
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    toast(error.toString())
                }

                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    mediaMetadata.title?.let {
                        // установить метаданные
                    }
                }
            }
        )

        selectedRadio = radioList[0]

        play(selectedRadio)

        registerReceiver(broadcastReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    private fun selectRadio(radio: Radio) {
        if (radio != selectedRadio) {
            selectedRadio = radio
        }
    }

    private fun play(radio: Radio) {

        selectRadio(radio)

        player.stop()

        if (isNetworkAvailable()) {

            player.setMediaItem(MediaItem.fromUri(Uri.parse(selectedRadio.streamURL)))
            player.prepare()
            player.play()

            toast("Playing ${selectedRadio.name}")

        } else {
            toast(getString(R.string.no_internet_connection))
        }
    }

    private fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    private fun playPrevious() {
        val index = radioList.indexOf(selectedRadio)
        if (index > 0) {
            play(radioList[index - 1])
        } else {
            play(radioList[radioList.size - 1])
        }
    }

    private fun playNext() {
        val index = radioList.indexOf(selectedRadio)
        if (index < radioList.size - 1) {
            play(radioList[index + 1])
        } else {
            play(radioList[0])
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            return true
        }
        return false
    }

    enum class Actions {
        START, STOP
    }

    private var radioList = arrayListOf(
        Radio(
            "Радио 7",
            "https://top-radio.ru/assets/image/radio/180/7-Na-semi-xolmax.png",
            "https://radio7.hostingradio.ru:8040/radio7128.mp3"
        ),
        Radio(
            "Dance Wave",
            "https://dancewave.online/wp-content/uploads/2021/06/dwplay_mp3.png",
            "https://dancewave.online/dance.mp3"
        ),
        Radio(
            "DFM",
            "https://upload.wikimedia.org/wikipedia/ru/0/02/Dfm_logo.jpg",
            "https://dfm.hostingradio.ru/dfm96.aacp"
        ),
        Radio(
            "DNB FM",
            "https://top-radio.ru/assets/image/radio/180/dnbfm.png",
            "http://go.dnbfm.ru:8000/play"
        ),
        Radio(
            "Record",
            "https://top-radio.ru/assets/image/radio/180/radiorecod.png",
            "https://radiorecord.hostingradio.ru/rr_main96.aacp"
        ),
        Radio(
            "Искатель",
            "https://top-radio.ru/assets/image/radio/180/iskatel.png",
            "https://iskatel.hostingradio.ru:8015/iskatel-128.mp3"
        ),
        Radio(
            "Garage FM",
            "https://top-radio.ru/assets/image/radio/180/garagefm.png",
            "http://213.189.208.146:8005/Garagefm192"
        ),
        Radio(
            "Soundpark Deep",
            "https://www.radiobells.com/stations/soundparkdeep.jpg",
            "https://relay4.radiotoolkit.com/spdeep"
        ),
        Radio(
            "L-radio",
            "https://radiopotok.ru/f/station/512/68.png",
            "https://air.unmixed.ru/lradio256"
        ),
        Radio(
            "Noise FM",
            "https://noisefm.ru/wp-content/uploads/2021/05/noisefm_social_logo.jpg",
            "http://noisefm.ru:8000/play_256"
        ),
    )


    val mediaSessionCallback = object:MediaSessionCompat.Callback() {
        override fun onPlay() {
            play(selectedRadio)
        }

        override fun onPause() {
            Log.i("RADIO","onPause()")
        }

        override fun onStop() {
            Log.i("RADIO","onStop()")
        }

        override fun onSkipToNext() {
            Log.i("RADIO","onSkipToNext()")
        }

        override fun onSkipToPrevious() {
            Log.i("RADIO","onSkipToPrevious()")
        }
    }

}



object MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of [MediaMetadataCompat.getDescription] to extract the appropriate information.
     *
     * @param context      Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    fun from(
        context: Context?,
        mediaSession: MediaSessionCompat,
        channelId: String?,
    ): NotificationCompat.Builder {
        val controller = mediaSession.controller
        //val mediaMetadata = controller.metadata
        //val description = mediaMetadata.description
        val builder = NotificationCompat.Builder(
            context!!, channelId!!
        )
        builder
            .setContentTitle("description.title")
            .setContentText("description.subtitle")
            .setSubText("description.description")
            //.setLargeIcon(description.iconBitmap)
            .setContentIntent(controller.sessionActivity) //.setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return builder
    }
}