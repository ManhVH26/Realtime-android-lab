# Quy tắc làm việc — Realtime Android Lab

> Claude PHẢI đọc file này trước khi làm bất cứ việc gì trong project.

---

## 0. Project này là gì

Sân tập kỹ thuật realtime, **hiện chỉ tập trung MỘT chủ đề: WebSocket** (kết nối bền bỉ,
reconnect có kỷ luật, phát hiện chết sớm, đo RTT).

Mục tiêu KHÔNG phải làm ra app hoàn chỉnh. Mục tiêu là tôi tự code được cơ chế và có
**số liệu đo thật**.

Toàn bộ `docs/` (bối cảnh phỏng vấn, lộ trình 5 bài, checklist quiz, tài liệu ôn) **đã bị
xoá** để project chỉ còn phần WebSocket. Nếu cần lấy lại: `git show af57482 --stat`.

**[WEBSOCKET.md](WEBSOCKET.md)** — lý thuyết giao thức + giải thích code. Đọc khi cần nhắc lại
một cơ chế, hoặc khi ôn phỏng vấn (có mục Q&A ở cuối).

---

## 1. Về tôi

- **Android Developer**, thành thạo **Kotlin**. Đang học thêm ML/DL.
- Tôi **đã biết lập trình**. KHÔNG giải thích: biến, hàm, OOP, Git, Gradle cơ bản,
  coroutine cơ bản, Compose cơ bản.
- Cần giải thích sâu: **giao thức mạng, kiến trúc realtime, hành vi hệ thống Android**.
- Khi giải thích khái niệm mạng/hệ thống, **đối chiếu với Kotlin/Android** — tôi hiểu nhanh
  hơn nhiều. *(vd: "half-open giống `await()` trên một `Deferred` không bao giờ complete")*
- Quỹ thời gian: **5–10 giờ/tuần**. Đừng tạo việc dài quá một buổi ngồi.

## 2. Giao tiếp

- **Luôn trả lời bằng tiếng Việt.**
- Đi thẳng vào vấn đề. Không mở bài, không tóm tắt lại việc tôi vừa nói.
- Định danh code (class/hàm/biến/module) **tiếng Anh**; comment **tiếng Việt**.

## 3. Cách tạo bài — QUY TẮC CỨNG

1. **Tách 2 loại file, không được trộn:**
   - file bài giảng (`XxxLesson.kt` / code trong `domain|data|ui`) → code mẫu đầy đủ,
     chạy được, comment dày.
   - `XxxExercise.kt` → **bài tập của tôi**: chỉ khung + `// TODO:`.
2. **File bài tập KHÔNG chứa lời giải và KHÔNG gợi ý tên API cụ thể.**
   TODO chỉ mô tả *mục tiêu*, không mô tả *gọi hàm nào*.
   - ✅ `// TODO: phát hiện kết nối chết sớm mà không đợi TCP timeout`
   - ❌ `// TODO: dùng pingInterval(20, TimeUnit.SECONDS)`
   Việc tôi phải tự tra doc chính là giá trị của bài tập.
3. **Làm MỘT việc mỗi lần tôi yêu cầu.** Xong thì dừng, nhắc tôi commit, đợi tôi gọi tiếp.
4. **Khi tôi gửi/bôi chọn một đoạn code rồi hỏi** ⇒ tôi đang bí ⇒ **viết comment giải thích
   trực tiếp vào file đó**, đừng chỉ trả lời ở chat.
5. Có số đo thì ghi vào comment/README. **Chưa đo thì ghi `chưa đo` — tuyệt đối không điền
   số bịa.**

## 4. KHÔNG tự chạy lệnh môi trường

**Tôi tự chạy, Claude chỉ viết lệnh ra kèm giải thích lệnh đó làm gì:**
- `.\gradlew.bat ...` (build / sync / assemble)
- `npm install`, `node server.js`
- `git add` / `commit` / `push` — **không commit hộ tôi trong mọi trường hợp**
- `adb`

Máy tôi: **Windows 11 + PowerShell**, Android Studio.
Lưu ý PowerShell: dùng `.\gradlew.bat`, không phải `./gradlew`; `&&` không hoạt động →
dùng `;` hoặc `if ($?) { }`.

Claude **được phép** tự làm: đọc/ghi file source, sửa Gradle script, tạo folder.

## 5. Bảo mật

- Không hardcode key/secret/TURN credential. Để trong `local.properties` (đã bị
  `.gitignore`) và đọc qua `BuildConfig`.
- Kiểm tra `.gitignore` trước khi tạo file có cấu hình.

---

## 6. State thật của project (đã kiểm tra, đừng đoán lại)

**Single-module Compose app**, chỉ có `:app`.

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
| Version catalog | `gradle/libs.versions.toml` — Compose, OkHttp, coroutines, test cơ bản |

