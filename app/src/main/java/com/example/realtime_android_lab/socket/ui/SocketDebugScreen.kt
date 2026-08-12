package com.example.realtime_android_lab.socket.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * BÀI 1 — MVI View. Chỉ làm 2 việc:
 * 1. Vẽ theo [SocketUiState] (state đi xuống).
 * 2. Phát [SocketIntent] qua `onIntent` khi người dùng thao tác (intent đi lên).
 * Không giữ state cục bộ (kể cả URL), không gọi thẳng use case.
 * Thu [SocketEffect] một lần để hiện Toast.
 */
@Composable
fun SocketDebugScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SocketDebugViewModel = viewModel(
        factory = SocketDebugViewModel.factory(context),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Effect một lần: thu bằng LaunchedEffect, hiện Toast. Không đưa vào state.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SocketEffect.ShowMessage ->
                    Toast.makeText(context, effect.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Bài 1 — WebSocket", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = onBack) { Text("← Menu") }
        }

        OutlinedTextField(
            value = state.url,
            onValueChange = { viewModel.onIntent(SocketIntent.UrlChanged(it)) },
            label = { Text("URL (emulator: 10.0.2.2 — máy thật: IP LAN của PC)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        StatusCard(state)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { viewModel.onIntent(SocketIntent.Connect) },
                modifier = Modifier.weight(1f),
            ) { Text("Kết nối") }

            OutlinedButton(
                onClick = { viewModel.onIntent(SocketIntent.Disconnect) },
                modifier = Modifier.weight(1f),
            ) { Text("Ngắt") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { viewModel.onIntent(SocketIntent.SendTest) },
                modifier = Modifier.weight(1f),
            ) { Text("Gửi thử") }

            OutlinedButton(
                onClick = { viewModel.onIntent(SocketIntent.Ping) },
                modifier = Modifier.weight(1f),
            ) { Text("Ping (đo RTT)") }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Log", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true, // dòng mới nhất nằm trên cùng
        ) {
            items(state.log.reversed()) { line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusCard(state: SocketUiState) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = state.state.label(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Metric("Số lần retry", state.retryCount.toString())
                Metric("Delay lần tới", "${state.nextDelayMs}ms")
                Metric("RTT gần nhất", state.lastRttMs?.let { "${it}ms" } ?: "—")
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
