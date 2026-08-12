# Mock server — Bài 1 (WebSocket)

Server WebSocket giả để test reconnect / backoff / RTT. Bạn **tự chạy** các lệnh dưới.

## Deploy online (Render — khuyến nghị, test được cả trên 4G)

1. Push code lên GitHub (đã có repo). File `render.yaml` ở gốc repo và `engines` trong `package.json` đã cấu hình sẵn.
2. Vào https://render.com → đăng nhập bằng GitHub.
3. **New → Blueprint** → chọn repo `Realtime-android-lab` → **Apply**. Render đọc `render.yaml`, tự deploy thư mục `server/`.
4. Xong sẽ có URL kiểu `https://realtime-ws-lab.onrender.com`. Trong app dùng **`wss://`** + route:
   ```
   wss://realtime-ws-lab.onrender.com/echo
   wss://realtime-ws-lab.onrender.com/drop
   wss://realtime-ws-lab.onrender.com/policy
   ```
   `wss://` = có TLS, chạy từ mọi mạng (Wi-Fi/4G), không cần mở firewall hay `usesCleartextTraffic`.

> Free tier **ngủ sau ~15 phút** không dùng: lần kết nối đầu sau khi ngủ mất ~30–60s để dậy — app sẽ hiện `đang nối lại…` rồi mới `đã kết nối`. Bình thường, không phải lỗi.

Kiểm tra nhanh service sống: mở `https://realtime-ws-lab.onrender.com` trên trình duyệt → thấy dòng `ws lab ok`.

## Chạy local

```powershell
cd server
npm install      # cài 'ws', chỉ cần lần đầu
npm start        # chạy ở cổng 8080
```

Đổi cổng nếu cần: `$env:PORT=9000; npm start`

## Địa chỉ để app kết nối

| Chạy app ở đâu | URL điền trong app |
|---|---|
| **Emulator** | `ws://10.0.2.2:8080/echo` (10.0.2.2 = máy tính host nhìn từ emulator) |
| **Máy thật** (cùng Wi-Fi) | `ws://<IP-LAN-của-PC>:8080/echo` — tìm IP bằng `ipconfig` (dòng IPv4) |

> App đã bật `usesCleartextTraffic` để cho phép `ws://` (không mã hoá) khi test cục bộ.
> Production phải dùng `wss://`.

## Các route để thử từng tình huống

| URL | Server làm gì | Dùng để thấy |
|---|---|---|
| `/echo` | echo lại mọi tin | RTT (nút "Ping"), gửi/nhận |
| `/slow` | trả chậm 2s | RTT xấu |
| `/drop` | tự đóng sau 3–8s | **reconnect + chuỗi backoff tăng dần** |
| `/policy` | đóng với code 4001 | client **dừng hẳn**, không nối lại (mô phỏng token bị thu hồi) |

## Kịch bản demo phỏng vấn

1. `npm start`, mở app, URL `/echo`, bấm **Kết nối** → thấy `đã kết nối`, bấm **Ping** đọc RTT.
2. **Test half-open / mất mạng:** bật **chế độ máy bay** trên máy ~30s → xem app chuyển `đang nối lại lần 1, 2, 3…` với delay tăng dần → tắt máy bay → app tự `đã kết nối` lại. Không crash.
3. **Test server chết:** đang kết nối `/echo` thì tắt server (Ctrl+C) → xem chuỗi backoff → bật lại `npm start` → tự nối lại.
4. **Test reconnect chủ động:** đổi Wi-Fi ↔ 4G giữa chừng → log hiện "mạng trở lại → reconnect NGAY".
5. **Test dừng hẳn:** URL `/policy` → sau 3s app chuyển `hỏng: server yêu cầu dừng`, KHÔNG nối lại.

Ghi các số quan sát được (chuỗi delay, RTT echo vs slow) vào `docs/03_SO_LIEU_DO_DUOC.md`.
