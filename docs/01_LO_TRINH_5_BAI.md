# 01 — Lộ trình: Bài 0 → Bài 5

> **Tạo MỘT bài mỗi lần được yêu cầu.** Xong thì dừng, nhắc commit, nhắc điền số liệu, đợi gọi bài tiếp.
> Quy tắc tách `Lesson` (code mẫu đầy đủ) vs `Exercise` (chỉ TODO, không gợi ý tên API): xem `CLAUDE.md` mục 3.

---

## Bài 0 — Dựng khung multi-module

**Hiện trạng:** single-module Compose app (chi tiết ở `CLAUDE.md` mục 6).

**Việc cần làm:**
1. `settings.gradle.kts`: include `:core-network`, `:core-database`, `:feature-chat`, `:feature-call`, `:feature-player`.
2. Tạo convention cho module Android library (tránh copy-paste `build.gradle.kts` 5 lần — dùng `buildSrc` hoặc `build-logic` composite, hoặc tối thiểu là một file convention plugin). **Giải thích cho tôi vì sao cách anh chọn tốt hơn cách kia** — đây là câu hỏi kiến trúc tôi có thể bị hỏi.
3. Thêm vào `libs.versions.toml`: OkHttp, kotlinx-serialization (hoặc Moshi), Room + KSP, Media3, Hilt, Paging 3, WorkManager, coroutines-test, Turbine.
   **Tự tra version tương thích Kotlin 2.2.10 / AGP 9.2.1 — KSP phải khớp Kotlin, đừng đoán số.** Nếu không chắc, ghi `# TODO version` và nói tôi tự điền còn hơn ghi số sai.
4. `app`: một màn hình Compose duy nhất — list 5 nút dẫn tới 5 demo (Bài 1..5), mỗi màn hình đích tạm để placeholder.
5. Tạo `README.md` gốc project với khung mục **"Những gì tôi đo được"** (copy bảng từ `docs/03_SO_LIEU_DO_DUOC.md`).
6. Tạo `server/README.md` mô tả mock server sẽ có gì (chưa cần code).

**Đưa tôi lệnh tự chạy:**
```powershell
.\gradlew.bat :app:assembleDebug --dry-run   # kiểm tra cấu hình, chưa build thật
.\gradlew.bat :app:assembleDebug
```

**Coi như xong khi:** sync sạch, chạy được app, bấm 5 nút ra 5 màn hình rỗng.

---

## Bài 1 — WebSocket: từ handshake tới sống sót thật

**Folder:** `core-network/` + `feature-chat/.../socket/`

### Cần dạy (LESSON)

**Nền tảng:**
- Handshake: HTTP GET + `Upgrade: websocket` / `Connection: Upgrade` / `Sec-WebSocket-Key` → **101 Switching Protocols** + `Sec-WebSocket-Accept = base64(SHA1(key + GUID))`
- Frame: opcode `0x1` text / `0x2` binary / `0x8` close / `0x9` ping / `0xA` pong; FIN + fragmentation; **client→server luôn mask**; close code
- `permessage-deflate`, `Sec-WebSocket-Protocol` (vd subprotocol `xmpp`)
- So sánh: long-polling / SSE / **WebSocket** / gRPC bidi / **MQTT** (Tammi có IoT nên MQTT liên quan) — theo trục: chiều dữ liệu, overhead, khi nào dùng

**Phần "chỉ dân làm thật mới biết" — dạy kỹ, đây là chỗ ăn điểm:**
- **Carrier NAT của mạng di động drop TCP idle sau ~2–5 phút** ⇒ vì sao buộc phải ping định kỳ
- **Half-open connection**: TCP tưởng còn sống nhưng đã đứt; chỉ ping/pong timeout mới lộ ra
- **Exponential backoff + jitter**; thiếu jitter ⇒ **thundering herd**: mất mạng cả vùng, triệu client reconnect cùng một giây, sập gateway
- Auth: token trong handshake (**không nhét query string** — lộ trong log); token hết hạn giữa phiên → server đóng với close code riêng → refresh → reconnect
- **Backpressure**: hàng đợi gửi phình lên nghĩa là mạng không đẩy kịp; với typing/presence phải **throttle + coalesce** (bỏ event cũ, giữ cái mới nhất), không queue hết

