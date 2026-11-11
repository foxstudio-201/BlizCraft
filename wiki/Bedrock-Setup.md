# Bedrock Setup

## 1. Thành phần

Đã test với:

| Plugin | Version |
|---|---|
| Geyser-Spigot | 2.12.1 |
| Floodgate-Spigot | 2.2.5 |
| ViaVersion | 5.12.3 |
| Paper | 26.2 build 135 |

- **Geyser**: chuyển Bedrock ↔ Java.
- **Floodgate**: xác thực Bedrock, không cần tài khoản Java.
- **ViaVersion**: cần khi Geyser dùng protocol Java mới hơn server (Geyser 2.12 cần 26.3, server 26.2).

---

## 2. Cài đặt

1. Copy vào `plugins/`:

```text
Geyser-Spigot.jar
Floodgate-Spigot.jar
ViaVersion.jar
```

2. Khởi động server một lần để Geyser/Floodgate tạo config.
3. Sửa `plugins/Geyser-Spigot/config.yml`:

```yaml
bedrock:
  address: 0.0.0.0
  port: 19132

remote:
  address: 127.0.0.1
  port: 25565
  auth-type: floodgate
```

4. Floodgate tạo `plugins/Floodgate/key.pem`.
   - Geyser + Floodgate trên Spigot thường tự dùng key của Floodgate.
   - Nếu Geyser báo thiếu key, copy:

```text
plugins/Floodgate/key.pem -> plugins/Geyser-Spigot/key.pem
```

5. Restart server.

Log thành công:

```text
[floodgate] Took ...ms to boot Floodgate
[Geyser-Spigot] Started Geyser on UDP port 19132
[BlizPocket] Đã bật GUI Bedrock native (Floodgate).
```

---

## 3. Kết nối

- Java: `IP:25565`
- Bedrock: `IP:19132`

Cùng máy:

```text
127.0.0.1:19132
```

LAN:

```text
IP-LAN-của-máy-chủ:19132
```

Internet:
- Forward **TCP + UDP 19132**.
- Geyser 2.12 dùng NetherNet/built-in signaling; cần mở đúng cả TCP/UDP nếu dùng ngoài mạng.
- Nếu dùng firewall, cho phép Java và Geyser.

---

## 4. Plugin nhận Bedrock thế nào?

Nếu Floodgate có mặt:
- Plugin gọi `isFloodgatePlayer(uuid)`.
- Java → Paper Dialog GUI.
- Bedrock → form native Cumulus.

Form Bedrock hỗ trợ:
- Menu chính.
- Tài khoản.
- Quy đổi: chọn loại tiền, mốc, tất cả, nhập số lượng.
- Đặt cược tùy chỉnh.
- Hũ.
- Hướng dẫn.
- Chi tiết phiên.
- Thế chấp/chuộc.

Inventory GUI dùng chung:
- GUI đặt cược 2 bên.
- Lịch sử.
- Cầu.
- Thế chấp.

---

## 5. Khác biệt Java/Bedrock

| Chức năng | Java | Bedrock |
|---|---|---|
| Menu | Dialog | SimpleForm/ModalForm |
| Nhập số tiền | Text input trong dialog | CustomForm input |
| Chọn loại tiền | Dropdown dialog | Dropdown form |
| GUI cược | Inventory | Inventory (Geyser) |
| Nút đóng | Dialog close | Form close |
| Anvil | Không dùng | Không dùng |

Tất cả thao tác đều có âm thanh click.

---

## 6. Version mismatch

### Geyser báo cần Java 26.3
- Cài ViaVersion.
- Hoặc dùng Geyser 2.11.3 nếu server 26.2 và client Bedrock không phải bản mới nhất.
- Geyser 2.12.1 hỗ trợ Bedrock 26.63; Geyser 2.11.3 hỗ trợ tới 26.62.

### Bedrock vào không được
Kiểm tra:
1. Cổng `19132` UDP/TCP đã mở/forward chưa.
2. `auth-type: floodgate` đúng chưa.
3. Floodgate đã load trước Geyser chưa.
4. `key.pem` tồn tại.
5. Client Bedrock có được Geyser version hỗ trợ không.
6. Log Geyser/Floodgate có lỗi auth/key không.

### Java vào được, Bedrock không
- Geyser bind 19132 chưa?
- Firewall Windows chặn UDP?
- Đang dùng IP sai (phải là IP máy chủ, port 19132).
- Client đang dùng phiên bản Bedrock quá mới so với Geyser.

---

## 7. Tắt Bedrock

Nếu không cần:
1. Xóa Geyser/Floodgate/ViaVersion.
2. Plugin vẫn chạy Java bình thường.
3. Không cần sửa config BlizPocket.

`softdepend: floodgate` đảm bảo plugin vẫn hoạt động khi Floodgate vắng mặt.

---

## 8. Bảo mật

- Floodgate cho phép người chơi Bedrock vào server online-mode.
- Không chia sẻ `key.pem`.
- Không mở RCON công khai.
- Chỉ forward port cần thiết.
- Dùng firewall/whitelist nếu server công khai.

Tiếp theo: [[Troubleshooting]]
