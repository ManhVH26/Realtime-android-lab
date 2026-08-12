# Research + Ôn phỏng vấn: Viettel Tammi (`com.viettel.appviettel`)

> Chuẩn bị cho phỏng vấn team làm app Viettel Tammi. HR gợi ý ôn: **WebSocket, chat, video call, streaming**.
> Ngày research: 12/08/2026.
>
> **Đây là tài liệu để TÔI đọc ôn** (dài, đọc dần). Phân biệt với các file khác trong `docs/`:
> - `00_BOI_CANH_PHONG_VAN.md` — bản rút gọn cho Claude, để nó biết bối cảnh mà không phải đọc file này
> - `01_LO_TRINH_5_BAI.md` — **đề bài cho Claude**, biến kiến thức ở đây thành code + bài tập
> - `02_CHECKLIST_QUIZ.md` — dùng để Claude quiz ngược tôi
> - `03_SO_LIEU_DO_DUOC.md` — nơi ghi số liệu tôi tự đo

---

## 1. App này là app gì? (chốt trước, vì dễ nhầm)

| | |
|---|---|
| **Tên** | **Viettel Tammi** (Tammi by Viettel) — KHÔNG phải "My Viettel" |
| **Package** | `com.viettel.appviettel` |
| **Chủ sở hữu** | **Viettel Telecom** (Tổng công ty Viễn thông Viettel) |
| **Ra mắt** | 15/04/2026 |
| **Category trên Play** | **Communication** (không phải Business/Lifestyle → nói lên trọng tâm sản phẩm) |
| **Version khi research** | 1.1.24, cập nhật 23/04/2026 |
| **Size** | ~197–210 MB (XAPK: base + `config.arm64_v8a` + `config.en` + `config.xxhdpi`) → **Android App Bundle, split APK** |
| **minSdk** | Android 10 (API 29) |
| **Cài đặt** | 100.000+ trên Play; báo chí/đối tác nói **1 triệu user tính đến 07/2026** |
| **Nền tảng khác** | iOS, **Web: `tammi.vn/chat`**, **Desktop Windows + macOS** |

**Định vị sản phẩm:** siêu app "1 app thay 30–50 app", tích hợp ~50 dịch vụ / 10 lĩnh vực (viễn thông, y tế 43 bệnh viện, giáo dục, mua sắm, thanh toán hoá đơn, gọi xe Grab/Be/Xanh SM, giao hàng, giải trí, IoT: loa thông minh / robot trẻ em / nhẫn sức khoẻ / camera).

**Nhưng trái tim kỹ thuật là khối OTT** (đây là phần anh sẽ bị hỏi):
- Chat 1-1, chat nhóm
- Voice call, video call
- **Meeting online** (họp nhóm nhiều người)
- **Gọi/nhắn/họp không tốn data** cho user Viettel
- **Lọc ồn bằng AI** — loại bỏ 300+ loại tiếng ồn (giao thông, gió, công trường, quán cafe, đám đông)
- **AI Agent** trợ lý cá nhân trong app
- Định vị "nền tảng liên lạc **xác thực định danh** đầu tiên VN" — account gắn với số thuê bao đã xác thực (khác Zalo/Messenger)

> ⚠️ **Tammi là con nối nghiệp của Mocha** (`com.viettel.mocha`, OTT chat/call/nhạc/stream của Viettel Media, 20M+ user). Kinh nghiệm, codebase và nhân sự realtime của Viettel gần như chắc chắn đi từ Mocha sang. Nếu phỏng vấn có nhắc Mocha — anh nên biết nó là gì.

---

## 2. Tech stack — cái gì CHẮC, cái gì SUY LUẬN

Tôi không decompile APK (chỉ dùng Play Store + JD tuyển dụng của Viettel + báo chí), nên tách rõ 2 cột để anh không bị "nói chắc điều mình đoán" khi phỏng vấn.

### 2.1 ✅ Xác nhận được (từ JD Android Developer của Viettel + store listing)

JD Android Developer của Viettel (tuyendung.viettel.vn, mã 41746) yêu cầu **đúng những gì HR nói với anh**:

| Hạng mục | Nội dung |
|---|---|
| Ngôn ngữ | **Kotlin / Java**, tối thiểu 2 năm |
| Kiến trúc | **MVC, MVVM, Clean Architecture** + design patterns phổ biến |
| Network | **RESTful API**, third-party SDK, **Firebase services** |
| Realtime (ưu tiên) | **WebRTC / WebSocket / XMPP** hoặc ứng dụng realtime |
| Streaming (ưu tiên) | **Live/Video Streaming — đặc biệt Media3, ExoPlayer** |
| SDK cụ thể được nêu | **tích hợp Jitsi SDK** cho chat, video, OTT, **gọi nhóm**, nhạc |
| Senior | ra quyết định kiến trúc, tối ưu performance (**multi-threading, memory, Jetpack components**), đảm bảo mượt trên đa thiết bị |

