# GUI Guide

Plugin có 2 lớp GUI:

- **Java**: Inventory GUI + Paper Dialog API 26.2.
- **Bedrock**: Inventory GUI + form native Cumulus qua Floodgate.

Không dùng anvil cho nhập số lượng.

---

## 1. GUI đặt cược

Kích thước: 54 ô.

Bố cục:

| Vùng | Nội dung |
|---|---|
| Trên trái | Cửa **TÀI** (đỏ) |
| Trên phải | Cửa **XỈU** (xanh) |
| Giữa | Số dư, vé hiện tại, kết quả gần nhất |
| Hàng 5 | Các mức cược sẵn |
| Hàng 6 | Giảm, tăng, x2, đặt lại, tùy chỉnh, tài khoản, lịch sử, quy đổi, thoát/đóng |

### Nút chính

- **TÀI / XỈU**: đặt mức đang chọn.
- **Mức sẵn**: chọn nhanh mức cược từ config.
- **- / +**: tăng giảm theo `betting.step`.
- **x2**: nhân đôi mức đang chọn (clamp max).
- **Đặt lại**: lặp lại cửa + số tiền của lần cược trước.
- **Tùy chỉnh**: mở dialog/form nhập số tiền.
- **Tài khoản**: xem thống kê.
- **Lịch sử**: lịch sử phiên + nút cầu.
- **Quy đổi**: nạp/rút ngoại tệ.
- **Hũ**: xem hũ/chance.
- **Thoát phòng**: Shift + chuột trái vào ô barrier.
- **Đóng**: chuột trái vào barrier khi không ở phòng.

### Cập nhật

- GUI tự cập nhật mỗi giây.
- Đặt cược/GUI mở sẽ cập nhật ngay.
- Số dư, vé, tổng Tài/Xỉu, thời gian phiên đều đổi theo trạng thái.

---

## 2. Dialog Java

Các form dùng Paper Dialog API:

- Menu chính.
- Tạo tài khoản.
- Tài khoản/thống kê.
- Quy đổi tiền.
- Nhập số lượng nạp/rút.
- Đặt cược tùy chỉnh.
- Chi tiết phiên.
- Hũ.
- Hướng dẫn.
- Thế chấp.
- Chuộc hợp đồng.

### Quy đổi

Luồng:

1. `/tx doicuoc` → chọn **Nạp** hoặc **Rút**.
2. Cửa sổ số lượng:
   - chọn loại tiền,
   - chọn mốc sẵn,
   - **Tất cả**,
   - ô nhập tùy chỉnh,
   - **Dùng số đã nhập**,
   - **Quay lại**.
3. Kết quả hiển thị trong chat, GUI mở lại với số dư mới.

Có thể mở thẳng cửa sổ số lượng:

```text
/tx doicuoc vault nap
/tx doicuoc diamond rut
```

### Điều hướng quay lại

- Dialog/form mở từ menu chính có nút **Quay lại** đưa về đúng màn hình trước đó (menu chính, lịch sử, cầu, thế chấp...), không đóng ra game.
- Dialog con mở tiếp (ví dụ: chọn tiền → nhập số lượng) giữ một **ngăn xếp điều hướng**; bấm Quay lại sẽ lùi từng bước.
- Đóng dialog con bằng phím Esc cũng quay về màn hình trước.
- Thao tác quy đổi thành công xong sẽ đưa về GUI/dialog trước với số dư mới.
- Thoát hẳn khỏi menu chính mới đóng ra game.

---

## 3. GUI Bedrock

Người chơi Bedrock (Floodgate) nhận form native:

- Menu chính.
- Tài khoản.
- Quy đổi (dropdown loại tiền + mốc + ô số lượng).
- Đặt cược tùy chỉnh.
- Hũ, hướng dẫn, chi tiết phiên.
- Thế chấp/chuộc.

Các GUI rương dùng chung:
- Đặt cược 2 bên.
- Lịch sử.
- Cầu.
- Thế chấp.

Form dùng màu legacy `§`, dropdown và input native, không cần anvil.

Xem [[Bedrock-Setup]].

---

## 4. Lịch sử và cầu

### Lịch sử
- 45 phiên/trang.
- Mỗi item: phiên, thời gian, kết quả, xúc xắc, Tài/Xỉu, người thắng, hũ.
- Click để xem chi tiết.
- Nút chuyển trang, xem cầu, quay lại GUI cược.

### Cầu
- Hiển thị các phiên gần nhất dưới dạng ô đỏ/xanh/đen.
- Click để xem chi tiết phiên.
- Chuyển trang, quay lại lịch sử.

---

## 5. Thế chấp

- Item được phép nằm ở nửa trên.
- Hợp đồng đang hoạt động ở nửa dưới.
- Click item → form nhập số lượng (hoặc `all`).
- Click hợp đồng → xác nhận trả nợ/chuộc.
- Refresh, menu, đóng ở hàng cuối.

Chi tiết: [[Collateral]].

---

## 6. Âm thanh

Mọi lần chọn GUI/dialog đều phát âm `click`, cấu hình trong:

```yaml
sounds:
  click:
    key: "ui.button.click"
    volume: 0.5
    pitch: 1.0
```

Các âm khác:
- đặt cược,
- khóa cược,
- quay xúc xắc,
- kết quả,
- thắng/thua/hòa,
- nổ hũ,
- quy đổi,
- lỗi.

---

## 7. Bossbar

Chỉ người có tài khoản + trong phòng thấy.

Hiển thị:

```text
PHIÊN #15 | Đang xúc xắc | 2s | Tài 1K | Xỉu 0 | Hũ 0
```

Số rút gọn bật/tắt:

```yaml
bossbar:
  compact-numbers: true
```

Màu trạng thái cấu hình theo phase.

---

## 8. Lưu ý màu

Plugin dùng MiniMessage. Một số tag Minecraft không phải tag MiniMessage hợp lệ:

- Sai: `<purple>`, `<pink>`
- Đúng: `<light_purple>`

Plugin đã chuẩn hóa `<purple>/<pink>/<orange>`, nhưng nên dùng tag đúng.

Tiếp theo: [[Currency-and-Exchange]]
