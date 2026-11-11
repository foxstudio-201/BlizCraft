# Cài đặt

## 1. Yêu cầu

| Thành phần | Bắt buộc | Phiên bản/ghi chú |
|---|---|---|
| Paper | Có | 26.2 (test build 135) |
| Java | Có | 25 |
| Vault | Khuyến nghị | Kinh tế ngoài |
| Economy provider | Khuyến nghị | EssentialsX 2.22.0 đã test |
| PlayerPoints | Tuỳ chọn | Quy đổi point |
| PlaceholderAPI | Tuỳ chọn | Custom currency |
| Geyser + Floodgate | Tuỳ chọn | Người chơi Bedrock |
| ViaVersion | Tuỳ chọn | Khi Geyser cần protocol mới hơn server |

Plugin có thể chạy chỉ với BlizCoin nội bộ, nhưng nên cài Vault + EssentialsX để người chơi có nguồn tiền ban đầu.

## 2. Cài plugin

1. Tải `BlizPocket-1.0.0.jar` từ [Releases](https://github.com/foxstudio-201/BlizCraft/releases) hoặc build từ source.
2. Copy vào:

```text
plugins/BlizPocket-1.0.0.jar
```

3. Cài các dependency vào `plugins/`:

```text
Vault.jar
EssentialsX.jar
PlayerPoints.jar
Geyser-Spigot.jar        # nếu dùng Bedrock
Floodgate-Spigot.jar     # nếu dùng Bedrock
ViaVersion.jar           # nếu Geyser cần
```

4. Khởi động server.
5. Kiểm tra console:

```text
[BlizPocket] Đã nạp 0 tài khoản, 0 phiên, hũ 0.
[BlizPocket] Đã nạp 5 loại tiền, khả dụng: 5.
[BlizPocket] Thế chấp: bật, 5 loại item, 0 người có hợp đồng.
[BlizPocket] BlizPocket đã bật. BlizCoin: BlizCoin.
```

6. Vào game:

```text
/tx dangky
/tx vaophong
```

## 3. Build từ source

Yêu cầu JDK 25 và Maven 3.9+.

```bash
git clone https://github.com/foxstudio-201/BlizCraft.git
cd BlizCraft
mvn clean package
```

Jar:

```text
target/BlizPocket-1.0.0.jar
```

Repo không chứa thư viện server, jar Paper, dữ liệu hay log chạy thử.

## 4. Cấu trúc dữ liệu sau lần chạy đầu

```text
plugins/BlizPocket/
  config.yml
  messages.yml
  accounts.yml          # tự tạo khi có dữ liệu
  sessions.yml
  collateral.yml        # tự tạo khi có hợp đồng
  transactions.log      # nếu bật log
```

## 5. Cấp quyền

Ví dụ LuckPerms:

```text
/lp group default permission set bliztaixiu.use true
/lp group default permission set bliztaixiu.play true
/lp group default permission set bliztaixiu.account true
/lp group default permission set bliztaixiu.exchange true
/lp group default permission set bliztaixiu.collateral true
```

Admin:

```text
/lp group admin permission set bliztaixiu.admin true
```

Hoặc đơn giản:

```text
op <tên>
```

## 6. Nâng cấp

1. Backup `plugins/BlizPocket/`.
2. Dừng server.
3. Thay jar.
4. Khởi động lại.
5. Chạy `/txa reload` nếu chỉ sửa config/messages.

Không xóa các file `.yml` dữ liệu khi nâng cấp, trừ khi muốn reset.

## 7. Update checker

Plugin kiểm tra GitHub Release một lần sau khi server khởi động xong. Cấu hình tại:

```yaml
update-checker:
  enabled: true
  github-repo: "foxstudio-201/BlizCraft"
  notify-admins: true
  timeout-seconds: 10
```

Xem [[Release-and-Updates]].

## 8. Gỡ plugin

1. Dừng server.
2. `/txa save` trước khi dừng nếu có thể.
3. Xóa jar.
4. Giữ hoặc backup thư mục `plugins/BlizPocket/`.

Tiếp theo: [[Configuration]]