→ **Đây là native Android (Kotlin/Java), không phải Flutter/React Native.** (Viettel có JD React Native nhưng ở đơn vị khác — Viettel Software Services.)

### 2.2 🔶 Suy luận có căn cứ (nói dưới dạng "em đoán/em hiểu là", đừng khẳng định)

| Mảng | Suy luận | Căn cứ |
|---|---|---|
| Chat signaling | **XMPP** (stanza message/presence/IQ, MUC cho group) chạy trên **WebSocket** (RFC 7395) | JD nêu XMPP; Mocha vốn dùng XMPP; server phổ biến: Openfire / ejabberd / Tigase |
| Call 1-1 | **WebRTC** P2P, signaling qua WebSocket hoặc **XMPP Jingle (XEP-0166)** | JD nêu WebRTC + XMPP |
| Meeting nhóm | **Jitsi Videobridge (SFU)** — routing chứ không mix | JD nêu Jitsi SDK + "gọi nhóm" |
| Live/video content | **Media3/ExoPlayer + HLS/DASH**, ABR | JD nêu Media3/ExoPlayer |
| Push | **FCM high-priority** để wake app khi có cuộc gọi/tin nhắn ở background | JD nêu Firebase; đây là cách duy nhất khả thi trên Android hiện đại |
| "Không tốn data" | **Zero-rating trên hạ tầng Viettel** (whitelist IP/APN, không tính data cho traffic tới media/signaling server của Tammi) — không phải trick giao thức. Kéo theo hệ quả kỹ thuật: **TURN/media server phải nằm trong dải IP được zero-rate**, ảnh hưởng lựa chọn ICE. | Báo chí: "Viettel làm chủ hạ tầng viễn thông" |
| Lọc ồn AI | **DNN noise suppression on-device** thay cho NS cổ điển của WebRTC, chạy trên frame 10–20ms trong audio pipeline (kiểu RNNoise/DTLN/Krisp) | Marketing "300+ loại tiếng ồn" ⇒ model học từ dataset nhiễu, không phải rule-based |
| Desktop app | Web client (`tammi.vn/chat`) đóng gói **Electron** | Có bản Win + macOS song song web |
| Kiến trúc app | **multi-module Gradle**, và ~50 dịch vụ chắc chắn không viết native hết ⇒ có **mini-app / H5 container + JSBridge**, deeplink routing, Remote Config | 210MB + 50 dịch vụ + tốc độ ra mắt |

### 2.3 🎯 Ba "mùi" kỹ thuật để anh ghi điểm khi nhận xét về app

Nhận xét được vấn đề thật = điểm cộng cực lớn, hơn là kể tên thư viện:

1. **APK ~200MB là rất lớn cho một app Communication.** Nguyên nhân điển hình: Jitsi Meet SDK Android **bên trong nhúng React Native** + native libs WebRTC (`libjingle_peerconnection.so`) cho nhiều ABI + assets sticker/emoji + nhiều mini-app. Hỏi: "Team đang xử lý APK size thế nào — dynamic feature module, R8 full mode, tách ABI?"
2. **minSdk 29 (Android 10)** — khá cao. Nghĩa là team chọn cắt máy cũ để dùng được API mới (Telecom, `AudioManager` device routing, `CallStyle`, foreground service types) thay vì gánh compat. Là quyết định kiến trúc hợp lý — nói được điều này chứng tỏ anh đọc được ý đồ.
3. **Zero-rating + WebRTC là mâu thuẫn thú vị:** WebRTC vốn muốn đi P2P (rẻ, latency thấp), nhưng P2P thì traffic đi ra IP của user khác ⇒ **không zero-rate được**. Muốn miễn data thì phải **ép relay qua TURN/SFU của Viettel** (`iceTransportPolicy: relay`). Đây là đánh đổi tiền băng thông server để lấy USP "không tốn data". Nếu anh nêu được điểm này, gần như chắc chắn gây ấn tượng.

---

## 3. WebSocket — ôn phần này trước, dễ bị hỏi nhất

### 3.1 Nền tảng phải nói trôi chảy

**Handshake (RFC 6455):** client gửi HTTP GET kèm `Upgrade: websocket`, `Connection: Upgrade`, `Sec-WebSocket-Key` → server trả **101 Switching Protocols** + `Sec-WebSocket-Accept = base64(SHA1(key + GUID))`. Sau đó **cùng một TCP connection** chuyển sang frame nhị phân, full-duplex, không còn HTTP header mỗi message.

**Frame:** opcode `0x1` text, `0x2` binary, `0x8` close, `0x9` ping, `0xA` pong; fragmentation qua bit FIN; **client→server luôn phải mask**. Có `permessage-deflate` để nén, `Sec-WebSocket-Protocol` để chọn subprotocol (vd `xmpp`).

**So sánh (câu hỏi kinh điển):**

