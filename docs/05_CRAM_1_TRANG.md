# 05 — Cram 1 trang (ôn gấp 1-3 ngày)

> Mục tiêu file này: **nói trôi chảy cả 5 chủ đề**, KHÔNG build. Đọc file này ~1-2h → nói "quiz tôi đi" (dùng `docs/02`) → chỗ nào hổng thì mở `docs/04` đọc sâu đúng mục đó.
> Thứ tự ưu tiên nếu thiếu thời gian: **WebRTC ≈ Chat > WebSocket > Streaming > Group**. Ba điểm Tammi-specific ở cuối là chỗ tạo khác biệt — học kỹ.

---

## 1. WebSocket

**Chốt:** Mạng di động (carrier NAT) **drop TCP idle sau ~2-5 phút** → buộc **ping/pong** ở tầng app; reconnect phải **backoff + jitter**.

- **Handshake:** HTTP GET + `Upgrade: websocket` → **101 Switching Protocols**. `Sec-WebSocket-Accept = base64(SHA1(key+GUID))` — server chứng minh nó hiểu WS, không phải HTTP thường.
- **Ping/pong dù TCP có keepalive:** TCP keepalive mặc định hàng giờ + carrier NAT đã cắt trước đó. Ping app-level phát hiện **half-open** (một đầu tưởng còn sống nhưng đã đứt) qua pong-timeout.
- **Backoff + jitter:** thiếu jitter → **thundering herd**: mất sóng cả vùng, triệu client reconnect cùng 1 giây → sập gateway. Jitter rải ngẫu nhiên thời điểm nối lại. Backoff phải **reset khi connect thành công**.
- **KHÔNG dùng WS khi:** request/response đơn giản (REST đủ); chỉ server→client 1 chiều (SSE nhẹ hơn); push khi app background (dùng FCM, không giữ socket vì Doze).
- **vs SSE vs MQTT:** SSE = 1 chiều server→client, nhẹ. MQTT = pub/sub, tiết kiệm pin/băng thông → **hợp IoT** (Tammi có loa/camera/nhẫn). WS = 2 chiều đa dụng.
- **Scale 1 triệu online:** bottleneck là **số kết nối đồng thời** (file descriptor/RAM mỗi socket), không phải CPU. Giải: nhiều node + load balancer, sticky/session, pub/sub (Redis) fanout giữa node.
- **Token hết hạn giữa phiên:** server đóng bằng **close code riêng** → client refresh token → reconnect. Token để trong **handshake header**, KHÔNG nhét query string (lộ trong log).

## 2. Chat (local-first)

**Chốt:** **Room là Single Source of Truth**, không phải network. UI chỉ observe `Flow<List<Message>>` từ Room; socket/REST chỉ ghi vào Room.

- **Vì sao Room là SSOT:** mở app thấy tin ngay (0 spinner), offline vẫn gửi được, và **không có 2 nguồn sự thật để lệch nhau**.
- **Không mất / không trùng:** **Outbox pattern** — insert Room `PENDING` → UI hiện ngay (optimistic) → socket send → ACK (serverId, seq) → `SENT`; timeout → `FAILED` + nút gửi lại. Dedupe bằng **`clientMsgId` (UUID client sinh)**, server bỏ trùng theo key đó → gửi lại vẫn idempotent.
- **Không sort theo timestamp client:** đồng hồ máy user sai là chuyện thường → sort theo **`serverSeq`** (số thứ tự server cấp).
- **Không OFFSET, dùng keyset:** `WHERE seq < :cursor ORDER BY seq DESC LIMIT 30`. OFFSET sai khi có tin mới chèn vào → lặp/nhảy tin.
- **Ảnh/video:** KHÔNG đẩy qua WebSocket. Upload lên object storage qua **presigned URL** → message chỉ chứa URL. Retry bằng **WorkManager** (sống qua process death).
- **Read receipt nhóm lớn:** aggregate **đếm số**, không fanout N receipt/người; danh sách người đã xem load lazy.
- **XMPP (chỉ cần từ vựng):** **JID** `user@domain/resource` (resource = từng thiết bị → multi-device). 3 stanza: `<message>` / `<presence>` / `<iq>`. **MUC** = chat nhóm. **Stream Management (XEP-0198)** = ack + **resume session** sau mất mạng → khỏi làm lại handshake+auth, cực quan trọng cho mobile.

## 3. Video call (WebRTC)

**Chốt:** WebRTC **KHÔNG định nghĩa signaling** → team tự làm qua WebSocket/XMPP. Đó là lý do JD gộp "WebRTC/WebSocket/XMPP" một dòng.

- **Luồng:** A gửi **offer (SDP)** → B trả **answer (SDP)** qua signaling; hai bên trao đổi **ICE candidates** (trickle, liên tục); media **SRTP/DTLS đi TRỰC TIẾP**, không qua signaling.
- **STUN vs TURN:** STUN = "IP public của tôi là gì" (nhẹ, gần free). **TURN = relay toàn bộ media** khi symmetric NAT/firewall chặn P2P — **tốn băng thông thật**, thực tế **~10-20% cuộc gọi** cần. Fallback cuối: **TURN over TCP/TLS 443** (nhìn như HTTPS, qua được firewall khó).
- **ICE:** gather candidate (host/srflx/relay) → ghép cặp → connectivity check → chọn cặp tốt nhất. **Trickle ICE** = gửi candidate dần thay vì gom hết → **giảm thời gian setup call**.
- **Mã hoá:** bắt buộc. DTLS handshake → khoá cho **SRTP**. Không có media trần.
- **Debug vỡ tiếng/giật hình — đọc `getStats()`:** RTT, jitter, packetsLost, freezeCount, framesDropped, bitrate thực. **Nghẽn mạng** → packetsLost/RTT cao; **thiếu CPU** → framesDropped/encode time cao dù mạng ổn.
- **Nhận call khi app đã kill:** **FCM high-priority data message** → wake process → connect socket → hiện UI. KHÔNG giữ socket sống background vì **Doze + background execution limits**.
- **Android 14 khi gọi:** foreground service type **`microphone` + `camera`** (bắt buộc khai báo); `USE_FULL_SCREEN_INTENT` bị giới hạn cho app calling.
- **Đổi Wi-Fi ↔ 4G giữa call:** **ICE restart**.

