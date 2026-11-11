# Dữ liệu và backup

## 1. File dữ liệu

Tất cả nằm trong:

```text
plugins/BlizPocket/
```

| File | Nội dung |
|---|---|
| `config.yml` | Cấu hình plugin |
| `messages.yml` | Tin nhắn/GUI text |
| `accounts.yml` | Số dư, thống kê, lịch sử cá nhân |
| `sessions.yml` | Lịch sử phiên + tiền hũ + next-id |
| `collateral.yml` | Hợp đồng thế chấp + ItemStack gốc |
| `transactions.log` | Log giao dịch (nếu bật) |

---

## 2. accounts.yml

Mỗi tài khoản lưu:
- UUID, tên.
- Số dư BlizCoin.
- Tổng đặt/nhận.
- Thắng/thua/hòa.
- Chuỗi thắng hiện tại/cao nhất.
- Thắng lớn nhất.
- Ngày tạo.
- Lần cược cuối.
- Lịch sử các phiên gần đây.

Không sửa tay nếu không hiểu cấu trúc; nên dùng lệnh admin.

---

## 3. sessions.yml

Lưu:
- `next-id`: ID phiên tiếp theo.
- `jackpot`: tiền hũ hiện tại.
- Danh sách phiên gần nhất:
  - ID, thời gian,
  - xúc xắc/tổng/kết quả,
  - tổng Tài/Xỉu,
  - số người cược/thắng,
  - hũ cộng/nổ,
  - lãi nhà cái,
  - top người thắng.

Dùng cho GUI lịch sử/cầu và chi tiết phiên.

---

## 4. collateral.yml

Lưu:
- ID hợp đồng.
- Chủ sở hữu/tên.
- Material/display.
- Số lượng.
- Danh sách ItemStack gốc.
- Principal, phí chuộc.
- Ngày tạo/hết hạn.

File này rất quan trọng: mất file = mất item thế chấp.

---

## 5. Cơ chế lưu

- Lưu ngay sau giao dịch quan trọng:
  - đặt cược,
  - trả thưởng,
  - forfeit,
  - quy đổi,
  - thế chấp/chuộc,
  - admin sửa tiền/hũ.
- Lưu định kỳ theo `storage.save-interval-seconds` (mặc định 300 giây).
- Có hàng đợi chống ghi đè: nếu đang ghi mà có thay đổi mới, sẽ ghi tiếp sau.
- `/txa save` buộc ghi ngay.
- Khi tắt server: plugin cố gắng lưu toàn bộ và hoàn tiền vé chưa có kết quả.

Dữ liệu không chỉ nằm trong RAM.

---

## 6. Backup

Nên backup định kỳ:

```text
plugins/BlizPocket/
```

Cách đơn giản:
1. Dừng server hoặc `/txa save`.
2. Copy cả thư mục `plugins/BlizPocket/`.
3. Nén theo ngày.

Không chỉ backup `config.yml`; cần cả `accounts.yml`, `sessions.yml`, `collateral.yml`.

---

## 7. Restore

1. Dừng server.
2. Thay thư mục `plugins/BlizPocket/` bằng bản backup.
3. Khởi động.
4. Kiểm tra `/txa stats`, `/txa hu info`, `/tx hũ`, `/tx thechap list`.

Nếu chỉ muốn reset:
- Xóa `accounts.yml` → mất tài khoản.
- Xóa `sessions.yml` → mất lịch sử + hũ về mặc định.
- Xóa `collateral.yml` → mất toàn bộ item thế chấp.
- Giữ `config.yml`/`messages.yml` nếu chỉ muốn reset dữ liệu.

Luôn backup trước khi xóa.

---

## 8. transactions.log

Nếu `storage.log-transactions: true`, plugin ghi:
- đặt cược,
- trả thưởng,
- nạp/rút,
- forfeit,
- thế chấp/chuộc,
- admin sửa dữ liệu.

Dùng để đối soát khi có khiếu nại.

Có thể xoay vòng/xóa log cũ khi quá lớn; không ảnh hưởng dữ liệu chính.

---

## 9. Quyền file

Đảm bảo user chạy server có quyền đọc/ghi thư mục `plugins/BlizPocket/`.
Nếu không, plugin log lỗi ghi file và dữ liệu có thể không bền.

---

## 10. Sự cố khi tắt server

- Tắt bình thường: plugin hoàn tiền vé chưa có kết quả và lưu dữ liệu.
- Crash/kill cứng: dữ liệu đã lưu tới giao dịch gần nhất vẫn còn; vé của phiên đang chạy có thể không được hoàn.
- Nên dùng lệnh stop/launcher stop thay vì kill process.

Tiếp theo: [[Bedrock-Setup]]
