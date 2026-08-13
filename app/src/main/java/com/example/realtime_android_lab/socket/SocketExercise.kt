package com.example.realtime_android_lab.socket

/**
 * BÀI 1 — BÀI TẬP CỦA TÔI.
 *
 * Luật chơi: KHÔNG mở các file Lesson trong `domain/`, `data/`, `ui/` (BackoffPolicy,
 * RealtimeRepositoryImpl, AndroidNetworkMonitor, SocketDebugViewModel…) cho tới khi
 * thật sự bí. Tự tra doc để biết dùng API nào — đó chính là giá trị của bài tập.
 * Các TODO dưới chỉ mô tả MỤC TIÊU, không nói gọi hàm gì.
 *
 * Thử tự chia theo Clean Architecture: quy tắc thuần (backoff) và interface ở domain,
 * OkHttp/ConnectivityManager ở data, ViewModel + Compose ở ui — domain KHÔNG biết OkHttp.
 *
 * Xong phần nào thì xoá comment "chưa làm" của phần đó và tự kiểm bằng mock server.
 */
class SocketExercise {

    // ----- 1. Tầng socket phát ra trạng thái và tin nhắn -----
    // TODO: biểu diễn kết nối bằng luồng bất đồng bộ, có đủ 4 trạng thái:
    //       đang kết nối / đã kết nối / đang nối lại / hỏng hẳn.
    // TODO: trạng thái và tin nhắn có bản chất khác nhau — một cái là GIÁ TRỊ HIỆN TẠI, một
    //       cái là SỰ KIỆN TRÔI QUA. Tự trả lời: nếu đưa cả hai ra bằng CÙNG một luồng thì
    //       cái nào sẽ sai, và sai lúc nào? (gợi ý kịch bản: màn hình bị dựng lại trong khi
    //       kết nối vẫn đang sống).
    // TODO: quyết định AI SỞ HỮU vòng đời kết nối — nó sống lâu hơn một màn hình hay chết
    //       cùng màn hình? Viết ra hệ quả của lựa chọn đó khi có HAI màn hình dùng chung
    //       một kết nối, rồi mới code.
    // TODO: cho phép gửi text ra kết nối đang sống; nếu chưa kết nối thì báo thất bại
    //       thay vì ném lỗi. Cẩn thận: "đã tạo socket" KHÁC "đã bắt tay xong".

    // ----- 2. Reconnect có kỷ luật -----
    // TODO: khi kết nối chết vì lý do có thể phục hồi, tự nối lại sau một khoảng chờ.
    // TODO: khoảng chờ phải tăng dần nhưng có TRẦN (không tăng vô hạn).
    // TODO: khoảng chờ phải có yếu tố NGẪU NHIÊN để hàng loạt client không nối lại
    //       cùng một thời điểm.
    // TODO: khi nối lại THÀNH CÔNG, khoảng chờ phải trở về mốc ban đầu.
    // TODO: nếu server đóng với tín hiệu "đừng nối lại nữa" thì phải dừng hẳn.

    // ----- 3. Phát hiện chết sớm & reconnect chủ động -----
    // TODO: phát hiện kết nối đã chết mà không phải ngồi đợi tầng vận chuyển tự timeout.
    // TODO: khi thiết bị vừa có mạng trở lại (sau khi từng mất), chủ động nối lại NGAY
    //       thay vì đợi hết khoảng chờ đang treo.

    // ----- 4. Số liệu cho màn debug -----
    // TODO: expose ra: trạng thái hiện tại, đã thử nối lại bao nhiêu lần, còn chờ bao lâu
    //       tới lần thử kế, và độ trễ vòng tròn (RTT) của lần đo gần nhất.

    // ----- 5. Test -----
    // TODO: viết unit test cho phần tính khoảng chờ: chứng minh nó có trần và không
    //       bao giờ vượt trần, ở nhiều lần thử khác nhau.
}
