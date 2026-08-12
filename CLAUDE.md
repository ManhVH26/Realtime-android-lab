# Quy tắc làm việc — Realtime Android Lab

> Claude PHẢI đọc file này trước khi làm bất cứ việc gì trong project.

---

## 0. Project này là gì

Sân tập kỹ thuật realtime để **chuẩn bị phỏng vấn team làm app Viettel Tammi** (khối OTT: chat / voice call / video call / meeting / streaming).

**Mục tiêu KHÔNG phải làm ra một app hoàn chỉnh.** Mục tiêu là:
1. Tôi tự code được các cơ chế realtime cốt lõi.
2. Tôi có **số liệu đo thật** để mang đi nói chuyện khi phỏng vấn.
3. Tôi trả lời trôi chảy checklist trong [docs/02_CHECKLIST_QUIZ.md](docs/02_CHECKLIST_QUIZ.md).

**Tài liệu chi tiết — đọc khi cần, đừng đọc hết mọi phiên:**
| File | Khi nào đọc |
|---|---|
| [docs/00_BOI_CANH_PHONG_VAN.md](docs/00_BOI_CANH_PHONG_VAN.md) | Cần biết app Tammi dùng gì, JD yêu cầu gì |
| [docs/01_LO_TRINH_5_BAI.md](docs/01_LO_TRINH_5_BAI.md) | **Trước khi tạo bất kỳ bài nào** — chứa đề bài từng bài |
| [docs/02_CHECKLIST_QUIZ.md](docs/02_CHECKLIST_QUIZ.md) | Khi tôi nói "quiz tôi đi" |
| [docs/03_SO_LIEU_DO_DUOC.md](docs/03_SO_LIEU_DO_DUOC.md) | Sau mỗi bài — nhắc tôi điền số |
| [docs/04_ON_TAP_CHI_TIET.md](docs/04_ON_TAP_CHI_TIET.md) | Tài liệu dài **cho tôi đọc ôn** (research đầy đủ + Q&A + kế hoạch 7 ngày). Claude **không cần đọc cả file** — chỉ mở đúng mục khi tôi hỏi về một chủ đề cụ thể, vì `00` và `01` đã có đủ thông tin để làm việc. |

---

## 1. Về tôi

- **Android Developer**, thành thạo **Kotlin**. Đang học thêm ML/DL.
- Tôi **đã biết lập trình**. KHÔNG giải thích: biến, hàm, OOP, Git, Gradle cơ bản, coroutine cơ bản, Compose cơ bản.
- Cần giải thích sâu: **giao thức mạng, kiến trúc realtime, media pipeline, hành vi hệ thống Android**.
- Khi giải thích khái niệm mạng/hệ thống, **đối chiếu với Kotlin/Android** — tôi hiểu nhanh hơn nhiều.
  *(vd: "SFU giống `SharedFlow` có filter riêng cho từng subscriber", "ICE gathering giống race trong `select {}`")*
- Quỹ thời gian: **5–10 giờ/tuần**. Đừng tạo bài dài quá một buổi ngồi.

## 2. Giao tiếp

- **Luôn trả lời bằng tiếng Việt.**
- Đi thẳng vào vấn đề. Không mở bài, không tóm tắt lại việc tôi vừa nói.
- Định danh code (class/hàm/biến/module) **tiếng Anh**; comment **tiếng Việt**.

## 3. Cách tạo bài — QUY TẮC CỨNG

1. **Tách 2 loại file mỗi bài, không được trộn:**
   - `XxxLesson.kt` hoặc `LESSON.md` → **bài giảng**: code mẫu đầy đủ, chạy được, comment dày.
   - `XxxExercise.kt` → **bài tập của tôi**: chỉ khung + `// TODO:`.
2. **File bài tập KHÔNG chứa lời giải và KHÔNG gợi ý tên API cụ thể.**
   TODO chỉ mô tả *mục tiêu*, không mô tả *gọi hàm nào*.
   - ✅ `// TODO: phát hiện kết nối chết sớm mà không đợi TCP timeout`
   - ❌ `// TODO: dùng pingInterval(20, TimeUnit.SECONDS)`
   Việc tôi phải tự tra doc chính là giá trị của bài tập.
3. **Mỗi bài một folder riêng.** Không nhồi 2 bài vào một file.
4. **Tạo MỘT bài mỗi lần tôi yêu cầu.** Xong thì dừng, nhắc tôi commit, đợi tôi gọi bài tiếp.
5. Sau mỗi bài, nhắc tôi cập nhật [docs/03_SO_LIEU_DO_DUOC.md](docs/03_SO_LIEU_DO_DUOC.md). **Chưa đo thì để `chưa đo` — tuyệt đối không điền số bịa.**
6. **Khi tôi gửi/bôi chọn một đoạn code rồi hỏi** ⇒ tôi đang bí ⇒ **viết comment giải thích trực tiếp vào file đó**, đừng chỉ trả lời ở chat.

## 4. KHÔNG tự chạy lệnh môi trường

