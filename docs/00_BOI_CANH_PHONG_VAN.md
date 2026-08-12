# 00 — Bối cảnh phỏng vấn: app Viettel Tammi

> Dữ liệu đã research và xác minh ngày 12/08/2026. **Đừng research lại**, dùng luôn.
> Mục đích: Claude hiểu vì sao project này chọn đúng 5 chủ đề đó, để tạo bài đúng trọng tâm.

---

## 1. App đích

| | |
|---|---|
| **Tên** | **Viettel Tammi** (không phải "My Viettel") |
| **Package** | `com.viettel.appviettel` |
| **Chủ sở hữu** | **Viettel Telecom** |
| **Ra mắt** | 15/04/2026 |
| **Category trên Play** | **Communication** — nói lên trọng tâm sản phẩm là liên lạc, không phải ví/thương mại |
| **Version research** | 1.1.24 (23/04/2026) |
| **Size** | **~197–210 MB** (App Bundle: base + `config.arm64_v8a` + `config.en` + `config.xxhdpi`) |
| **minSdk** | **Android 10 (API 29)** — cao, tức là team chọn cắt máy cũ để dùng API mới thay vì gánh compat |
| **Cài đặt** | 100.000+ trên Play; báo chí nói **1 triệu user tính đến 07/2026** |
| **Nền tảng khác** | iOS, **web `tammi.vn/chat`**, **desktop Windows + macOS** (khả năng cao là Electron bọc web client) |

**Định vị:** siêu app "1 app thay 30–50 app", ~50 dịch vụ / 10 lĩnh vực (viễn thông, y tế 43 bệnh viện, giáo dục, mua sắm, thanh toán hoá đơn, gọi xe, giao hàng, giải trí, IoT: loa thông minh / robot trẻ em / nhẫn sức khoẻ / camera).

**Trái tim kỹ thuật — khối OTT** (đây là phần bị hỏi):
- Chat 1-1, chat nhóm
- Voice call, video call
- **Meeting online** nhiều người
- **Gọi / nhắn / họp không tốn data** cho user Viettel
- **Lọc ồn bằng AI** — loại 300+ loại tiếng ồn (giao thông, gió, công trường, quán cafe, đám đông)
- **AI Agent** trợ lý cá nhân
- Định vị "nền tảng liên lạc **xác thực định danh** đầu tiên VN" — account gắn số thuê bao đã xác thực

**Tiền thân: Mocha** (`com.viettel.mocha`) — OTT chat/call/nhạc/stream của Viettel Media, 20M+ user. Nhân sự và kinh nghiệm realtime của Viettel đi từ đó sang. Nếu phỏng vấn nhắc Mocha thì đó là lý do.

---

## 2. JD Android Developer của Viettel (đã xác minh — nguồn: tuyendung.viettel.vn mã 41746)

| Hạng mục | Nội dung |
|---|---|
| Ngôn ngữ | **Kotlin / Java**, tối thiểu 2 năm |
| Kiến trúc | **MVC, MVVM, Clean Architecture** + design patterns phổ biến |
| Network | **RESTful API**, third-party SDK, **Firebase services** |
| Realtime (ưu tiên) | **WebRTC / WebSocket / XMPP** hoặc ứng dụng realtime |
| Streaming (ưu tiên) | **Live/Video Streaming — đặc biệt Media3, ExoPlayer** |
| SDK nêu tên | **tích hợp Jitsi SDK** cho chat, video, OTT, **gọi nhóm**, nhạc |
| Senior | ra quyết định kiến trúc, tối ưu performance (**multi-threading, memory, Jetpack components**), mượt trên đa thiết bị |

⇒ **Native Android (Kotlin/Java), không Flutter/React Native.**
⇒ 5 bài của project này map 1-1 với JD. Đừng thêm chủ đề ngoài JD.

---

## 3. Suy luận có căn cứ về stack (nói dạng "em hiểu là", KHÔNG khẳng định khi phỏng vấn)

Không decompile được APK, nên tách rõ:

