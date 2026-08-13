package com.example.realtime_android_lab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.realtime_android_lab.socket.ui.SocketDebugScreen
import com.example.realtime_android_lab.ui.theme.RealtimeandroidlabTheme

/**
 * App chỉ có MỘT màn hình ⇒ vào thẳng, không menu, không thư viện navigation.
 *
 * Bỏ luôn `Screen` enum + `HomeMenu`: điều hướng giữa 1 màn là indirection rỗng.
 * Thêm màn thứ hai thì lúc đó mới dựng navigation, và dựng bằng thứ phù hợp lúc đó.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RealtimeandroidlabTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SocketDebugScreen()
                    }
                }
            }
        }
    }
}