**Tôi tự chạy, Claude chỉ viết lệnh ra kèm giải thích lệnh đó làm gì:**
- `.\gradlew.bat ...` (build / sync / assemble)
- `npm install`, `node server.js`
- `git add` / `commit` / `push` — **không commit hộ tôi trong mọi trường hợp**
- `adb`

Máy tôi: **Windows 11 + PowerShell**, Android Studio.
Lưu ý PowerShell: dùng `.\gradlew.bat`, không phải `./gradlew`; `&&` không hoạt động → dùng `;` hoặc `if ($?) { }`.

Claude **được phép** tự làm: đọc/ghi file source, sửa Gradle script, tạo folder.

## 5. Bảo mật

- Không hardcode key/secret/TURN credential. Để trong `local.properties` (đã bị `.gitignore`) và đọc qua `BuildConfig`.
- Kiểm tra `.gitignore` trước khi tạo file có cấu hình.

---

## 6. State thật của project (đã kiểm tra, đừng đoán lại)

Android Studio đã tạo sẵn, hiện là **single-module Compose app**:

| Hạng mục | Giá trị hiện tại |
|---|---|
| `rootProject.name` | `Realtime-android-lab` |
| Module | chỉ `:app` |
| namespace / applicationId | `com.example.realtime_android_lab` |
| AGP | `9.2.1` |
| Kotlin | `2.2.10` |
| compileSdk | `36` (minorApiLevel 1) |
| **minSdk** | **24** |
| targetSdk | `36` |
| UI | **Compose only** (Compose BOM `2026.02.01`, Material3). Không có XML layout. |
| Java | `VERSION_11` |
| Version catalog | `gradle/libs.versions.toml` — **chỉ có Compose + test cơ bản** |

**Lưu ý quan trọng khi sửa Gradle — đọc kỹ để không phá build:**
- Project **không** apply `org.jetbrains.kotlin.android`. AGP 9 đã có Kotlin support tích hợp. **Đừng thêm plugin đó** trừ khi build thật sự báo thiếu; nếu định thêm, kiểm tra trước rồi báo tôi.
- `release` block dùng DSL mới `optimization { enable = false }` (AGP 9) thay cho `minifyEnabled`. Khi làm thí nghiệm APK size ở Bài 4 thì **bật lên** để đo con số có ý nghĩa.
- `compileSdk` dùng block syntax mới `compileSdk { version = release(36) { minorApiLevel = 1 } }` — giữ nguyên style này.
- **minSdk 24 giữ nguyên** — đủ cho WebRTC, Media3 và Jitsi Meet SDK. Nhưng nhiều API tôi cần học chỉ có ở bản mới ⇒ luôn viết code có nhánh version check và **giải thích trong comment API đó xuất hiện từ API level nào**, vì Tammi thật đặt minSdk 29 và đây là chi tiết tôi sẽ được hỏi.
- Thư viện cần thêm (OkHttp, Room + KSP, Media3, WebRTC, Hilt, Paging 3, WorkManager) **chưa có gì trong catalog**. Khi thêm: đưa vào `libs.versions.toml` chứ không hardcode version trong `build.gradle.kts`. **Tự tra version tương thích với Kotlin 2.2.10 / AGP 9.2.1** (đặc biệt KSP phải khớp Kotlin), đừng đoán số.

## 7. Cấu trúc đích (Bài 0 sẽ dựng)

```
Realtimeandroidlab/
├── CLAUDE.md
├── docs/                   # 00..03, xem bảng ở mục 0
├── README.md               # phải có mục "Những gì tôi đo được"
├── app/                    # shell: menu chọn 5 demo
├── core-network/           # socket client, backoff+jitter, ConnectivityObserver
├── core-database/          # Room: entity, DAO, converters
├── feature-chat/           # Bài 1 + 2
├── feature-call/           # Bài 3 + 4
├── feature-player/         # Bài 5
└── server/                 # mock server Node (ws + signaling) + README riêng
```

Mỗi `feature-*` tách 3 layer `data / domain / ui`.
**`domain` không được biết OkHttp, Room hay WebRTC là gì** — JD yêu cầu Clean Architecture, và tôi sẽ bị hỏi đúng chỗ này.

---

## 8. Bắt đầu

Khi tôi nói **"bắt đầu"** hoặc **"làm Bài 0"**:
1. Đọc [docs/01_LO_TRINH_5_BAI.md](docs/01_LO_TRINH_5_BAI.md) phần Bài 0.
2. Chuyển single-module → multi-module, tạo `app` menu 5 nút (chưa cần nội dung).
3. Viết ra lệnh sync/build cho tôi **tự chạy**.
4. Dừng. Nhắc commit. Đợi tôi gọi Bài 1.

Nếu tôi sốt ruột bảo "gộp lại làm nhanh" → nhắc tôi **một lần** rằng **Bài 1 và Bài 2 bị hỏi nhiều nhất**, đừng cắt ngắn hai bài đó. Tôi vẫn quyết định cuối cùng.