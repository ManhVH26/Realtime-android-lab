package com.example.realtime_android_lab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.realtime_android_lab.socket.ui.SocketDebugScreen
import com.example.realtime_android_lab.ui.theme.RealtimeandroidlabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RealtimeandroidlabTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        App()
                    }
                }
            }
        }
    }
}

/** Điều hướng tối giản bằng state (chưa cần Navigation lib cho lab này). */
@Composable
private fun App() {
    var screen by remember { mutableStateOf(Screen.Home) }
    when (screen) {
        Screen.Home -> HomeMenu(onOpen = { screen = it })
        Screen.Socket -> SocketDebugScreen(onBack = { screen = Screen.Home })
    }
}

private enum class Screen { Home, Socket }

@Composable
private fun HomeMenu(onOpen: (Screen) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Realtime Android Lab", style = MaterialTheme.typography.titleLarge)

        Button(onClick = { onOpen(Screen.Socket) }, modifier = Modifier.fillMaxWidth()) {
            Text("Bài 1 — WebSocket (reconnect + backoff)")
        }
        // Bài 2..5 chưa làm — để nút vô hiệu hoá làm chỗ giữ chỗ.
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Bài 2 — Chat local-first")
        }
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Bài 3 — WebRTC 1-1")
        }
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Bài 4 — Group call (SFU/Jitsi)")
        }
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Bài 5 — Streaming (Media3)")
        }
    }
}
