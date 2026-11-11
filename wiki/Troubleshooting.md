# Troubleshooting

## 1. Title/bossbar hiện `<purple>...</purple>` hoặc tag lạ

Nguyên nhân: dùng tag không hợp lệ của MiniMessage.

Sai:

```yaml
<purple>...</purple>
<pink>...</pink>
```

Đúng:

```yaml
<light_purple>...</light_purple>
```

Plugin đã tự chuẩn hóa `<purple>`, `<pink>`, `<orange>` trong code, nhưng nên sửa `messages.yml` cho đúng.

Sau khi sửa:

```text
/txa reload
```

hoặc restart.

---

## 2. Bossbar/title hiện `bossbar.betting`, `title.lock-sub`...

Nguyên nhân: thiếu key trong `messages.yml`.

Plugin có fallback về `messages.yml` mặc định trong jar. Nếu vẫn thấy key:
1. Kiểm tra file có đúng YAML không.
2. `/txa reload`.
3. Restart nếu vừa thay jar.
4. Gửi log/key chính xác khi báo lỗi.

Không đặt key mới trùng tên section.

---

## 3. Màu title/bossbar không đúng

- Dùng MiniMessage, không dùng mã `&`/`§` trong `messages.yml` (trừ form Bedrock được chuyển đổi).
- Màu hợp lệ: `<red>`, `<blue>`, `<light_purple>`, `<gold>`, `<yellow>`, `<gray>`, `<dark_gray>`, `<dark_red>`, `<aqua>`, `<white>`, `<black>`, `<green>`.
- Với Bedrock, tránh gradient phức tạp; plugin đã dùng màu đơn giản cho title/bossbar.
- Bossbar quá dài: bật `bossbar.compact-numbers: true`.

---

## 4. EssentialsX báo “unsupported server version”

Đây là cảnh báo của EssentialsX với Paper 26.2. Trong test:
- Vault vẫn hook Essentials Economy.
- Nạp/rút Vault vẫn hoạt động.

Nếu economy không hoạt động:
1. Kiểm tra `/vault-info` hoặc log Vault.
2. Đảm bảo Vault + EssentialsX cùng bật.
3. Cập nhật EssentialsX khi có bản hỗ trợ 26.2.
4. Dùng economy provider khác có Vault.

---

## 5. `Address already in use` / `session.lock`

Nguyên nhân: có server khác đang chạy cùng thư mục/cổng.

Xử lý:
1. Kiểm tra:

```text
netstat -ano | findstr :25565
netstat -ano | findstr :19132
```

2. Tắt server cũ/manual process.
3. Không mở 2 server cùng lúc.
4. Nếu dùng launcher, tránh start khi một process Java khác đang giữ world.

Không xóa `session.lock` khi server còn chạy; chỉ xóa khi chắc chắn không còn process.

---

## 6. Không nạp/rút được Vault

- Vault chưa cài/chưa hook.
- Không có economy provider.
- Player chưa có tiền.
- Xem log khởi động:

```text
Đã nạp ... loại tiền, khả dụng: ...
```

- `/txa reload`.
- `/tx doicuoc` xem provider có hiện không.

---

## 7. PlayerPoints không khả dụng

- Chưa cài PlayerPoints.
- Sai phiên bản plugin.
- API chưa sẵn sàng.
- Xem lại log provider.
- Thử `/tx nap playerpoints 10` để kiểm tra.

---

## 8. Không vào được phòng/không đặt được cược

Kiểm tra lần lượt:
1. Đã có tài khoản chưa? `/tx dangky`.
2. Đã vào phòng chưa? `/tx vaophong`.
3. Phiên có đang `BETTING` không?
4. Đã đặt vé trong phiên chưa? Mỗi phiên chỉ 1 vé.
5. Số dư đủ không?
6. Vé có dưới `betting.min` hoặc trên `betting.max` không?
7. Có bị `max-per-session` không?
8. Có quyền `bliztaixiu.play` không?

---

## 9. Rời phòng mất tiền

Đây là hành vi cấu hình:

```yaml
room:
  forfeit-bet-on-leave: true
```

Nếu muốn giữ vé khi rời phòng:

```yaml
forfeit-bet-on-leave: false
```

Sau đó `/txa reload`.

Lưu ý: tắt forfeit có thể cho phép người chơi rời phòng để tránh thông báo nhưng vẫn nhận kết quả.

---

## 10. Thế chấp

### Không thế chấp được
- Item không nằm trong `allowed-items`.
- Không đủ số lượng.
- Hợp đồng dưới `min-payout`.
- Đã đạt `max-contracts-per-player`.
- Chức năng đang tắt.

### Không chuộc được
- Không đủ BlizCoin.
- Hợp đồng đã hết hạn/bị xóa.
- Sai ID.
- Mất `collateral.yml`.

### Item mất
- Quá hạn `duration-hours`.
- Admin dùng `clear`.
- File dữ liệu bị xóa/hỏng.

---

## 11. Dữ liệu không lưu

- Kiểm tra quyền ghi thư mục `plugins/BlizPocket/`.
- `/txa save`.
- Xem log lỗi ghi file.
- Không kill cứng server.
- Kiểm tra ổ đĩa đầy.
- Backup trước khi sửa file tay.

---

## 12. Bedrock không kết nối

Xem [[Bedrock-Setup]]. Kiểm tra nhanh:
- `Geyser-Spigot.jar`, `Floodgate-Spigot.jar`.
- `auth-type: floodgate`.
- Port 19132 UDP/TCP.
- `key.pem`.
- Client Bedrock/Geyser version.
- ViaVersion nếu Geyser cần protocol mới hơn server.

---

## 13. Bedrock không thấy form

- Floodgate chưa load.
- Player không phải Bedrock (Java).
- Plugin log không có `Đã bật GUI Bedrock native`.
- Form gửi lỗi; xem chat/log.
- Thử lệnh `/tx` lại.
- Inventory GUI (đặt cược/lịch sử/cầu) vẫn dùng được ngay cả khi form lỗi.

---

## 14. Lệnh admin không dùng được

- Cần `bliztaixiu.admin` hoặc `op`.
- Subpermission:
  - reload,
  - economy,
  - jackpot,
  - session,
  - stats,
  - collateral.
- Console có toàn quyền.

---

## 15. Log cần gửi khi báo lỗi

- `logs/latest.log`
- Phiên bản Paper/Java.
- Danh sách plugin.
- Đoạn `config.yml`/`messages.yml` liên quan.
- Key bị thiếu (nếu có).
- Log Geyser/Floodgate nếu lỗi Bedrock.

Che token/mật khẩu trước khi gửi công khai.