## 4. Group call (mesh / MCU / SFU)

**Chốt:** **SFU** là lựa chọn thực tế: server chỉ **route có chọn lọc**, không decode/mix.

| | Cách chạy | Upload client | Chết ở |
|---|---|---|---|
| **Mesh** | mỗi người gửi mọi người | (N-1)× bitrate | ~4 người, mobile nóng/tụt pin |
| **MCU** | server decode + trộn 1 stream | 1× | CPU server cực đắt, thêm latency |
| **SFU** ✅ | server route, không decode | 1× (simulcast 2-3 layer) | download vẫn tăng theo N → cần last-N |

- **Jitsi = Videobridge (SFU).** **Simulcast:** client encode 3 độ phân giải (180/360/720p) cùng lúc, SFU chọn layer hợp cho **từng người nhận**. **Last-N:** chỉ forward video N người nói gần nhất → họp 50 người vẫn chạy mobile. **Dominant speaker:** ai nói lên khung lớn.
- **Điểm đau:** Jitsi Meet SDK Android **nhúng React Native** → APK phình. Giải: `lib-jitsi-meet` / tự tích hợp JVB.

## 5. Streaming (Media3)

**Chốt:** chọn giao thức **theo latency**; latency thấp thì đắt & khó scale.

| Giao thức | Latency | Dùng khi |
|---|---|---|
| **HLS / DASH** | 15-30s | VOD, live thường — **đi CDN, rẻ, scale vô hạn** |
| **LL-HLS / LL-DASH** | 2-5s | live thể thao, bán tương tác |
| **WebRTC** | <500ms | call, meeting, live 2 chiều — đắt, cần SFU/TURN |

- **Media3 vs ExoPlayer:** `androidx.media3` **đã thay** `exoplayer2`; ExoPlayer nay là 1 implementation của interface `Player`. (Câu trả lời đúng thời sự nếu bị hỏi "ExoPlayer hay Media3".)
- **Vì sao 2-3s mới lên hình (TTFF):** DNS/connect + tải manifest + tải segment đầu + buffer tối thiểu + decode. Giảm: hạ min-buffer khởi động, segment ngắn/CMAF, preconnect/CDN gần, chọn bitrate khởi đầu thấp.
- **ABR** đổi chất lượng theo **`BandwidthMeter` + mức buffer hiện tại**.
- **`LoadControl`** = đánh đổi **startup nhanh ↔ ít rebuffer** (min/max buffer).
- **Live giữ đúng offset:** player **chỉnh tốc độ phát ±5%** kéo về `targetOffsetMs`, không seek giật.
- **Reels:** dùng **player pool + preload item kế**, KHÔNG tạo/hủy player mỗi lần scroll (tốn, giật).

---

## ⭐ Ba điểm Tammi-specific (học kỹ nhất — chỗ ăn điểm)

1. **"Không tốn data" xung đột P2P:** "gọi không tốn data" = **zero-rating** trên hạ tầng Viettel. Nhưng P2P đẩy media tới IP user khác → **không zero-rate được**. Muốn miễn data phải **ép relay toàn bộ qua TURN/SFU trong dải IP zero-rate** (`iceTransportPolicy = relay`). **Đánh đổi: trả tiền băng thông server để lấy USP, chịu RTT cao hơn P2P.**
2. **Lọc ồn AI:** "300+ loại tiếng ồn" → không rule-based được, phải **DNN nhỏ real-time** đặt **thay khối NS** trong audio pipeline WebRTC (**AEC → NS → AGC → VAD**). Ràng buộc khó nhất **không phải accuracy** mà là **latency/CPU/pin** → quantize INT8, NNAPI/LiteRT. Đo bằng **MOS/PESQ**, không phải accuracy.
3. **APK ~200MB:** đến từ **Jitsi SDK nhúng React Native** + native WebRTC nhiều ABI + assets sticker + nhiều mini-app. Giảm: dynamic feature module, tách Jitsi, split theo ABI.

Phụ (nếu hỏi kiến trúc app): **minSdk 29** = chấp nhận cắt máy cũ để dùng API mới, khỏi gánh compat. **~50 dịch vụ** → core OTT (chat/call) **native**, dịch vụ phụ **mini-app/H5 + JSBridge** để ra nhanh.

---

## Câu tự hỏi lại nhà tuyển dụng (chọn 3-4 — thể hiện trình độ hơn cả trả lời)

Xem `docs/02` mục cuối. Mạnh nhất: signaling XMPP hay tự định nghĩa (có Stream Management resume không); meeting dùng Jitsi hay SFU tự làm (simulcast/last-N chưa); "không tốn data" có ép relay qua hạ tầng Viettel không; lọc ồn on-device hay server.