**Code mẫu trong LESSON phải chạy được:**
- Socket layer expose `Flow` (`callbackFlow` + `awaitClose` ⇒ socket tự chết theo scope, khỏi leak — bệnh cổ điển của code socket viết bằng callback + thread thủ công)
- `StateFlow<ConnectionState>` cho UI
- Backoff tập trung một chỗ ở tầng Flow

### Bài tập (chỉ TODO)
- Tự viết lại lớp socket theo Flow, có state connecting / connected / reconnecting / failed
- Tự implement backoff: có cap, có jitter, **reset khi connect thành công**
- Tự viết `ConnectivityObserver` để reconnect **chủ động** khi đổi mạng, thay vì đợi timeout
- Màn hình debug hiện: state hiện tại, số lần retry, delay lần tới, RTT của ping gần nhất
- Unit test cho hàm tính backoff (dùng coroutines-test + Turbine)

### Mock server cần viết (`server/`)
Node + `ws`: echo message, **có route cố tình đóng kết nối** và **route cố tình delay**, để tôi test reconnect. Viết `package.json` + hướng dẫn, **tôi tự chạy `npm install`**.

### Coi như xong khi
Bật chế độ máy bay 30s rồi tắt → app tự reconnect, không crash, log ra đúng chuỗi backoff; và tôi giải thích được vì sao cần jitter.

### Câu phỏng vấn gắn với bài
"User vào hầm rồi ra, anh xử lý thế nào?" · "Khi nào KHÔNG nên dùng WebSocket?" · "1 triệu user online scale thế nào?"

---

## Bài 2 — Chat local-first (bài quan trọng nhất, đầu tư nhiều nhất)

**Folder:** `feature-chat/` + `core-database/`

### Cần dạy (LESSON)

**Nguyên tắc gốc:** **Room là Single Source of Truth, không phải network.** UI chỉ observe `Flow<List<Message>>` từ Room; socket/REST chỉ ghi vào Room.
Lý do phải nói được: mở app **thấy tin nhắn ngay** (0 spinner), offline vẫn gửi được, và **không có 2 nguồn sự thật để lệch nhau**.

**Outbox pattern:**
```
insert Room (state = PENDING) → UI hiện ngay (optimistic)
      ↓ socket send
  ACK (serverId, seq, serverTime) → state = SENT
      ↓ timeout / lỗi
  state = FAILED → nút "gửi lại" (dùng lại clientMsgId cũ ⇒ idempotent)
```

**Các bài toán khó phải dạy đủ:**
| Bài toán | Nội dung |
|---|---|
| Trạng thái | `PENDING → SENT → DELIVERED → SEEN` |
| Không trùng tin | `clientMsgId` (UUID) do client sinh, server dedupe theo key đó |
| Thứ tự | **Sort theo `serverSeq`, KHÔNG theo timestamp client** — đồng hồ máy user sai là chuyện thường |
| Phân trang | **Keyset**: `WHERE seq < :cursor ORDER BY seq DESC LIMIT 30`. Giải thích vì sao OFFSET sai khi có tin mới chèn vào (lặp/nhảy tin) |
| Đồng bộ sau offline | Reconnect → gửi `lastSeq` per-conversation → server trả delta |
| Typing indicator | Throttle 2–3s + coalesce, **không gửi mỗi keystroke** |
| Read receipt group lớn | Aggregate count, **không fanout N receipt/người**; danh sách người đã xem load lazy |
| Ảnh/file | **Không đẩy qua WebSocket**. Presigned URL lên object storage → message chứa URL. Retry bằng **WorkManager** (sống qua process death) |
| Xoá/sửa/thu hồi | Tombstone + version, không DELETE thật — client khác cần biết "đã thu hồi" |
| Fanout group | Ghi 1 lần vào conversation, N client đọc theo seq (fan-out on read), không nhân bản N record |