| | Hướng | Overhead | Khi nào dùng |
|---|---|---|---|
| Long-polling | Half | Cao (HTTP mỗi lần) | Fallback cuối |
| SSE | Server→Client | Thấp | Feed/notification 1 chiều |
| **WebSocket** | Full-duplex | Rất thấp sau handshake | **Chat, signaling, presence** |
| gRPC bidi stream | Full-duplex | Thấp, cần HTTP/2 | Service-to-service |
| MQTT | Full-duplex, pub/sub, QoS 0/1/2 | Rất thấp | IoT, mobile tiết kiệm pin |

*(Tammi có IoT → biết MQTT là điểm cộng.)*

### 3.2 Những thứ chỉ dân làm thật mới biết — nói ra là khác biệt ngay

- **Keepalive:** carrier NAT của mạng di động **drop TCP idle sau ~2–5 phút**. Phải ping định kỳ (OkHttp: `pingInterval(20, SECONDS)`) để giữ mapping NAT và **phát hiện chết sớm** (half-open connection: TCP tưởng còn sống nhưng thực tế đã đứt — chỉ ping/pong timeout mới lộ ra).
- **Reconnect:** exponential backoff **+ jitter** (`min(cap, base * 2^n) * random(0.5..1.5)`). Không có jitter → **thundering herd**: mất mạng vùng, cả triệu client reconnect cùng giây, sập gateway.
- **Ordering:** TCP đảm bảo thứ tự *trong một connection*. **Qua reconnect thì không.** Giải pháp: server gán **`seq` tăng dần theo từng conversation**; client giữ `lastSeq`, reconnect thì gửi lên để **delta sync** phần bị hụt.
- **Idempotency:** client tự sinh `clientMsgId` (UUID) → gửi lại khi timeout không tạo tin nhắn trùng; server dedupe theo key đó. Kinh điển: user thấy tin nhắn nhân đôi khi mạng chập chờn.
- **Ack ở tầng app:** WebSocket không có ack cho từng message. Muốn "đã gửi thành công" phải tự làm ack (XMPP có sẵn: **Stream Management XEP-0198** cho ack + resume session).
- **Backpressure:** `webSocket.queueSize()` phình = mạng không đẩy kịp. Với typing indicator / presence phải **throttle + coalesce** (bỏ event cũ, chỉ giữ cái mới nhất), không được queue hết.
- **Auth:** không nên nhét token vào query string (lộ trong log). Cách tốt: header trong handshake, hoặc gửi frame `AUTH` đầu tiên. Token hết hạn giữa phiên → server đóng với close code riêng → client refresh rồi reconnect.

### 3.3 Vòng đời Android — chỗ này anh có lợi thế sân nhà, khai thác mạnh

Đây là phần **anh mạnh hơn ứng viên backend**, hãy chủ động kéo về:

- **Không thể giữ WebSocket sống ở background.** Android 8+ background execution limits + **Doze/App Standby** cắt network khi màn hình tắt lâu. ⇒ Kiến trúc thật: **socket sống khi app foreground; background thì dựa vào FCM high-priority push** để wake app rồi reconnect + fetch.
- **Đang trong cuộc gọi** thì cần **foreground service**, và từ Android 14 phải khai báo type: `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_CAMERA`, hoặc dùng `ConnectionService` của Telecom framework.
- **Pin:** mỗi lần ping làm radio thức (RRC state promotion), tốn pin hơn CPU. ⇒ đồng bộ ping với các network activity khác, nới `pingInterval` khi app ở background/screen off.
- **Network change:** dùng `ConnectivityManager.NetworkCallback` (Wi-Fi ⇄ 4G) để reconnect chủ động thay vì đợi timeout. Cân nhắc `MultipathPolicy`/network handover.
- **Lifecycle-aware:** socket nên do một component gắn `ProcessLifecycleOwner` quản lý, không phải Activity.

### 3.4 Code mẫu Kotlin (nắm chắc, có thể bị bảo viết trên giấy)

```kotlin
// Socket layer: expose Flow, ViewModel collect — cách làm chuẩn hiện nay
class ChatSocket(private val client: OkHttpClient, private val url: String) {

    fun events(): Flow<SocketEvent> = callbackFlow {
        var attempt = 0
        var ws: WebSocket? = null

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                attempt = 0                       // reset backoff khi mở thành công
                trySend(SocketEvent.Connected)
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                trySend(SocketEvent.Message(text))
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, r: Response?) {
                trySend(SocketEvent.Disconnected(t))
                close(t)                          // để retry ở tầng ngoài xử lý
            }
        }
        ws = client.newWebSocket(Request.Builder().url(url).build(), listener)

        awaitClose { ws?.close(1000, "lifecycle") }   // hủy coroutine ⇒ đóng socket
    }.retryWhen { cause, attempt ->
        val delayMs = (1000L shl attempt.toInt().coerceAtMost(6))   // 1s → 64s
        val jitter = (delayMs * 0.5 * Random.nextDouble()).toLong()
        delay(delayMs + jitter)
        true
    }
}

// OkHttp client cho socket
OkHttpClient.Builder()
    .pingInterval(20, TimeUnit.SECONDS)   // giữ NAT + phát hiện half-open
    .build()
```