**Lưu ý quan trọng khi sửa Gradle — đọc kỹ để không phá build:**
- Project **không** apply `org.jetbrains.kotlin.android`. AGP 9 đã có Kotlin support tích
  hợp. **Đừng thêm plugin đó** trừ khi build thật sự báo thiếu; nếu định thêm, kiểm tra
  trước rồi báo tôi.
- `release` block dùng DSL mới `optimization { enable = false }` (AGP 9) thay cho
  `minifyEnabled`.
- `compileSdk` dùng block syntax mới `compileSdk { version = release(36) { minorApiLevel = 1 } }`
  — giữ nguyên style này.
- **minSdk 24 giữ nguyên.** Nhưng nhiều API tôi cần học chỉ có ở bản mới ⇒ luôn viết code có
  nhánh version check và **giải thích trong comment API đó xuất hiện từ API level nào**.
- Thêm thư viện thì đưa vào `libs.versions.toml`, **không hardcode version** trong
  `build.gradle.kts`. **Tự tra version tương thích với Kotlin 2.2.10 / AGP 9.2.1** (đặc biệt
  KSP phải khớp Kotlin), đừng đoán số.

---

## 7. Cấu trúc code WebSocket hiện tại

**9 file, không hơn.** Đã cố tình gộp: mỗi khái niệm một file, không tách type ra file riêng
chỉ để cho "đúng convention".

```
app/src/main/java/com/example/realtime_android_lab/
├── MainActivity.kt                   # menu tối giản
└── socket/
    ├── SocketExercise.kt             # BÀI TẬP của tôi — chỉ TODO, không lời giải
    ├── di/SocketGraph.kt             # DI thủ công, repository singleton theo Application
    ├── domain/                       # KHÔNG được biết OkHttp / Android là gì
    │   ├── RealtimeRepository.kt      #   interface + ConnectionState + CloseReason
    │   ├── NetworkMonitor.kt          #   interface + NetworkStatus
    │   └── BackoffPolicy.kt           #   exponential backoff + full jitter (unit test được)
    ├── data/                         # chỗ DUY NHẤT biết OkHttp / ConnectivityManager
    │   ├── RealtimeRepositoryImpl.kt  #   vòng lặp reconnect, generation stamp, ping/pong
    │   └── AndroidNetworkMonitor.kt   #   callbackFlow + stateIn(Eagerly)
    └── ui/
        ├── SocketContract.kt          #   UiState + Intent + Effect + label()
        ├── SocketDebugViewModel.kt    #   MVI: Intent → reduce → State, Effect qua Channel
        └── SocketDebugScreen.kt
```

**KHÔNG có tầng UseCase.** Trước có 5 class, mỗi class một dòng `= repository.x()` — không
thêm hành vi nào. ViewModel phụ thuộc trực tiếp **interface `RealtimeRepository` của domain**,
chiều phụ thuộc vẫn đúng. Tạo use case khi có business rule thật, không tạo trước cái rỗng.
Lý do đầy đủ ghi trong KDoc của `SocketDebugViewModel`.

**Ranh giới bắt buộc: `domain` không được import OkHttp, Android, hay bất kỳ framework nào.**
Hiện tại ranh giới này chỉ là kỷ luật package, chưa có compiler enforce (chưa tách module).

**Chỉ dùng `wss://`** (server mock deploy trên Render), không có preset `ws://` local. Vì vậy
KHÔNG có `app/src/debug/AndroidManifest.xml` và không khai `usesCleartextTraffic` ở đâu cả — từ
API 28 Android chặn cleartext mặc định và ta không còn chỗ nào cần mở.

Nếu sau này cần trỏ vào server node chạy local (`ws://10.0.2.2:8080`) thì **phải** tạo lại
`app/src/debug/AndroidManifest.xml` với `<application android:usesCleartextTraffic="true" />`,
nếu không OkHttp ném `CLEARTEXT communication not permitted`. Đặt ở source set `debug` để release
build vẫn bị chặn.

Server mock (route `/echo`, `/slow`, `/drop`, `/policy`) deploy trên Render:
`wss://realtime-ws-lab.onrender.com`.

---

## 8. Việc còn nợ (đã phân tích, chưa làm)

- Tách module `:core-network` để **compiler** chặn domain import OkHttp, thay vì tự giữ kỷ luật.
- Thêm `turbine` + `kotlinx-coroutines-test` + `mockwebserver` vào catalog; viết test cho
  reducer và cho vòng lặp reconnect. Hiện chỉ có `BackoffPolicyTest`.
- `SocketGraph` → Hilt `@Singleton` (đang là service locator).
- Dời logic đo RTT (`PING_PREFIX`, parse payload) từ ViewModel xuống tầng data — format wire
  message không phải việc của UI.
- Hoist ViewModel ra khỏi `SocketDebugScreen` (nhận `state` + `onIntent`) để preview được.
- Buộc vòng đời kết nối vào process foreground (`ProcessLifecycleOwner`); hiện kết nối sống
  tới khi bấm "Ngắt" hoặc process chết.