**Phần XMPP — chỉ cần `LESSON.md` + bảng từ vựng, KHÔNG dựng server thật** (quá tốn thời gian so với giá trị phỏng vấn):
- **JID** `user@domain/resource` — resource = từng thiết bị ⇒ multi-device
- 3 stanza: `<message>` fire&forget · `<presence>` online/typing · `<iq>` request/response
- **MUC (XEP-0045)** chat nhóm · **Jingle (XEP-0166)** signaling call
- **Stream Management (XEP-0198)** — ack + **resume session** sau mất mạng; cực quan trọng cho mobile vì khỏi làm lại handshake + auth
- **MAM (XEP-0313)** archive · **Carbons (XEP-0280)** sync đa thiết bị
- **RFC 7395** XMPP over WebSocket (thay BOSH XEP-0124 kiểu long-polling cũ)
- Library Android: **Smack**
- E2EE: **OMEMO (XEP-0384)** dựa Signal protocol — nhưng telco có "định danh xác thực" thường **không** làm E2EE vì lý do quản lý ⇒ đây là câu hay để hỏi lại nhà tuyển dụng

### Bài tập (chỉ TODO)
- Thiết kế schema Room cho `conversation` + `message` — **tôi tự quyết index nào cần** và giải thích vì sao
- Outbox: optimistic send → ack update → timeout FAILED + retry idempotent
- Paging 3 + `RemoteMediator` với keyset cursor
- Delta sync khi reconnect
- **Test offline:** tắt mạng, gửi 5 tin, bật mạng → cả 5 lên đúng thứ tự, không trùng, không mất

### Coi như xong khi
Tôi vẽ được sơ đồ luồng một tin nhắn từ lúc bấm Gửi tới lúc hiện "đã xem", và chỉ ra được **từng chỗ có thể mất hoặc trùng tin**.

---

## Bài 3 — WebRTC 1-1 (voice + video)

**Folder:** `feature-call/`

### Cần dạy (LESSON)

**Cốt lõi phải nhấn: WebRTC KHÔNG định nghĩa signaling.** Đó là lý do team phải tự làm bằng WebSocket/XMPP — và là lý do JD gộp "WebRTC/WebSocket/XMPP" thành một dòng.

Vẽ được sơ đồ:
```
A                    Signaling server (WebSocket)                   B
│── offer (SDP) ─────────────►│──────────────────────────────────►│
│◄────────────────────────────│◄──────── answer (SDP) ────────────│
│◄──── ICE candidates (trickle, 2 chiều, liên tục) ──────────────►│
│═══════ media: SRTP/DTLS — đi TRỰC TIẾP, không qua signaling ═══►│
```

**NAT traversal (hỏi 10 lần thì 8 lần có):**
- **STUN**: "IP public của tôi là gì" → candidate srflx. Nhẹ, gần như free.
- **TURN**: relay toàn bộ media khi **symmetric NAT / firewall doanh nghiệp** chặn P2P. **Tốn băng thông thật.** Thực tế ~10–20% cuộc gọi cần. Fallback cuối: **TURN over TCP/TLS port 443** (nhìn như HTTPS nên qua được firewall khó).
- **ICE**: gather candidate (host / srflx / relay) → ghép cặp → STUN connectivity check → chọn cặp tốt nhất. **Trickle ICE** = gửi dần thay vì gom hết ⇒ giảm rõ thời gian setup call.
- **Mã hoá bắt buộc**: DTLS handshake → khoá cho **SRTP**. WebRTC không cho media trần.

**Codec & thích ứng mạng:**
- Audio **Opus** (48kHz, in-band FEC, DTX tắt gửi khi im lặng)
- Video VP8/VP9/H264/AV1 — mobile ưu tiên **H.264 hardware encoder** (đỡ nóng, đỡ tốn pin), nhưng **simulcast H264 trên Android bị hạn chế** ⇒ đánh đổi thật
- **GCC + TWCC** ước lượng băng thông → tự hạ bitrate/resolution/framerate
- `degradationPreference`: share màn hình → giữ resolution; nói chuyện → giữ framerate
- Chống mất gói: NACK / **FEC** / PLI (xin keyframe) / **jitter buffer** (đánh đổi latency ↔ mượt)
- **Audio pipeline: AEC → NS → AGC → VAD.** **Chỉ rõ khối NS nằm ở đâu** — vì "lọc ồn AI" của Tammi chính là thay khối này bằng DNN (xem `docs/00` mục 4.2)