**Điểm đáng nói khi trình bày:** `callbackFlow` + `awaitClose` = socket tự chết theo scope; `retryWhen` = backoff tập trung một chỗ. So với Kotlin thuần anh đã quen: đây chính là pattern biến callback thành cold stream.

---

## 4. Chat — thiết kế hệ thống (khả năng cao bị hỏi system design nhẹ)

### 4.1 Local-first là câu trả lời đúng

**Room là Single Source of Truth, không phải network.** UI chỉ observe `Flow<List<Message>>` từ Room. Socket/REST chỉ ghi vào Room.

Lý do phải nói được: mở app là **thấy tin nhắn ngay** (0 spinner), gửi tin khi offline vẫn vào được, và **không có 2 nguồn sự thật** để lệch nhau.

**Outbox pattern cho tin gửi đi:**
```
Message(state = PENDING) → insert Room → UI hiện ngay (optimistic)
        ↓ socket send
    ACK từ server (kèm serverId, seq, serverTime) → update state = SENT
        ↓ timeout / lỗi
    state = FAILED → UI cho nút "gửi lại" (dùng lại clientMsgId cũ ⇒ idempotent)
```

### 4.2 Các bài toán khó — biết là ăn điểm

| Bài toán | Cách xử lý |
|---|---|
| **Trạng thái tin nhắn** | `PENDING → SENT → DELIVERED → SEEN` (XMPP: XEP-0184 receipts) |
| **Đồng bộ sau offline** | Gửi `lastSeq` per-conversation → server trả delta. XMPP có **MAM (XEP-0313)** archive |
| **Sort ổn định** | Sort theo `serverSeq`, KHÔNG theo `System.currentTimeMillis()` của client (đồng hồ máy user sai là chuyện thường) |
| **Typing indicator** | XEP-0085 chat states; **throttle 2–3s**, không gửi mỗi keystroke |
| **Read receipt trong group 500 người** | Không fanout N receipt/người. Gửi **count tổng hợp** + danh sách lazy khi user bấm xem |
| **Phân trang** | **Keyset pagination theo `seq`** (`WHERE seq < :cursor ORDER BY seq DESC LIMIT 30`), không dùng OFFSET. Paging 3 + `RemoteMediator` |
| **Ảnh/file** | Không đẩy qua WebSocket. Upload lên object storage bằng **presigned URL** (resumable), rồi gửi message chứa URL. Retry bằng **WorkManager** (sống qua process death) |
| **Chat nhóm fanout** | Server ghi 1 lần vào conversation, N client đọc theo seq (fan-out on read) — không nhân bản N bản ghi |
| **Xoá/sửa/thu hồi tin** | Tombstone record + version, không DELETE thật (vì client khác cần biết là "đã thu hồi") |

### 4.3 XMPP — vì JD nêu tên, phải nắm từ vựng

- **JID:** `user@domain/resource` (resource = từng thiết bị của cùng user ⇒ multi-device)
- **3 stanza:** `<message>` (fire & forget), `<presence>` (online/offline/typing), `<iq>` (request/response)
- **MUC — XEP-0045:** multi-user chat = chat nhóm
- **Jingle — XEP-0166:** signaling cho voice/video call (SDP offer/answer bọc trong XMPP)
- **Stream Management — XEP-0198:** ack + **resume session** sau khi mất mạng (cực quan trọng cho mobile: không phải làm lại handshake + auth)
- **Carbons — XEP-0280:** đồng bộ tin nhắn giữa các thiết bị của cùng user
- **XMPP over WebSocket — RFC 7395** (thay cho BOSH XEP-0124 kiểu long-polling cũ)
- **Library Android:** Smack (thuần Java, phổ biến nhất)
- **E2EE:** OMEMO (XEP-0384, dựa Signal protocol) — nhưng telco có "định danh xác thực" thường **không** làm E2EE vì lý do quản lý. **Đây là câu hay để hỏi lại họ.**

---

## 5. Video call — WebRTC

### 5.1 Luồng phải vẽ được trên bảng

```
A                        Signaling server (WebSocket)                     B
│──── offer (SDP) ──────────────────►│──────────────────────────────────►│
│◄─────────────────────────────────  │◄──────── answer (SDP) ────────────│
│◄──── ICE candidates (trickle, cả 2 chiều, liên tục) ─────────────────►│
│                                                                        │
│════════════ media: SRTP/DTLS, đi TRỰC TIẾP không qua signaling ══════►│
```

