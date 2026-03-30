# SocialConnect - Ứng dụng Tương tác Mạng xã hội

**SocialConnect** là một ứng dụng di động mạng xã hội đa tính năng, tập trung vào trải nghiệm người dùng hiện đại, kết nối cộng đồng thông qua các bài viết, hình ảnh, video và giao tiếp thời gian thực. Dự án được phát triển theo tiêu chuẩn Clean Architecture giúp mã nguồn dễ dàng mở rộng và bảo trì.

## 🚀 Tính năng chính (Features)

### 👤 Người dùng & Tài khoản
- **Xác thực:** Đăng nhập, Đăng ký và Quên mật khẩu với quy trình bảo mật.
- **Trang cá nhân:** Quản lý thông tin cá nhân, cập nhật ảnh đại diện và tiểu sử.
- **Tìm kiếm:** Tìm kiếm người dùng khác và các bài viết theo từ khóa.
- **Bạn bè:** Gửi/Nhận lời mời kết bạn, quản lý danh sách bạn bè.

### 📝 Bài viết & Nội dung
- **Bảng tin (Home Feed):** Cập nhật các bài viết mới nhất từ cộng đồng và bạn bè.
- **Tạo bài viết:** Hỗ trợ đăng tải văn bản đi kèm với hình ảnh hoặc video.
- **Tương tác:** Like (thích), Comment (bình luận) và Share (chia sẻ) bài viết.
- **Chi tiết bài viết:** Xem nội dung đầy đủ cùng danh sách người tương tác.
- **Đa phương tiện:** Trình phát video tích hợp mượt mà và hiển thị hình ảnh chất lượng cao.

### 💬 Giao tiếp & Thông báo
- **Nhắn tin (Real-time):** Trò chuyện tức thời giữa các người dùng thông qua Socket.IO.
- **Gọi điện (WebRTC):** Hỗ trợ đàm thoại video/voice chất lượng cao.
- **Thông báo:** Nhận thông báo realtime về các hoạt động mới (kết bạn, tương tác, tin nhắn).

---

## 🛠 Công nghệ sử dụng (Tech Stack)

- **Jetpack Compose:** Xây dựng giao diện UI Declarative hiện đại.
- **Clean Architecture & MVVM:** Thiết kế kiến trúc mã nguồn sạch và dễ bảo trì.
- **Dagger Hilt:** Quản lý Dependency Injection.
- **Retrofit & OkHttp:** Quản lý Network API và Logging.
- **Socket.IO:** Giao thức nhắn tin thời gian thực.
- **Stream WebRTC:** Nền tảng thực hiện cuộc gọi video.
- **Room Database:** Lưu trữ dữ liệu cục bộ ổn định.
- **Coil:** Thư viện tải và xử lý hình ảnh mạnh mẽ.
- **Media3 (ExoPlayer):** Trình phát Video chuẩn xác và mượt mà.
- **DataStore:** Quản lý Preferences người dùng.

---

## 🚀 Cài đặt và Chạy project

### Điều kiện tiên quyết
- **Android Studio Koala** hoặc mới hơn.
- Cài đặt **JDK 11+**.

### Hướng dẫn nhanh
1. Clone dự án.
2. Mở thư mục `Frontend` bằng Android Studio.
3. Chỉnh sửa tệp `local.properties` (xem dưới đây).
4. Run ứng dụng lên thiết bị ảo hoặc thực tế.

---

## ⚙️ Cách cài đặt và chạy project

### Yêu cầu hệ thống
- **Android Studio:** Phiên bản Koala (2024.1.1) hoặc mới hơn.
- **JDK:** Version 11 hoặc 17.
- **Thiết bị:** Emulator Android (API 26+) hoặc điện thoại thật.

### Các bước thực hiện
1. **Clone repository:**
   ```bash
   git clone https://github.com/chuonghoai/SocialConnect.git
   ```
2. **Mở project:**
   Mở Android Studio và chọn thư mục `Frontend`.
3. **Đồng bộ hóa Gradle:**
   Đợi Android Studio tải các dependencies và hoàn tất build lần đầu.
4. **Cấu hình Server:**
   Đảm bảo bạn đã có Backend running (SocialConnect-Server).
5. **Chạy ứng dụng:**
   Nhấn `Run` (biểu tượng Play) trong Android Studio.

---

## 🔑 Cấu hình .env (local.properties)

Dự án sử dụng tệp `local.properties` (hoặc `gradle/local.properties`) để quản lý các biến cấu hình. Vui lòng thêm các dòng sau:

```properties
# Endpoint của Server Backend (Mặc định 10.0.2.2 cho Android Emulator)
BASE_URL="http://10.0.2.2:8081/"

```

---

## 🤝 Đóng góp (Contributing)

Chúng tôi luôn hoan nghênh mọi đóng góp để hoàn thiện ứng dụng!
1. Fork dự án.
2. Tạo Brand mới cho tính năng của bạn (`git checkout -b feature/AmazingFeature`).
3. Commit thay đổi (`git commit -m 'Add some AmazingFeature'`).
4. Push lên Brand (`git push origin feature/AmazingFeature`).
5. Tạo Pull Request.

---

## ✍️ Credits / Author
*Dự án đang được phát triển bởi:*
- **Trương Hoài Chương**
- **Nguyễn Sư Thành Đạt**
- **Trịnh Đại Nghĩa**
- **Hà Trường Giang**

---
*© 2026 SocialConnect Team. Build with ❤️ for community.*