**Android-specific — đây là lợi thế sân nhà của tôi, dạy kỹ nhất:**
| Vấn đề | API |
|---|---|
| Cuộc gọi đến khi app đã bị kill | **FCM high-priority data message** → wake process → connect socket → hiện UI. **Vì sao KHÔNG giữ socket sống ở background: Doze + background execution limits** |
| UI cuộc gọi đến | `Notification.CallStyle` (API 31+) + full-screen intent (Android 14 giới hạn `USE_FULL_SCREEN_INTENT` cho app calling) |
| Tích hợp hệ thống | `ConnectionService` / Telecom framework |
| Giữ call sống | Foreground service type **`microphone` + `camera`** (bắt buộc khai báo từ Android 14) |
| Routing âm thanh | `AudioManager.MODE_IN_COMMUNICATION`, audio focus, `setCommunicationDevice()` (API 31+), Bluetooth SCO |
| Áp tai tắt màn hình | Proximity sensor + `PROXIMITY_SCREEN_OFF_WAKE_LOCK` |
| Camera & render | `Camera2Enumerator`, `SurfaceTextureHelper`, `SurfaceViewRenderer`, `EglBase` |
| Đổi Wi-Fi ↔ 4G giữa cuộc gọi | **ICE restart** |

⚠️ minSdk project là 24 → **comment rõ API nào cần version check và có từ API level nào** (Tammi thật minSdk 29, tôi sẽ bị hỏi chỗ này).

### Bài tập (chỉ TODO)
- Signaling server Node trong `server/`: room-based, chuyển tiếp offer / answer / candidate
- Gọi 1-1 giữa 2 thiết bị (hoặc 1 máy thật + 1 emulator): **audio-only trước**, xong rồi thêm video
- Màn hình **Call Stats** đọc `getStats()`: RTT, jitter, packetsLost, freezeCount, framesDropped, bitrate thực, và **cặp ICE đang dùng là host / srflx / relay**
- **Thí nghiệm zero-rating (quan trọng nhất bài này):** cấu hình ép đi relay, đo RTT/bitrate so với P2P → **ghi số vào `docs/03`**
- Xử lý đủ vòng đời: foreground service, audio routing, proximity, ICE restart khi đổi mạng

### Coi như xong khi
Vẽ được sơ đồ signaling trên bảng, giải thích vì sao ~10–20% cuộc gọi phải qua TURN, và **có con số RTT P2P vs relay do tôi tự đo**.

---

## Bài 4 — Group call: mesh vs MCU vs SFU (Jitsi)

**Folder:** `feature-call/group/`

### Cần dạy (LESSON)

| | Cách hoạt động | Upload của client | Điểm chết |
|---|---|---|---|
| **Mesh (P2P full)** | mỗi người gửi cho mọi người | **(N-1) × bitrate** | Sập ở ~4 người; mobile nóng máy, tụt pin |
| **MCU** | server **decode + trộn** thành 1 stream | 1 × | CPU server cực đắt, thêm latency, mất layout linh hoạt |
| **SFU** ✅ | server **chỉ route có chọn lọc**, không decode | 1 × (có simulcast: 2–3 layer) | Download vẫn tăng theo N ⇒ cần last-N |

**Jitsi cụ thể — phải biết tên các tính năng:**
- **Videobridge = SFU**
- **Simulcast**: client encode 3 độ phân giải (180p/360p/720p) cùng lúc; SFU chọn layer phù hợp cho **từng người nhận** theo băng thông của họ
- **Dominant speaker detection**: ai đang nói thì lên khung lớn
- **Last-N**: chỉ forward video của N người nói gần nhất ⇒ họp 50 người vẫn chạy trên mobile
- **E2EE qua Insertable Streams**: mã hoá thêm một lớp mà SFU không đọc được
- ⚠️ **Điểm đau thật: Jitsi Meet SDK Android nhúng React Native bên trong ⇒ APK phình.** Nên có lựa chọn `lib-jitsi-meet` / tự tích hợp JVB.

