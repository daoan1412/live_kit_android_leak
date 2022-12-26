package com.example.live_kit_memory_leak

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch

class VideoCallActivity: AppCompatActivity() {
    lateinit var room: Room

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)
        // Create Room object.
        room = LiveKit.create(applicationContext)
        // Setup the video renderer
        room.initVideoRenderer(findViewById<SurfaceViewRenderer>(R.id.renderer))

        requestNeededPermissions { connectToRoom() }
    }

    private fun connectToRoom() {
        val url = "ws://192.168.1.9:7880"
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2aWRlbyI6eyJyb29tSm9pbiI6dHJ1ZSwicm" +
                "9vbSI6IjUwMDE3ZDc3LTY2ZDMtNDEyNi1hNjJkLWFmNjY2YzI4YTFjNyJ9LCJpYXQiOjE2NzIwNjEwNzEsIm5iZiI6MTY3MjA2MTA3MSwiZXhwIjoxNjcyMDgyNjcxLCJpc3MiOiJBUElmUEViVm5oRH" +
                "UzQnIiLCJzdWIiOiJ0ZXJtaW5hbC1CMTRGMDA4SDAwMTAwMTk5IiwianRpIjoidGVybWluYWwtQjE0RjAwOEgwMDEwMDE5OSJ9.Djkm29WUne4LUzsWOtWxNnplb4wRBjSGHTfe9xNfAis"
//        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2aWRlbyI6eyJyb29tSm9pbiI6dHJ1ZSwicm9vbSI6IjUwMDE3ZDc3LTY2ZDMtNDEyNi1hNjJkLWFmNjY2YzI4YTFjNyJ9LCJpYXQiOjE2NzIw" +
//                "NjEwNzEsIm5iZiI6MTY3MjA2MTA3MSwiZXhwIjoxNjcyMDgyNjcxLCJpc3MiOiJBUElmUEViVm5oRHUzQnIiLCJzdWIiOiJ1c2VyLWZiN2I2Mjk0LTllOTYtNDM5My1hODI2LTBmMGRkMmRjZGQ5YiIs" +
//                "Imp0aSI6InVzZXItZmI3YjYyOTQtOWU5Ni00MzkzLWE4MjYtMGYwZGQyZGNkZDliIn0.P_2JQs1fs2oWXrjAqEwQXFPPhVRQ1bTSm7H6EH4wBKk"
        lifecycleScope.launch {

            // Setup event handling.
            launch {
                room.events.collect { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> onTrackSubscribed(event)
                        else -> {}
                    }
                }
            }

            // Connect to server.
            room.connect(
                url,
                token,
            )

            // Turn on audio/video recording.
            val localParticipant = room.localParticipant
            localParticipant.setMicrophoneEnabled(false)
            localParticipant.setCameraEnabled(true)
        }
    }

    private fun onTrackSubscribed(event: RoomEvent.TrackSubscribed) {
        val track = event.track
        if (track is VideoTrack) {
            attachVideo(track)
        }
    }

    private fun attachVideo(videoTrack: VideoTrack) {
        videoTrack.addRenderer(findViewById<SurfaceViewRenderer>(R.id.renderer))
        findViewById<View>(R.id.progress).visibility = View.GONE
    }

    private fun requestNeededPermissions(onHasPermissions: () -> Unit) {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
                var hasDenied = false
                // Check if any permissions weren't granted.
                for (grant in grants.entries) {
                    if (!grant.value) {
                        Toast.makeText(this, "Missing permission: ${grant.key}", Toast.LENGTH_SHORT).show()

                        hasDenied = true
                    }
                }

                if (!hasDenied) {
                    onHasPermissions()
                }
            }

        // Assemble the needed permissions to request
        val neededPermissions = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .let { perms ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Need BLUETOOTH_CONNECT permission on API S+ to output to bluetooth devices.
                    perms + listOf(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    perms
                }
            }
            .filter { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED }
            .toTypedArray()

        if (neededPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(neededPermissions)
        } else {
            onHasPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        room.disconnect()
    }
}