| Mảng | Suy luận | Căn cứ |
|---|---|---|
| Chat signaling | **XMPP** (message/presence/IQ, MUC cho group) trên **WebSocket** (RFC 7395) | JD nêu XMPP; Mocha vốn XMPP; server phổ biến Openfire/ejabberd/Tigase |
| Call 1-1 | **WebRTC**, signaling qua WebSocket hoặc **XMPP Jingle (XEP-0166)** | JD nêu cả WebRTC và XMPP |
| Meeting nhóm | **Jitsi Videobridge (SFU)** — route chứ không mix | JD nêu Jitsi SDK + "gọi nhóm" |
| Live/video content | **Media3/ExoPlayer + HLS/DASH**, ABR | JD nêu Media3/ExoPlayer |
| Push | **FCM high-priority** để wake app khi có call/tin nhắn ở background | JD nêu Firebase; là cách duy nhất khả thi trên Android hiện đại |
| Kiến trúc app | multi-module Gradle + **mini-app / H5 container + JSBridge** cho ~50 dịch vụ | 210MB + 50 dịch vụ + tốc độ ra mắt |

---

## 4. BA ĐIỂM ĐẶC THÙ — phải lồng vào bài tập, đây là chỗ tạo khác biệt

### 4.1 "Không tốn data" xung đột với P2P của WebRTC

"Gọi không tốn data" = **zero-rating trên hạ tầng Viettel** (không tính data cho traffic tới server của Tammi), không phải trick giao thức.

Hệ quả kỹ thuật: **WebRTC vốn muốn đi P2P** (rẻ, latency thấp) — nhưng P2P thì media đi tới IP của user khác ⇒ **không thể zero-rate**. Muốn miễn data thì **buộc phải relay toàn bộ qua TURN/SFU nằm trong dải IP được zero-rate** (`iceTransportPolicy = relay`).

⇒ Đánh đổi: **trả tiền băng thông server để lấy USP "không tốn data"**, và chấp nhận RTT cao hơn P2P.

→ **Bài 3 phải có thí nghiệm đo RTT: P2P vs forced-relay.** Đây là số liệu mạnh nhất tôi mang đi phỏng vấn.

### 4.2 Lọc ồn AI = ML on-device

"300+ loại tiếng ồn" ⇒ không thể rule-based, phải là **DNN nhỏ chạy real-time trên frame 10–20ms**, đặt **thay khối NS cổ điển trong audio pipeline của WebRTC**.

Ràng buộc khó nhất **không phải accuracy** mà là: latency, CPU, pin. Muốn chạy on-device phải quantize (INT8), dùng NNAPI/LiteRT hoặc delegate. Đo bằng **MOS/PESQ**, không phải accuracy.

→ Đây là giao điểm **Android + ML on-device**, đúng hướng tôi đang học. Khi dạy Bài 3, **chỉ ra rõ khối NS nằm ở đâu trong pipeline** để tôi nói được chuyện này. **Không cần train model** — ngoài phạm vi.

### 4.3 APK ~200MB cho một app Communication

Nguồn gốc điển hình: **Jitsi Meet SDK Android nhúng React Native bên trong** + native libs WebRTC (`libjingle_peerconnection.so`) nhiều ABI + assets sticker/emoji + nhiều mini-app.

→ **Bài 4 phải đo APK size trước/sau khi thêm Jitsi SDK** bằng APK Analyzer. Đây là con số chạm đúng nỗi đau thật của team.

---

## 5. Ngoài phạm vi — đừng làm, không đổi được điểm phỏng vấn

- Dựng server XMPP thật (Openfire/ejabberd) — **chỉ cần nắm từ vựng và khái niệm**
- Tự implement E2EE / Signal protocol
- Tự viết SFU
- Backend production (auth thật, DB, scale) — **mock server Node đủ dùng**
- **UI đẹp** — chức năng và số liệu quan trọng hơn; UI chỉ cần đủ để demo và debug
- Train model lọc ồn — chỉ cần hiểu nó nằm đâu trong pipeline và bị ràng buộc gì