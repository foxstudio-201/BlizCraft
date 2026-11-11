# Release và kiểm tra cập nhật

## 1. Version

Version nằm trong:

- `pom.xml` — `<version>1.0.0</version>`
- `src/main/resources/plugin.yml` — `version: 1.0.0`

Khi phát hành bản mới, tăng cả hai (hoặc dùng script) rồi commit.

Ví dụ:

```text
1.0.0 -> 1.0.1
```

---

## 2. Release workflow

File:

```text
.github/workflows/release.yml
```

Workflow:
1. Checkout source.
2. Cài Java 25.
3. Chạy `mvn -B -DskipTests clean package`.
4. Đọc version từ `pom.xml`.
5. Tạo GitHub Release với tag `v<version>`.
6. Upload jar `target/BlizPocket-<version>.jar`.

### Cách phát hành bằng tag

```bash
git tag v1.0.1
git push origin v1.0.1
```

Workflow `Release` tự chạy và tạo release `v1.0.1`.

### Cách phát hành thủ công

Vào:

```text
Actions -> Release -> Run workflow
```

Có thể nhập tag tùy chỉnh; để trống sẽ dùng `v<version>` trong pom.

### Kết quả

Release có:
- tag,
- tiêu đề,
- release notes tự động,
- file jar đính kèm.

---

## 3. Update checker trong plugin

Plugin kiểm tra bản mới **một lần duy nhất khi server khởi động xong**.

- Dùng `ServerLoadEvent`.
- Chỉ chạy với `LoadType.STARTUP`.
- Không chạy khi `/reload`.
- Không có task lặp vài phút.
- Restart server → kiểm tra lại một lần.

Nguồn kiểm tra:

```text
https://api.github.com/repos/foxstudio-201/BlizCraft/releases/latest
```

Cấu hình:

```yaml
update-checker:
  enabled: true
  github-repo: "foxstudio-201/BlizCraft"
  notify-admins: true
  timeout-seconds: 10
```

### Thông báo

- Console luôn nhận cảnh báo nếu có bản mới.
- Nếu `notify-admins: true`, người online có quyền `bliztaixiu.admin` nhận thông báo chat.
- Không broadcast cho player thường.
- Nếu API lỗi/rate limit/không có release, plugin chỉ log nhẹ và không thử lại cho tới lần restart sau.

### So sánh version

- Bỏ tiền tố `v`.
- So sánh từng phần số.
- `1.0.1` mới hơn `1.0.0`.
- `1.0.0` không báo nếu release cũng `1.0.0`.
- Phần `-beta` được bỏ qua khi so sánh cơ bản.

Ví dụ:

- Local `1.0.0`, release `v1.0.1` → báo.
- Local `1.0.0`, release `v1.0.0` → không báo.
- Local `1.0.1`, release `v1.0.0` → không báo.

---

## 4. Quy trình phát hành khuyến nghị

1. Sửa code/tài liệu.
2. Tăng version trong pom + plugin.yml.
3. Build local:
   ```bash
   mvn clean package
   ```
4. Test.
5. Commit.
6. Tạo tag:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
7. Chờ workflow Release thành công.
8. Restart server để update checker kiểm tra bản mới.

---

## 5. Lưu ý

- Update checker cần Internet và GitHub API.
- GitHub API có rate limit; kiểm tra một lần/startup là đủ.
- Nếu repo private, API cần token; repo hiện public.
- Không đặt token trong source.
- Release workflow cần `contents: write`.
- Nếu tag đã tồn tại, workflow có thể lỗi; dùng tag mới.