### Bài tập (chỉ TODO)
- Tích hợp Jitsi Meet SDK vào `feature-call`, join room public, custom được toolbar / feature flags
- **Đo APK size trước/sau khi thêm Jitsi SDK** bằng APK Analyzer của Android Studio. **Nhớ bật `optimization { enable = true }` cho release** để số có ý nghĩa. Ghi vào `docs/03`.
- Viết `LESSON.md` so sánh 3 kiến trúc **kèm tính toán băng thông cụ thể** cho cuộc họp 4 / 10 / 20 người

### Coi như xong khi
Trả lời câu "gọi nhóm 20 người thì làm sao?" trong 2 phút, **có số**.

---

## Bài 5 — Streaming với Media3

**Folder:** `feature-player/`

### Cần dạy (LESSON)

**Chọn giao thức theo latency:**
| Giao thức | Latency | Dùng khi |
|---|---|---|
| RTMP / **SRT** | ingest | Người phát → server |
| **HLS** (TS/CMAF) | 15–30s | VOD, live thường, tương thích rộng nhất |
| **DASH** | 10–30s | Android/web, DRM linh hoạt |
| **LL-HLS / LL-DASH (CMAF chunked)** | 2–5s | Live thể thao, bán tương tác |
| **WebRTC** | < 500ms | Call, meeting, live 2 chiều |

**Nguyên tắc trả lời:** latency thấp ⇒ đắt và khó scale (cần SFU/TURN, không CDN-friendly). HLS/DASH đi CDN ⇒ rẻ, scale vô hạn, nhưng chậm. **Tammi có cả hai nhu cầu** ⇒ chọn theo tính năng, không chọn "cái nào xịn hơn".

**Media3 cụ thể:**
- **`androidx.media3` đã thay `com.google.android.exoplayer2`**; ExoPlayer nay là một implementation của interface `Player`. Nếu bị hỏi "ExoPlayer hay Media3" thì đây là câu trả lời đúng thời sự.
- Luồng: `MediaSource` (HLS/DASH/Progressive) → `TrackSelector` → `Renderer` → `MediaCodec` → `Surface`
- **ABR**: `AdaptiveTrackSelection` quyết định theo `BandwidthMeter` + **mức buffer hiện tại**
- Tuning `LoadControl` (min/max buffer) = đánh đổi **startup nhanh ↔ ít rebuffer**
- **Live**: `MediaItem.LiveConfiguration` + `targetOffsetMs`; player **điều chỉnh tốc độ phát ±5%** để kéo về offset thay vì seek giật
- **DRM**: Widevine **L1 (hardware, secure decoder) / L3 (software)**, `MediaDrm`, offline license
- Offline: `DownloadManager` + `CacheDataSource`

### Bài tập (chỉ TODO)
- Phát stream HLS test công khai, overlay hiện: bitrate hiện tại, độ phân giải, mức buffer, số lần rebuffer
- Đo **TTFF (time to first frame)** qua `AnalyticsListener`; thử **3 cấu hình `LoadControl`** khác nhau → **bảng số liệu vào `docs/03`**
- Feed video kiểu reels với **player pool + preload item kế tiếp** (KHÔNG tạo/hủy player mỗi lần scroll) — câu hỏi thực tế rất hay gặp
- Giới hạn băng thông (Network Profiler / emulator) để xem ABR hạ chất lượng theo thời gian thực

### Coi như xong khi
Giải thích được vì sao video mất 2–3s mới lên hình, nêu 3 cách giảm, **kèm số tôi tự đo**.

---

## Sau khi xong Bài 5

Nhắc tôi chuyển sang `docs/02_CHECKLIST_QUIZ.md` và **quiz tôi từng câu**.