**Điểm cốt lõi phải nhấn:** **WebRTC không định nghĩa signaling.** Đó là lý do team phải tự làm bằng WebSocket/XMPP — và cũng là lý do JD gộp "WebRTC/WebSocket/XMPP" thành một dòng.

### 5.2 NAT traversal — hỏi 10 lần thì 8 lần có

- **STUN:** "IP public của tôi là gì?" → server trả về địa chỉ reflexive. Nhẹ, gần như free.
- **TURN:** relay toàn bộ media qua server khi **symmetric NAT / firewall doanh nghiệp** chặn P2P. **Tốn băng thông thật.** Thực tế ~10–20% cuộc gọi cần TURN; fallback cuối là **TURN over TCP/TLS port 443** (nhìn như HTTPS nên qua được firewall khó).
- **ICE:** thu thập candidate (host / srflx / relay), ghép cặp, ping STUN connectivity check, chọn cặp tốt nhất theo priority. **Trickle ICE** = gửi candidate dần thay vì đợi gom hết ⇒ giảm thời gian setup call rõ rệt.
- **Mã hoá là bắt buộc:** DTLS handshake → khoá cho **SRTP**. WebRTC không cho phép media không mã hoá.

### 5.3 Group call: mesh vs MCU vs SFU (Jitsi ở đâu)

| | Cách hoạt động | Upload của client | Điểm chết |
|---|---|---|---|
| **Mesh (P2P full)** | mỗi người gửi cho mọi người | **(N-1) × bitrate** | Sập ở ~4 người, mobile nóng máy tụt pin |
| **MCU** | server **giải mã + trộn** thành 1 stream | 1 × | CPU server cực đắt, thêm latency, mất layout linh hoạt |
| **SFU** ✅ | server **chỉ route** packet có chọn lọc, không decode | 1 × (có simulcast: 2–3 layer) | Download vẫn tăng theo N ⇒ cần "last-N" |

**Jitsi Videobridge = SFU.** Các tính năng của nó nên biết tên:
- **Simulcast:** client encode 3 độ phân giải (180p/360p/720p) cùng lúc, SFU chọn layer phù hợp cho từng người nhận theo băng thông của họ.
- **Dominant speaker detection:** phát hiện ai đang nói để đưa lên khung lớn.
- **Last-N:** chỉ forward video của N người gần đây nhất → họp 50 người vẫn chạy được trên mobile.
- **E2EE qua Insertable Streams** (mã hoá thêm một lớp mà SFU không đọc được).

⚠️ **Nhận xét đáng giá:** **Jitsi Meet SDK cho Android nhúng React Native bên trong**. Đó là lý do rất có thể APK phình ~200MB, và cũng là lý do team có thể đang phải dùng `lib-jitsi-meet` / tự tích hợp JVB thay vì SDK đóng gói. Hỏi câu này là chạm đúng nỗi đau thật của họ.

### 5.4 Chất lượng & thích ứng mạng

- **Codec:** audio **Opus** (48kHz, in-band FEC, DTX tắt tiếng khi im lặng). Video **VP8 / VP9 / H.264 / AV1** — mobile ưu tiên **H.264 để dùng hardware encoder** (đỡ nóng, đỡ tốn pin), nhưng H.264 simulcast trên Android hay bị hạn chế → đánh đổi thật.
- **Congestion control:** **GCC** + **TWCC** (transport-wide congestion control) ước lượng băng thông khả dụng → tự hạ bitrate/resolution/framerate. `degradationPreference`: giữ resolution hay giữ framerate (share màn hình → giữ resolution; nói chuyện → giữ framerate).
- **Chống mất gói:** NACK (xin gửi lại), **FEC**, **PLI/keyframe request**, **jitter buffer** (đánh đổi latency ↔ độ mượt).
- **Audio pipeline:** AEC (chống vọng — ưu tiên HW AEC của thiết bị), AGC, NS, VAD. **"Lọc ồn AI" của Tammi = thay khối NS cổ điển bằng một DNN nhỏ chạy on-device.**
- **Đo lường:** `peerConnection.getStats()` → RTT, jitter, `packetsLost`, `freezeCount`, `framesDropped`, bitrate thực. Biết tên field cụ thể chứng minh anh từng debug call thật.

### 5.5 Android-specific — vũ khí riêng của anh

| Vấn đề | API/giải pháp |
|---|---|
| Cuộc gọi đến khi app đã bị kill | **FCM high-priority data message** → wake app → connect socket → hiện UI |
| UI cuộc gọi đến | **`Notification.CallStyle`** (Android 12+) + **full-screen intent** (Android 14 chỉ cho phép app calling/alarm dùng `USE_FULL_SCREEN_INTENT`) |
| Tích hợp với hệ thống (call log, chặn khi đang gọi GSM) | **`ConnectionService` / Telecom framework** |
| Giữ call sống | Foreground service với type `microphone` + `camera` (bắt buộc khai báo từ Android 14) |
| Routing âm thanh | `AudioManager.MODE_IN_COMMUNICATION`, audio focus, Bluetooth SCO, `setCommunicationDevice()` (API 31+) |
| Áp tai thì tắt màn hình | Proximity sensor + `PROXIMITY_SCREEN_OFF_WAKE_LOCK` |
| Camera | `Camera2Enumerator` + `SurfaceTextureHelper` của WebRTC, render bằng `SurfaceViewRenderer` / `EglBase` |
| Đổi mạng giữa cuộc gọi | **ICE restart** (Wi-Fi → 4G) |

