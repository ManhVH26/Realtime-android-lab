package com.example.realtime_android_lab.socket.ui

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * BÀI 1 — MVI View. Chỉ làm 2 việc:
 * 1. Vẽ theo [SocketUiState] (state đi xuống).
 * 2. Phát [SocketIntent] qua `onIntent` khi người dùng thao tác (intent đi lên).
 * Không giữ state cục bộ (kể cả URL), không gọi thẳng use case.
 * Thu [SocketEffect] một lần để hiện Toast.
 */
@Composable
fun SocketDebugScreen() {
    // koinViewModel() lấy ViewModel từ container Koin và vẫn gắn đúng ViewModelStoreOwner hiện
    // tại (Activity ở đây) — nghĩa là vẫn sống qua xoay màn hình như `viewModel()` thường.
    val viewModel: SocketDebugViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Context giờ CHỈ còn để hiện Toast, không còn dùng để dựng ViewModel nữa.
    val context = LocalContext.current

    // Effect một lần: thu bằng LaunchedEffect, hiện Toast. Không đưa vào state.
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SocketEffect.ShowMessage ->
                    Toast.makeText(context, effect.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bài 1 — WebSocket", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = state.url,
            onValueChange = { viewModel.onIntent(SocketIntent.UrlChanged(it)) },
            label = { Text("URL — hoặc chọn nhanh bên dưới") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        // Chọn nhanh URL: bấm chip = phát UrlChanged intent (không cần gõ tay).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            URL_PRESETS.forEach { preset ->
                FilterChip(
                    selected = state.url == preset.url,
                    onClick = { viewModel.onIntent(SocketIntent.UrlChanged(preset.url)) },
                    label = { Text(preset.label) },
                )
            }
        }

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

        // reverseLayout = true + danh sách đã đảo = kiểu chat: dòng MỚI NHẤT nằm DƯỚI CÙNG
        // và viewport tự neo ở đó, không phải cuộn tay khi log chạy.
        // (Comment cũ ghi "mới nhất nằm trên cùng" là sai — hai lần đảo triệt tiêu nhau.)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
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

private data class UrlPreset(val label: String, val url: String)

/**
 * URL chọn sẵn cho các kịch bản test. Server mock deploy trên Render.
 *
 * Chỉ `wss://` — không có preset `ws://` local. Hệ quả: không cần mở `usesCleartextTraffic`,
 * vì từ API 28 Android chặn cleartext mặc định và ta không còn chỗ nào cần nó.
 */
private val URL_PRESETS = listOf(
    UrlPreset("echo", "wss://realtime-ws-lab.onrender.com/echo"),
    UrlPreset("slow", "wss://realtime-ws-lab.onrender.com/slow"),
    UrlPreset("drop", "wss://realtime-ws-lab.onrender.com/drop"),
    UrlPreset("policy", "wss://realtime-ws-lab.onrender.com/policy"),
)
