# Thế chấp tài sản

## 1. Khái niệm

Người chơi đưa vật phẩm cho plugin để nhận BlizCoin ngay.

- Item được lưu lại.
- Người chơi phải trả tiền chuộc trước hạn để lấy lại.
- Quá hạn: item mất vĩnh viễn.
- Chỉ item nằm trong `allowed-items` mới được thế chấp.

---

## 2. Cấu hình

```yaml
collateral:
  enabled: true
  duration-hours: 24.0
  interest-percent: 10.0
  enchant-bonus-percent-per-level: 10.0
  min-payout: 100
  max-contracts-per-player: 3
  expire-check-seconds: 60
  allowed-items:
    DIAMOND:
      display: "Kim cương"
      icon: DIAMOND
      rate: 3000.0
    EMERALD:
      display: "Ngọc lục bảo"
      icon: EMERALD
      rate: 1500.0
    GOLD_BLOCK:
      display: "Khối vàng"
      icon: GOLD_BLOCK
      rate: 4000.0
    NETHERITE_INGOT:
      display: "Thỏi netherite"
      icon: NETHERITE_INGOT
      rate: 12000.0
```

- `rate`: giá cho 1 item chưa enchant.
- `enchant-bonus-percent-per-level`: item enchant được cộng giá.
- `interest-percent`: lãi khi chuộc.
- `duration-hours`: hạn hợp đồng.
- `min-payout`: giá trị tối thiểu.
- `max-contracts-per-player`: số hợp đồng tối đa.
- `expire-check-seconds`: tần suất tự kiểm tra hết hạn.

---

## 3. Định giá

Mỗi stack:

```text
enchantLevels = tổng level của tất cả enchant trên item
multiplier    = 1 + enchantLevels × (enchant-bonus-percent-per-level / 100)
value         = floor(amount × rate × multiplier)
```

Tổng principal = tổng value của các stack đã lấy.

Ví dụ:
- Kim cương rate 3.000.
- Kiếm kim cương Sharpness V: 5 cấp, bonus 10%/cấp → +50%.
- Giá trị = 3.000 × 1.5 = 4.500 BlizCoin.

Lưu ý:
- Enchant được tính theo level, cộng dồn nhiều enchant.
- Item gốc (NBT/enchant) được lưu và trả lại nguyên bản khi chuộc.

---

## 4. Lãi và hạn

```text
repay = principal + ceil(principal × interest-percent / 100)
```

Mặc định 10%:
- Nhận 9.000 → chuộc 9.900.
- Nhận 4.500 → chuộc 4.950.

Nếu quá `duration-hours`:
- Hợp đồng bị xóa.
- Item mất.
- Không hoàn tiền.
- Người chơi online nhận thông báo.

Hợp đồng cũng được kiểm tra ngay khi mở GUI/dùng lệnh, không chỉ theo task.

---

## 5. GUI

`/tx thechap`

- Nửa trên: các item được phép, tỉ giá, số lượng trong túi, giá trị ước tính, enchant bonus.
- Nửa dưới: hợp đồng đang hoạt động.
- Mỗi hợp đồng hiển thị:
  - ID,
  - item/số lượng,
  - số tiền đã nhận,
  - phí chuộc,
  - hạn,
  - thời gian còn lại.
- Click item → form nhập số lượng.
- Click hợp đồng → xác nhận chuộc.

Bedrock dùng CustomForm/ModalForm tương ứng.

---

## 6. Lệnh

```text
/tx thechap
/tx thechap dat <item> <so_luong|all>
/tx thechap tra <id>
/tx thechap list
```

Ví dụ:

```text
/tx thechap dat diamond 3
/tx thechap dat diamond all
/tx thechap tra 1
```

Admin:

```text
/txa collateral <player>
/txa collateral clear <player>
```

`clear` tịch thu toàn bộ tài sản thế chấp, không hoàn item.

---

## 7. Quy trình

1. Có item được phép trong túi.
2. Mở GUI hoặc dùng lệnh `dat`.
3. Item bị lấy khỏi túi.
4. Principal được cộng vào BlizCoin.
5. Hợp đồng có hạn + phí chuộc.
6. Trả nợ trước hạn → nhận lại item.
7. Quá hạn → item mất.

Người chơi **không đủ tiền chuộc** có thể kiếm thêm trước hạn; nếu vẫn không đủ và quá hạn, item mất.

---

## 8. Dữ liệu

Lưu trong:

```text
plugins/BlizPocket/collateral.yml
```

Bao gồm:
- ID hợp đồng.
- Chủ sở hữu/tên.
- Material/display.
- Số lượng.
- Danh sách ItemStack gốc.
- Principal/phí chuộc.
- Thời gian tạo/hết hạn.

Có thể backup file này để giữ hợp đồng.

---

## 9. Thiết kế giá

Nên đặt `rate` thấp hơn giá trị thị trường của item để tránh lạm phát.

Ví dụ:
- Kim cương bán 5.000 → thế chấp 3.000.
- Netherite bán 15.000 → thế chấp 12.000.

Enchant bonus khuyến khích item có giá trị cao, nhưng cũng nên kiểm soát để tránh item enchant quá mạnh tạo tiền quá lớn.

---

## 10. Lưu ý

- Chỉ item trong config.
- Item số lượng lớn được chia stack.
- Nếu túi đầy khi chuộc, item dư rơi dưới chân.
- Hợp đồng đã tạo vẫn dùng rate/interest lúc tạo, kể cả sau khi đổi config.
- Xóa `collateral.yml` sẽ mất toàn bộ hợp đồng/item.

Tiếp theo: [[Data-and-Backups]]