---

## 6. Streaming — Media3 / ExoPlayer

### 6.1 Chọn giao thức theo latency (bảng này nên thuộc)

| Giao thức | Latency | Dùng khi |
|---|---|---|
| RTMP / **SRT** | ingest (push lên server) | Đường từ người phát → server |
| **HLS** (TS/CMAF) | 15–30s | VOD, live thường, tương thích rộng nhất |
| **DASH** | 10–30s | Android/web, DRM linh hoạt |
| **LL-HLS / LL-DASH (CMAF chunked)** | 2–5s | Live thể thao, bán tương tác |
| **WebRTC** | < 500ms | Call, meeting, đấu giá, live 2 chiều |

**Quy tắc trả lời:** latency thấp thì đắt và khó scale (WebRTC cần SFU/TURN, không CDN-friendly); HLS/DASH đi qua CDN nên rẻ và scale vô hạn nhưng chậm. **Tammi có cả hai loại nhu cầu** → chọn theo tính năng, không chọn theo "cái nào xịn hơn".

### 6.2 Media3 — những gì cần biết cụ thể

- **Media3 (`androidx.media3`) đã thay ExoPlayer 2 (`com.google.android.exoplayer2`)** — ExoPlayer giờ là một implementation của interface `Player`. Nếu họ hỏi "ExoPlayer hay Media3", trả lời được điều này là đúng thời sự.
- **Kiến trúc:** `MediaSource` (HLS/DASH/Progressive) → `TrackSelector` → `Renderer` → `MediaCodec` (hardware decode ra `Surface`).
- **ABR:** `AdaptiveTrackSelection` quyết định đổi chất lượng dựa trên `BandwidthMeter` + mức buffer hiện tại. Tuning `LoadControl` (min/max buffer) là đánh đổi **startup nhanh ↔ ít rebuffer**.
- **Live:** `MediaItem.LiveConfiguration` với `targetOffsetMs`; player **điều chỉnh tốc độ phát rất nhẹ (±5%)** để kéo về đúng offset thay vì seek giật.
- **DRM:** Widevine **L1 (hardware, secure decoder) / L3 (software)**, `MediaDrm`, offline license cho tải về xem sau.
- **Cache/offline:** `DownloadManager` + `CacheDataSource`.
- **Feed video kiểu reels:** **player pool + preload** item kế tiếp; không tạo/hủy ExoPlayer mỗi lần scroll (đây là câu hỏi thực tế rất hay gặp).
- **Metrics phải đo:** startup time (**TTFF** — time to first frame), **rebuffer ratio**, average bitrate, playback error rate → qua `AnalyticsListener`.

---

## 7. Bộ câu hỏi dự đoán + gợi ý trả lời

### 7.1 WebSocket

**Q: WebSocket khác HTTP polling ở đâu, khi nào KHÔNG nên dùng WebSocket?**
> Full-duplex, một TCP, không header lặp lại. Không nên dùng khi: dữ liệu 1 chiều thưa (SSE/push đủ), hoặc app ở background trên Android (socket bị Doze cắt → dùng FCM), hoặc request/response đơn giản (REST dễ cache, dễ load-balance, dễ debug hơn).

**Q: App đang chạy, user vào hầm rồi ra — anh xử lý thế nào?**
> Ping/pong phát hiện đứt (không đợi TCP timeout). Reconnect backoff + jitter. Reconnect xong gửi `lastSeq` để delta sync. Tin đang gửi nằm ở outbox `PENDING` với `clientMsgId` → gửi lại idempotent. Nếu dùng XMPP thì XEP-0198 resume session, đỡ auth lại.

**Q: Làm sao đảm bảo không mất và không trùng tin nhắn?**
> Không mất: outbox trong Room + ack tầng app + delta sync theo seq khi reconnect. Không trùng: `clientMsgId` do client sinh, server dedupe. Thứ tự: sort theo `serverSeq` chứ không theo timestamp client.

**Q: 1 triệu user online, server scale thế nào?** (họ có thể hỏi để xem tầm nhìn)
> Nhiều WebSocket gateway stateless phía sau LB (cần sticky hoặc connection registry), gateway ↔ backend qua pub/sub (Redis/Kafka), presence trong Redis với TTL. Bottleneck thường là số file descriptor + memory/connection, và fanout.

### 7.2 Chat

