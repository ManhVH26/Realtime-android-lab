# 03 — Những gì tôi đo được

> **Đây là phần giá trị nhất của cả project.**
> Người phỏng vấn đọc số liệu thật sẽ tin ngay là tôi tự làm, không copy tutorial. Nó tạo khác biệt lớn hơn cả chất lượng code.
>
> **Quy tắc bất di bất dịch:** chưa đo thì để `chưa đo`. **Tuyệt đối không điền số bịa** — bị hỏi lại chi tiết là lộ ngay, và mất hết uy tín cho cả buổi.
>
> Claude: sau mỗi bài, **nhắc tôi điền bảng này**, và khi tôi đã có số thì **copy sang mục "Những gì tôi đo được" trong `README.md` gốc**.

---

## Bài 1 — WebSocket & reconnect

| Thí nghiệm | Cấu hình | Kết quả |
|---|---|---|
| Reconnect sau máy bay 30s | base ?s, cap ?s, jitter ±?% | chưa đo — chuỗi delay từng lần retry, connect thành công ở lần thứ mấy |
| Thời gian phát hiện đứt kết nối | có `pingInterval` vs không | chưa đo — bao nhiêu giây mới biết là đứt |
| TCP idle bị NAT drop | mạng 4G, không ping, để yên | chưa đo — sau bao lâu thì kết nối chết |
| Đổi Wi-Fi → 4G | có `ConnectivityObserver` vs không | chưa đo — thời gian tới khi connected lại |

**Ghi chú / điều bất ngờ khi đo:**
> (điền sau khi đo — phần này còn giá trị hơn con số, vì nó chứng minh tôi thật sự chạy thí nghiệm)

---

## Bài 2 — Chat local-first

| Thí nghiệm | Cấu hình | Kết quả |
|---|---|---|
| Offline gửi 5 tin rồi bật mạng | outbox + clientMsgId | chưa đo — đúng thứ tự? trùng tin? mất tin? |
| Thời gian mở màn hình chat (10k tin trong Room) | keyset pagination, page 30 | chưa đo — ms tới khi hiện tin đầu |
| So sánh keyset vs OFFSET | 10k tin, vừa chèn tin mới | chưa đo — OFFSET lặp/nhảy bao nhiêu tin |
| Query có index vs không index | index trên `(conversationId, seq)` | chưa đo — ms |

**Ghi chú:**
> (điền sau)

---

## Bài 3 — WebRTC 1-1  ⭐ bảng quan trọng nhất

| Thí nghiệm | Cấu hình | Kết quả |
|---|---|---|
| **P2P vs forced relay** | cùng Wi-Fi, 2 thiết bị, `iceTransportPolicy` default vs `relay` | chưa đo — **RTT ?ms → ?ms**, bitrate, cặp ICE được chọn |
| Thời gian setup call | trickle ICE bật vs tắt | chưa đo — ms từ lúc bấm gọi tới lúc có audio |
| Cuộc gọi qua 4G | audio-only | chưa đo — RTT, jitter, packetsLost, bitrate |
| Thêm video vào cuộc gọi | 720p | chưa đo — bitrate tăng bao nhiêu, framesDropped, nhiệt/pin nếu quan sát được |
| Giới hạn băng thông | Network Profiler hạ xuống ?kbps | chưa đo — TWCC hạ bitrate/resolution thế nào, sau bao lâu |
| ICE restart khi đổi mạng | Wi-Fi → 4G giữa cuộc gọi | chưa đo — bao lâu thì có lại hình/tiếng |
| Codec | H.264 hardware vs VP8 | chưa đo — CPU, bitrate, chất lượng cảm nhận |

**Ghi chú — nhớ ghi cả cặp ICE thực tế được chọn (host / srflx / relay) trong từng lần đo:**
> (điền sau)

---

## Bài 4 — Group call & APK size  ⭐ bảng quan trọng thứ hai

| Thí nghiệm | Cấu hình | Kết quả |
|---|---|---|
| **APK size trước khi thêm Jitsi SDK** | release, `optimization enable = true` | chưa đo — ? MB |
| **APK size sau khi thêm Jitsi SDK** | release, cùng cấu hình | chưa đo — ? MB (**tăng ? MB**) |
| Breakdown theo APK Analyzer | native libs / dex / resources / assets | chưa đo — thành phần nào chiếm nhiều nhất |
| Nếu tách chỉ 1 ABI (arm64) | so với universal | chưa đo — ? MB |
| Tính toán băng thông họp nhóm | mesh vs SFU, 4 / 10 / 20 người | chưa đo — Mbps upload mỗi client (tính tay, ghi công thức) |

**Ghi chú:**
> (điền sau — đặc biệt: native lib nào của Jitsi/WebRTC nặng nhất, có phải React Native bên trong không)

---

## Bài 5 — Streaming (Media3)

| Thí nghiệm | Cấu hình | Kết quả |
|---|---|---|
| **TTFF theo `LoadControl`** | minBuffer 2s / 5s / 15s | chưa đo — ms cho từng cấu hình |
| Số lần rebuffer trong 5 phút | 3 cấu hình trên, cùng stream | chưa đo — lần/5 phút |
| ABR khi hạ băng thông | giới hạn xuống ?kbps | chưa đo — bao lâu thì đổi track, đổi từ ?p sang ?p |
| Feed reels: có player pool vs không | scroll 20 item | chưa đo — thời gian tới frame đầu mỗi item, memory |
| HLS vs DASH cùng nội dung | cùng mạng | chưa đo — TTFF, số lần rebuffer |

**Ghi chú:**
> (điền sau)

---

## Kết luận rút ra (điền cuối cùng, trước khi đi phỏng vấn)

Ba câu tôi sẽ nói khi được hỏi "anh đã làm gì để chuẩn bị", mỗi câu **phải kèm một con số ở trên**:

1. chưa viết
2. chưa viết
3. chưa viết