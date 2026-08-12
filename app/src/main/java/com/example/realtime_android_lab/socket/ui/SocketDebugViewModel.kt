package com.example.realtime_android_lab.socket.ui

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.realtime_android_lab.socket.data.AndroidNetworkMonitor
import com.example.realtime_android_lab.socket.data.RealtimeRepositoryImpl
import com.example.realtime_android_lab.socket.domain.BackoffPolicy
import com.example.realtime_android_lab.socket.domain.model.ConnectionEvent
import com.example.realtime_android_lab.socket.domain.model.ConnectionState
import com.example.realtime_android_lab.socket.domain.usecase.ConnectUseCase
import com.example.realtime_android_lab.socket.domain.usecase.DisconnectUseCase
import com.example.realtime_android_lab.socket.domain.usecase.ObserveConnectionUseCase
import com.example.realtime_android_lab.socket.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * BÀI 1 — MVI ViewModel.
 *
 * Luồng một chiều: View phát [SocketIntent] qua [onIntent] → xử lý (gọi use case) →
 * sinh [Change] → [reduce] thuần dựng [SocketUiState] mới → View vẽ lại.
 * Sự kiện một lần (toast) đi qua [effects], KHÔNG nằm trong state.
 *
 * ViewModel chỉ phụ thuộc use case của DOMAIN — không biết OkHttp/ConnectivityManager.
 */
class SocketDebugViewModel(
    private val observeConnection: ObserveConnectionUseCase,
    private val connectUseCase: ConnectUseCase,
    private val disconnectUseCase: DisconnectUseCase,
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SocketUiState())
    val state: StateFlow<SocketUiState> = _state.asStateFlow()

    private val _effects = Channel<SocketEffect>(Channel.BUFFERED)
    val effects: Flow<SocketEffect> = _effects.receiveAsFlow()

    private var pingJob: Job? = null
    private var lastPingSentAt = 0L

    init {
        // Nguồn thứ hai (ngoài Intent) đổ vào cùng một reducer: sự kiện từ repository.
        viewModelScope.launch {
            observeConnection().collect(::onConnectionEvent)
        }
    }

    /** Cửa vào DUY NHẤT của mọi ý định người dùng. */
    fun onIntent(intent: SocketIntent) {
        when (intent) {
            is SocketIntent.UrlChanged -> dispatch(Change.Url(intent.url))
            SocketIntent.Connect -> connect()
            SocketIntent.Disconnect -> disconnect()
            SocketIntent.SendTest -> sendTest()
            SocketIntent.Ping -> ping()
        }
    }

    // ----- xử lý intent (side effect: gọi use case) rồi phát Change -----

    private fun connect() {
        val url = _state.value.url
        dispatch(Change.Log("bắt đầu kết nối tới $url"))
        connectUseCase(url)
        startPingLoop()
    }

    private fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        disconnectUseCase()
        dispatch(Change.Log("người dùng ngắt kết nối"))
    }

    private fun sendTest() {
        val ok = sendMessage("hello ${_state.value.log.size}")
        if (ok) {
            dispatch(Change.Log("gửi: hello"))
        } else {
            _effects.trySend(SocketEffect.ShowMessage("Chưa kết nối, không gửi được"))
        }
    }

    private fun ping() {
        lastPingSentAt = SystemClock.elapsedRealtime()
        if (sendMessage("PING:$lastPingSentAt")) dispatch(Change.Log("ping…"))
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            while (isActive) {
                delay(PING_EVERY_MS)
                ping()
            }
        }
    }

    private fun onConnectionEvent(event: ConnectionEvent) {
        when (event) {
            is ConnectionEvent.StateChanged -> {
                dispatch(Change.State(event.state))
                dispatch(Change.Log("state: ${event.state.label()}"))
            }

            is ConnectionEvent.Message -> onMessage(event.text)
        }
    }

    private fun onMessage(text: String) {
        if (text.startsWith("PING:")) {
            val rtt = SystemClock.elapsedRealtime() - lastPingSentAt
            dispatch(Change.Rtt(rtt))
            dispatch(Change.Log("pong ← RTT = ${rtt}ms"))
        } else {
            dispatch(Change.Log("nhận: $text"))
        }
    }

    // ----- reducer: (State, Change) -> State, THUẦN, là chỗ duy nhất tạo state mới -----

    private fun dispatch(change: Change) = _state.update { reduce(it, change) }

    private fun reduce(s: SocketUiState, change: Change): SocketUiState = when (change) {
        is Change.Url -> s.copy(url = change.url)
        is Change.State -> s.copy(
            state = change.state,
            retryCount = (change.state as? ConnectionState.Reconnecting)?.attempt ?: s.retryCount,
            nextDelayMs = (change.state as? ConnectionState.Reconnecting)?.nextDelayMs ?: 0,
        )
        is Change.Rtt -> s.copy(lastRttMs = change.rttMs)
        is Change.Log -> s.copy(log = (s.log + change.line).takeLast(MAX_LOG_LINES))
    }

    /** Thay đổi từng phần của state — đầu vào của reducer. */
    private sealed interface Change {
        data class Url(val url: String) : Change
        data class State(val state: ConnectionState) : Change
        data class Rtt(val rttMs: Long) : Change
        data class Log(val line: String) : Change
    }

    override fun onCleared() {
        disconnectUseCase()
    }

    companion object {
        private const val PING_EVERY_MS = 10_000L
        private const val MAX_LOG_LINES = 100

        /** DI thủ công (lab không dùng Hilt): dựng graph data→domain→ui cho `viewModel()`. */
        fun factory(context: Context) = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                val repository = RealtimeRepositoryImpl(
                    backoff = BackoffPolicy(),
                    networkMonitor = AndroidNetworkMonitor(appContext),
                )
                SocketDebugViewModel(
                    observeConnection = ObserveConnectionUseCase(repository),
                    connectUseCase = ConnectUseCase(repository),
                    disconnectUseCase = DisconnectUseCase(repository),
                    sendMessage = SendMessageUseCase(repository),
                )
            }
        }
    }
}