**Q: Thiết kế màn hình chat cho app hàng triệu user.**
> Room là SSOT → `Flow` → UI. Optimistic send + outbox. Keyset pagination theo seq + Paging 3 `RemoteMediator`. Ảnh/file upload riêng qua presigned URL + WorkManager. Socket khi foreground, FCM khi background. Typing throttle. Read receipt group thì aggregate.

**Q: Vì sao không dùng OFFSET để phân trang tin nhắn?**
> Tin mới chèn liên tục làm offset lệch → tin bị lặp hoặc bị nhảy. Keyset (`seq < cursor`) ổn định và dùng được index.

### 7.3 Video call

**Q: Giải thích luồng thiết lập một cuộc gọi WebRTC.**
> (Vẽ sơ đồ ở mục 5.1) — nhấn: signaling do mình tự làm, media đi riêng, ICE trickle để giảm thời gian setup, DTLS-SRTP bắt buộc.

**Q: Gọi nhóm 20 người thì làm sao?**
> Không mesh (upload 19×). SFU (Jitsi Videobridge): 1 upload có simulcast, server route theo băng thông từng người, last-N + dominant speaker để mobile chịu được.

**Q: Cuộc gọi bị vỡ tiếng/giật hình, anh debug thế nào?**
> `getStats()`: RTT, jitter, packetsLost, freezeCount, bitrate thực → phân biệt nghẽn mạng (bitrate tụt, TWCC hạ) vs thiếu CPU (framesDropped, encode time cao) vs codec/hardware. Kiểm tra có bị fallback sang TURN không (relay ⇒ RTT cao hơn). Log theo mạng (Wi-Fi/4G), theo model máy.

**Q: App bị kill mà vẫn nhận được cuộc gọi đến bằng cách nào?**
> FCM high-priority data message (bypass Doze) → wake process → hiện `CallStyle` notification + full-screen intent → user bấm nhận → mới connect socket & WebRTC. Không giữ socket sống ở background.

### 7.4 Streaming

**Q: HLS vs WebRTC — chọn cái nào cho tính năng livestream có tương tác?**
> Nếu chỉ chat text kèm xem: LL-HLS (rẻ, CDN, scale triệu view). Nếu khán giả được "lên sóng" nói chuyện: WebRTC cho nhóm host + LL-HLS cho khán giả xem, và chấp nhận độ trễ lệch nhau — kiến trúc hybrid là câu trả lời thực tế.

**Q: Vì sao video hay bị delay 2–3s mới lên hình?**
> Manifest → segment init → buffer đủ ngưỡng `LoadControl` mới render. Tối ưu: giảm min buffer khởi động, dùng segment ngắn/CMAF chunk, preload manifest, warm-up player, chọn track khởi đầu thấp rồi ABR lên.

---

## 8. Điểm khác biệt của riêng anh — khai thác 3 thứ này

1. **Android sâu.** Đa số ứng viên nói được "WebRTC có STUN/TURN" nhưng không nói được `MODE_IN_COMMUNICATION`, `ConnectionService`, foreground service type của Android 14, Doze. **Đó là mảng anh nên chủ động kéo về.**

2. **Kotlin/Coroutines/Flow cho realtime.** Realtime rất khớp với Flow: `callbackFlow` bọc socket, `StateFlow` cho connection state, `SharedFlow` cho event, structured concurrency để socket tự chết theo scope (khỏi leak — bệnh cổ điển của code socket viết bằng callback + thread thủ công).

3. **AI/ML on-device — lợi thế đúng lúc.** Tammi quảng cáo **lọc ồn AI** và **AI Agent**. Anh đang học ML/DL. Nói được: *"Lọc ồn AI thực chất là một mạng nhỏ chạy real-time trên frame 10–20ms trong audio pipeline, ràng buộc khó nhất không phải accuracy mà là latency + CPU + pin; muốn chạy được on-device phải quantize (INT8), dùng NNAPI/LiteRT hoặc delegate, và đo bằng MOS/PESQ chứ không phải accuracy."*
   → Đây chính là **giao điểm Android + ML** mà rất ít ứng viên có. Nếu team đang làm hướng này thì anh đúng người.

---

## 9. Kế hoạch ôn 5–7 ngày (5–10h/tuần)

