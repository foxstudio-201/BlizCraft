# Tiền tệ và quy đổi

## 1. BlizCoin

BlizCoin là tiền nội bộ, dùng cho mọi vé cược.

- Tên/ký hiệu cấu hình:

```yaml
currencies:
  base:
    name: "BlizCoin"
    symbol: "⛁"
```

- Số dư lưu trong `accounts.yml`.
- Không phụ thuộc plugin economy ngoài.

---

## 2. Các nguồn tiền hỗ trợ

| Provider | Cần cài | Ghi chú |
|---|---|---|
| `vault` | Vault + economy provider | EssentialsX/CMI/... |
| `playerpoints` | PlayerPoints | Dùng API point |
| `experience` | Không | Cấp kinh nghiệm |
| `item:<MATERIAL>` | Không | Vật phẩm trong túi |
| `custom:<key>` | PlaceholderAPI | Đọc balance + lệnh console |

**Mọi nguồn tiền đều tùy chọn.** Thiếu plugin tương ứng thì nguồn đó tự bị bỏ qua (log cảnh báo), không làm tắt BlizPocket; BlizCoin và các nguồn còn lại vẫn chạy bình thường. Chỉ những nguồn khả dụng mới hiện trong GUI quy đổi; nếu không có nguồn ngoài nào, GUI báo "không có loại tiền ngoài khả dụng".

### Alias

- Vault: `vault`, `eco`, `economy`, `essentials`, `xu`
- PlayerPoints: `playerpoints`, `points`, `point`, `pp`
- Experience: `experience`, `exp`, `xp`, `level`
- Item: `item:DIAMOND`, `diamond`, `emerald`...
- Custom: `custom:gems`, `gems`

---

## 3. Tỉ giá

Ví dụ mặc định:

```yaml
currencies:
  vault:
    rate: 1.0
  playerpoints:
    rate: 100.0
  experience:
    rate: 500.0
  items:
    entries:
      DIAMOND:
        rate: 5000.0
      EMERALD:
        rate: 2500.0
      GOLD_INGOT:
        rate: 500.0
```

Nghĩa là:
- 1 Vault = 1 BlizCoin.
- 1 PlayerPoint = 100 BlizCoin.
- 1 kim cương = 5.000 BlizCoin.

---

## 4. Công thức

```text
Nạp:
coins = floor(amount × rate × (1 - fee/100))

Rút:
cost  = ceil(amount × rate / (1 - fee/100))

Max ngoại tệ có thể rút:
max = floor(balance × (1 - fee/100) / rate)
```

- `amount` là số đơn vị ngoại tệ.
- `fee-percent` mặc định `0`.
- Kết quả BlizCoin làm tròn xuống.
- Chi phí rút làm tròn lên.

Ví dụ:
- Nạp 10 PlayerPoints, rate 100 → 1.000 BlizCoin.
- Rút 1 kim cương, rate 5.000, fee 0 → tốn 5.000 BlizCoin.
- Rút 1 Vault, rate 1 → tốn 1 BlizCoin.

---

## 5. GUI quy đổi

1. `/tx doicuoc`
2. Chọn loại tiền.
3. Bấm **Nạp** hoặc **Rút**.
4. Cửa sổ số lượng:
   - mốc sẵn trong `currencies.exchange.presets`,
   - **Tất cả**,
   - ô nhập tùy chỉnh,
   - **Dùng số đã nhập**,
   - Quay lại.
5. Kết quả + số dư mới hiển thị.

Có thể mở thẳng:

```text
/tx doicuoc diamond nap
/tx doicuoc vault rut
```

### Bedrock
Dùng CustomForm:
- dropdown loại tiền,
- dropdown mốc,
- input số lượng,
- nút submit.

---

## 6. Lệnh nạp/rút

```text
/tx nap <loai> <so_luong|all>
/tx rut <loai> <so_luong|all>
```

Ví dụ:

```text
/tx nap vault 500
/tx nap playerpoints 10
/tx nap diamond 2
/tx rut vault 50
/tx rut diamond 1
/tx nap vault all
```

`all` khi nạp dùng toàn bộ balance ngoại tệ.
`all` khi rút dùng max có thể rút theo số dư BlizCoin.

---

## 7. Cấu hình exchange

```yaml
currencies:
  exchange:
    enabled: true
    fee-percent: 0.0
    allow-withdraw: true
    presets: [100, 1000, 10000, 100000]
```

- `enabled`: bật quy đổi.
- `fee-percent`: phí.
- `allow-withdraw`: bật rút ngược.
- `presets`: mốc hiện trong dialog (tối đa 4).

---

## 8. Thêm item

```yaml
currencies:
  items:
    enabled: true
    entries:
      NETHERITE_INGOT:
        display: "Thỏi netherite"
        icon: NETHERITE_INGOT
        rate: 12000.0
```

Sau đó `/txa reload`.

Lưu ý:
- Nạp item sẽ **lấy item khỏi túi**.
- Rút item sẽ đưa item mới vào túi; không giữ NBT gốc cho loại tiền item thường.
- Nếu túi đầy, item dư được thả dưới chân.

---

## 9. Custom currency

Dùng khi plugin tiền có PlaceholderAPI + lệnh give/take.

```yaml
currencies:
  custom:
    enabled: true
    entries:
      gems:
        display: "Gems"
        icon: EMERALD
        rate: 10.0
        balance-placeholder: "%gems_balance%"
        withdraw-command: "gems take {player} {amount}"
        deposit-command: "gems give {player} {amount}"
```

- `{player}`: tên người chơi.
- `{amount}`: số nguyên đã kiểm tra.
- Lệnh chạy bằng console.
- Cần PlaceholderAPI để đọc balance.

Chỉ bật khi đã biết chính xác placeholder và lệnh của plugin tiền đó.

---

## 10. Lưu ý

- Vault balance là số thực; plugin dùng phần nguyên.
- PlayerPoints giới hạn `int`.
- Nếu provider không khả dụng, nó không xuất hiện trong GUI.
- Kiểm tra console/`/txa reload` để xem provider load.
- Log giao dịch nằm trong `transactions.log`.

Tiếp theo: [[Collateral]]
