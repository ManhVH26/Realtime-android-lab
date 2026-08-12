# 02 — Checklist tự kiểm tra & Quiz

> Khi tôi nói **"quiz tôi đi"**: hỏi **từng câu một**, đợi tôi trả lời, rồi **chấm** (đúng / thiếu gì / sai gì) và bổ sung phần thiếu. Không được hỏi dồn nhiều câu một lượt, cũng không được tự trả lời trước khi tôi nói.
>
> Nếu tôi trả lời sai một câu ⇒ ghi lại, và cuối buổi tổng kết **những chỗ tôi cần ôn lại**, kèm trỏ tới bài/file tương ứng.

---

## WebSocket

- [ ] Mô tả handshake WebSocket. `Sec-WebSocket-Accept` để làm gì?
- [ ] Vì sao cần ping/pong ở tầng WebSocket dù TCP đã có keepalive?
- [ ] Half-open connection là gì, phát hiện bằng cách nào?
- [ ] Backoff + jitter: thiếu jitter thì hỏng chuyện gì ở quy mô triệu user?
- [ ] Khi nào **KHÔNG** nên dùng WebSocket? (kể được ít nhất 3 tình huống)
- [ ] So sánh WebSocket vs SSE vs MQTT — chọn cái nào cho IoT và vì sao?
- [ ] 1 triệu user online cùng lúc, server scale thế nào? Bottleneck nằm ở đâu?
- [ ] Token auth hết hạn giữa phiên socket thì xử lý ra sao?

## Chat

- [ ] Thiết kế màn hình chat cho app hàng triệu user (5 phút, có vẽ)
- [ ] Vì sao Room là Single Source of Truth mà không phải network?
- [ ] Đảm bảo **không mất** và **không trùng** tin nhắn bằng cách nào?
- [ ] Vì sao không sort tin nhắn theo timestamp của client?
- [ ] Vì sao không phân trang bằng OFFSET? Keyset pagination hoạt động thế nào?
- [ ] Ảnh/video trong chat gửi thế nào? Vì sao không đẩy qua WebSocket?
- [ ] Read receipt trong nhóm 500 người — làm sao không nổ số lượng message?
- [ ] XMPP: JID + resource là gì, 3 loại stanza, MUC, và **Stream Management (XEP-0198)** giải quyết vấn đề gì cho mobile?

## Video call (WebRTC)

- [ ] Vẽ luồng thiết lập một cuộc gọi WebRTC từ đầu đến khi có hình
- [ ] WebRTC có định nghĩa signaling không? Ý nghĩa của câu trả lời đó là gì?
- [ ] STUN vs TURN — khi nào **buộc** phải relay? Bao nhiêu % cuộc gọi trên thực tế?
- [ ] Trickle ICE giải quyết vấn đề gì?
- [ ] Media của WebRTC được mã hoá bằng gì, có tuỳ chọn tắt không?
- [ ] Gọi nhóm 20 người: mesh / MCU / SFU chọn gì, tính toán băng thông ra sao?
- [ ] Simulcast và last-N của Jitsi giải quyết vấn đề gì khác nhau?
- [ ] Cuộc gọi vỡ tiếng / giật hình — debug theo thứ tự nào, đọc chỉ số nào trong `getStats()`?
- [ ] Phân biệt "nghẽn mạng" vs "thiếu CPU" qua chỉ số nào?
- [ ] App đã bị kill mà vẫn nhận được cuộc gọi đến bằng cách nào? Vì sao không giữ socket sống ở background?
- [ ] Android 14 yêu cầu gì với foreground service khi đang gọi?
- [ ] Đổi Wi-Fi sang 4G giữa cuộc gọi thì làm gì để không đứt?

## Streaming (Media3)

- [ ] HLS vs WebRTC cho livestream **có tương tác** — chọn gì? Kiến trúc hybrid nghĩa là sao?
- [ ] Vì sao video mất 2–3s mới lên hình? Nêu 3 cách giảm.
- [ ] ABR quyết định đổi chất lượng dựa trên những gì?
- [ ] Tuning `LoadControl` là đánh đổi giữa hai thứ gì?
- [ ] Player live giữ đúng độ trễ mục tiêu bằng cách nào (không seek)?
- [ ] Feed video kiểu reels — vì sao không tạo/hủy ExoPlayer mỗi lần scroll?
- [ ] Media3 khác ExoPlayer 2 ở chỗ nào?

## Tammi-specific — mấy câu tôi muốn tự tin nhất

- [ ] Vì sao "gọi không tốn data" **xung đột** với P2P của WebRTC? Đánh đổi là gì?
- [ ] "Lọc ồn AI" nằm ở đâu trong audio pipeline? Ràng buộc khó nhất là gì (gợi ý: không phải accuracy)? Đo bằng metric gì?
- [ ] APK 200MB của một app Communication đến từ đâu? Giảm bằng cách nào?
- [ ] Tammi minSdk 29 — đó là quyết định kiến trúc gì, được lợi gì?
- [ ] Với ~50 dịch vụ, phần nào nên native, phần nào nên mini-app/H5? Đánh đổi?

---

## Câu tôi sẽ hỏi lại nhà tuyển dụng (chọn 3–4)

Đây là phần thể hiện trình độ hơn cả trả lời. **Nhắc tôi ôn cả phần này.**

1. Signaling của Tammi theo XMPP hay protocol tự định nghĩa trên WebSocket? Nếu XMPP thì có dùng Stream Management để resume không?
2. Gọi nhóm/meeting dùng Jitsi Videobridge hay SFU tự phát triển? Có simulcast + last-N chưa, giới hạn số người hiện tại?
3. Tính năng không tốn data có buộc media relay qua hạ tầng Viettel không? Team cân đối thế nào giữa chi phí băng thông TURN/SFU và chất lượng cuộc gọi?
4. Lọc ồn AI chạy on-device hay server-side? Model do team Viettel AI train hay SDK bên thứ ba?
5. APK ~200MB — team đang xử lý size thế nào? Có dynamic feature module hay tách bớt Jitsi SDK?
6. Với ~50 dịch vụ, phần nào native, phần nào mini-app/H5? Team em phỏng vấn nằm ở khối OTT core hay khối service?
7. Chat có E2EE chưa, và "định danh xác thực" ảnh hưởng thế nào tới thiết kế bảo mật?