| Ngày | Bài trong lab | Việc | Ra được gì |
|---|---|---|---|
| 1 | Bài 0 | Dựng khung multi-module. Đọc kỹ RFC 6455 phần handshake + frame; đọc OkHttp `WebSocketListener` | Nói trôi handshake, ping/pong, close code |
| 2 | **Bài 1** | Code socket layer: OkHttp + `callbackFlow` + backoff/jitter + mock server Node `ws` | **Demo chạy được** — mạnh hơn mọi câu trả lời lý thuyết |
| 3 | Bài 1 (đo) | Bật/tắt máy bay, khoá màn hình 10 phút, đổi Wi-Fi↔4G → xem socket chết thế nào. Ghi vào `03_SO_LIEU_DO_DUOC.md` | Kể được **quan sát thật**, không phải lý thuyết |
| 4 | **Bài 2** | Chat local-first: Room SSOT + outbox + keyset pagination + test offline 5 tin | Vẽ được luồng một tin nhắn từ Gửi tới "đã xem" |
| 5 | **Bài 3** | WebRTC 1-1 + signaling server + màn hình Call Stats. **Đo P2P vs forced relay** | Số liệu RTT để nói về zero-rating |
| 6 | Bài 4 + 5 | Jitsi SDK (đo APK size trước/sau) + Media3 phát HLS, đo TTFF theo `LoadControl` | Trả lời "gọi nhóm 20 người" và ABR bằng số mình đo |
| 7 | — | Cài **Tammi thật**, dùng chat + gọi + video + meeting. Bật Developer options ghi nhận: thời gian setup call, hành vi khi mất mạng, chất lượng khi bật lọc ồn | **Nhận xét sản phẩm cụ thể = ấn tượng mạnh nhất** |

> Nếu chỉ có thời gian cho 2 việc: **Bài 1 + Bài 2** (socket có reconnect, chat local-first) và **ngày 7 (dùng app thật)** — hai thứ này đổi được nhiều điểm nhất.
>
> Nếu thiếu thời gian nữa: **Bài 4 (Jitsi) có thể chỉ đọc `LESSON.md` không code**, vì phần đó hỏi lý thuyết là chính.

---

## 10. Câu hỏi nên hỏi lại họ (chọn 3–4 câu)

Câu hỏi tốt thể hiện trình độ hơn cả câu trả lời:

1. "Signaling của Tammi đi theo XMPP hay protocol tự định nghĩa trên WebSocket? Nếu XMPP thì có dùng Stream Management để resume không?"
2. "Gọi nhóm/meeting dùng Jitsi Videobridge hay SFU tự phát triển? Có simulcast + last-N chưa, và giới hạn số người hiện tại là bao nhiêu?"
3. "Tính năng không tốn data có buộc media phải relay qua hạ tầng Viettel không? Nếu có thì team cân đối thế nào giữa chi phí băng thông TURN/SFU và chất lượng cuộc gọi?"
4. "Lọc ồn AI chạy on-device hay server-side? Model do team Viettel AI train hay SDK bên thứ ba?" *(câu này bộc lộ hướng ML của anh)*
5. "APK ~200MB — team đang xử lý size thế nào? Có dynamic feature module hay tách bớt Jitsi SDK không?"
6. "Với ~50 dịch vụ, phần nào là native, phần nào là mini-app/H5? Team em phỏng vấn nằm ở khối OTT core hay khối service?"
7. "Chat có E2EE chưa, và định danh xác thực ảnh hưởng thế nào tới thiết kế bảo mật?"

---

## 11. Nguồn

- [Tammi by Viettel — Google Play](https://play.google.com/store/apps/details?id=com.viettel.appviettel&hl=vi)
- [Viettel Tammi trên APKCombo (version, size, split APK)](https://apkcombo.com/vi/viettel-tammi/com.viettel.appviettel/)
- [Viettel Tammi — APKPure (cấu hình split: base + arm64_v8a + en + xxhdpi)](https://apkpure.com/viettel-tammi/com.viettel.appviettel/download)
- [Viettel Tammi — AppBrain](https://www.appbrain.com/app/tammi-by-viettel/com.viettel.appviettel)
- [**JD Android Developer — tuyendung.viettel.vn (WebRTC/WebSocket/XMPP, Media3/ExoPlayer, Jitsi SDK)**](https://tuyendung.viettel.vn/recruitment-information/detail/41746)
- [Tammi by Viettel là gì — Viettel Data (50 dịch vụ, 1 triệu user 07/2026, IoT)](https://www.vietteldata.vn/tu-van/tammi-viettel-la-gi)
- [Hướng dẫn trải nghiệm Viettel Tammi — Viettelnet](https://viettelnet.vn/huong-dan-trai-nghiem-viettel-tammi-ca-nhan-ho-gia-dinh/)
- [Viettel Tammi — App Store (iOS)](https://apps.apple.com/vn/app/viettel-tammi/id6749427319)
- [Giải pháp liên lạc khi hết data — Thanh Niên (gọi/nhắn không tốn data)](https://thanhnien.vn/giai-phap-lien-lac-khi-tai-khoan-dien-thoai-het-tien-het-dung-luong-data-185260807093523222.htm)
- [Android Developers at Viettel Media — ITJobs (bối cảnh Mocha, Keeng)](https://www.itjobs.com.vn/en/job/60024/10-android-developers)
- [WebRTC signaling: SIP vs XMPP vs REST — BlogGeek.me](https://bloggeek.me/webrtc-signaling/)
- [XMPP + WebRTC (Jingle) — xmpp.org](https://xmpp.org/uses/webrtc/)