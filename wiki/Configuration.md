# Cấu hình

File chính:

```text
plugins/BlizPocket/config.yml
plugins/BlizPocket/messages.yml
```

Sau khi sửa:

```text
/txa reload
```

Reload áp dụng cho config, messages, tỉ giá, thế chấp, bossbar, âm thanh và thời gian phiên mới. Java class không reload; thay jar cần restart.

---

## 1. language

```yaml
language:
  number-format: "#,##0"
  date-format: "dd/MM/yyyy HH:mm:ss"
  timezone: "Asia/Ho_Chi_Minh"
```

- `number-format`: định dạng số.
- `date-format`: ngày giờ trong lịch sử/hợp đồng.
- `timezone`: múi giờ hiển thị; để trống dùng múi giờ máy chủ.

---

## 2. storage

```yaml
storage:
  save-interval-seconds: 300
  max-sessions: 300
  log-transactions: true
  debug: false
```

- `save-interval-seconds`: chu kỳ tự lưu.
- `max-sessions`: số phiên lịch sử giữ lại.
- `log-transactions`: ghi `transactions.log`.
- `debug`: log chi tiết.

Dữ liệu còn được lưu ngay sau các giao dịch quan trọng, khi `/txa save`, khi tắt server.

---

## 3. update-checker

```yaml
update-checker:
  enabled: true
  github-repo: "foxstudio-201/BlizCraft"
  notify-admins: true
  timeout-seconds: 10
```

- Chỉ kiểm tra **một lần khi server khởi động xong** (`ServerLoadEvent` STARTUP).
- Không kiểm tra định kỳ, không chạy khi `/txa reload`.
- Restart server → kiểm tra lại một lần.
- Console luôn nhận cảnh báo; admin online nhận chat nếu bật.
- Nếu API lỗi, không thử lại cho tới lần restart sau.

## 4. account

```yaml
account:
  starting-balance-enabled: true
  starting-balance: 1000
  max-history: 100
```

- `starting-balance-enabled`: bật/tắt cấp tiền tân thủ.
- `starting-balance`: số BlizCoin cấp khi tạo tài khoản; nếu tắt thì tài khoản mới có `0`.
- `max-history`: số phiên gần nhất lưu trong lịch sử cá nhân.

---

## 4. room

```yaml
room:
  require: true
  max-players: 0
  announce-join-leave: true
  announce-scope: room
  remove-on-quit: true
  forfeit-bet-on-leave: true
```

- `require`: bắt buộc vào phòng mới được cược/nhận bossbar.
- `max-players`: `0` = không giới hạn.
- `announce-join-leave`: bật thông báo vào/ra phòng.
- `announce-scope`:
  - `room`: chỉ người trong phòng thấy (mặc định, đúng luật không thông báo người ngoài).
  - `server`: toàn server thấy.
- `remove-on-quit`: tự rời phòng khi thoát server.
- `forfeit-bet-on-leave`: rời phòng/thoát server khi vé chưa có kết quả sẽ mất vé; tiền vào hũ.

Ví dụ thông báo:
- `PlayerA đã vào phòng cược. (3 người)`
- `PlayerA đã rời phòng cược. (còn 2 người)`

---

## 5. session

```yaml
session:
  betting-seconds: 45
  lock-seconds: 5
  roll-seconds: 4
  result-seconds: 6
  next-round-delay-seconds: 5
  countdown-seconds: 5
  dice:
    sides: 6
    symbols: ["⚀", "⚁", "⚂", "⚃", "⚄", "⚅"]
    roll-interval-ticks: 2
  titles:
    enabled: true
    fade-in-ms: 0
    stay-ms: 900
    fade-out-ms: 200
```

Chu kỳ:

1. `BETTING`: mở cược.
2. `LOCKED`: khóa cược, title đếm ngược.
3. `ROLLING`: quay xúc xắc.
4. `RESULT`: công bố + trả thưởng.
5. `WAITING`: chờ phiên mới.

Lưu ý:
- Không còn tùy chọn bỏ qua phiên rỗng. Không có ai cược vẫn quay và ghi lịch sử.
- `countdown-seconds` là số giây hiện title khóa; mặc định 5.
- `roll-interval-ticks`: tần suất đổi mặt xúc xắc.
- `symbols`: biểu tượng xúc xắc; có thể thay bằng ký tự khác.

---

## 6. betting

```yaml
betting:
  min: 100
  max: 1000000
  max-per-session: 0
  step: 100
  default-amount: 1000
  presets: [100, 500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000]
```

- Mỗi người chỉ được **một vé/phiên**.
- Không thể đặt thêm hoặc đổi Tài ↔ Xỉu sau khi đã đặt.
- `max-per-session`: giới hạn thêm cho vé; `0` = không giới hạn.
- `step`: bước tăng/giảm.
- `presets`: các mức sẵn trên GUI (tối đa 9).

---

## 7. payout

```yaml
payout:
  jackpot-contribution-percent: 25.0
  house-rake-enabled: true
  house-rake-percent: 5.0
  rake-to-jackpot: false
  house-win-on-triple: true
  house-random-chance-percent: 0.0
  minimum-win-profit-percent: 100.0
  house-covers-shortfall: true
```

- `minimum-win-profit-percent`: lợi nhuận tối thiểu khi thắng.
  - `100` = gốc + 100% lời (2x).
  - `0` = chỉ chia theo pool, có thể hoà vốn.
- `house-covers-shortfall`: nhà cái bù nếu tiền người thua không đủ mức lời tối thiểu.
  - `true`: thắng luôn có lời; tiền bù được tạo ra (có thể gây lạm phát, cân nhắc tỉ lệ).
  - `false`: chỉ chia pool; không có người thua đối diện thì hoà vốn.

Cách chia khi không phải nhà cái thắng:

```text
losingPool = tổng tiền người thua
jackpotAdd = losingPool * jackpot-contribution-percent%
houseCut   = losingPool * house-rake-percent% (nếu bật)
prizePool  = losingPool - jackpotAdd - houseCut
```

- Người thắng nhận lại tiền gốc + phần chia theo tỉ lệ tiền đặt.
- Bộ ba + `house-win-on-triple: true` → nhà cái thắng.
- `house-random-chance-percent`: tỉ lệ random nhà cái thắng.
- `rake-to-jackpot`: cộng phần lãi nhà cái vào hũ thay vì bỏ đi.

---

## 8. jackpot

```yaml
jackpot:
  enabled: true
  starting-amount: 0
  explode-chance-percent: 3.0
  min-explode-amount: 10000
  announce-to-room: true
```

- Hũ nhận phần trăm từ tiền thua, tiền forfeit, và có thể cả lãi nhà cái.
- Nổ hũ random nếu có người thắng và hũ đủ mức tối thiểu.
- Chỉ người thắng được chia hũ, theo tỉ lệ tiền đặt.
- Hũ chia hết về 0, không để dư.

---

## 9. bossbar

```yaml
bossbar:
  enabled: true
  color:
    betting: BLUE
    locked: YELLOW
    rolling: PURPLE
    result: GREEN
    waiting: WHITE
    idle: RED
  overlay: PROGRESS
  compact-numbers: true
```

- Chỉ người có tài khoản + trong phòng thấy bossbar.
- Bossbar hiển thị `Tài`, `Xỉu`, `Hũ`, thời gian.
- `compact-numbers`: `1.5K`, `1.2M` để không tràn. Đặt `false` để hiện số đầy đủ.
- Màu hợp lệ: `RED`, `BLUE`, `GREEN`, `YELLOW`, `PURPLE`, `PINK`, `WHITE`.
- Overlay: `PROGRESS`, `NOTCHED_6`, `NOTCHED_10`, `NOTCHED_12`, `NOTCHED_20`.

---

## 10. sounds

```yaml
sounds:
  enabled: true
  bet: { key: "ui.button.click", volume: 0.8, pitch: 1.2 }
```

- Mọi nút GUI/dialog/chọn mục dùng âm `click`.
- Các âm khác: đặt cược, khóa, quay, kết quả, thắng, thua, hòa, nổ hũ, quy đổi, lỗi.
- `key` là namespaced sound key của Minecraft.

---

## 11. currencies

```yaml
currencies:
  base:
    name: "BlizCoin"
    symbol: "⛁"
  exchange:
    enabled: true
    fee-percent: 0.0
    allow-withdraw: true
    presets: [100, 1000, 10000, 100000]
  vault:
    enabled: true
    display: "Xu (Vault / EssentialsX)"
    icon: GOLD_INGOT
    rate: 1.0
  playerpoints:
    enabled: true
    display: "PlayerPoints"
    icon: AMETHYST_SHARD
    rate: 100.0
```

- `base`: tên/ký hiệu BlizCoin.
- `fee-percent`: phí quy đổi.
- `allow-withdraw`: cho phép rút BlizCoin → ngoại tệ.
- `presets`: mốc hiện trong dialog nạp/rút.
- `rate`: số BlizCoin nhận được cho 1 đơn vị ngoại tệ.

Công thức:

```text
Nạp:  coins = floor(amount * rate * (1 - fee/100))
Rút:  cost  = ceil(amount * rate / (1 - fee/100))
Max rút = floor(balance * (1 - fee/100) / rate)
```

Xem thêm [[Currency-and-Exchange]].

---

## 12. collateral

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
```

- Chỉ item trong `allowed-items` mới được thế chấp.
- `rate`: BlizCoin nhận cho 1 item chưa enchant.
- `enchant-bonus-percent-per-level`: mỗi cấp enchant cộng thêm %.
- `interest-percent`: lãi khi chuộc.
- `duration-hours`: hạn hợp đồng; quá hạn mất item.
- `min-payout`: giá trị hợp đồng tối thiểu.
- `max-contracts-per-player`: số hợp đồng tối đa.
- `expire-check-seconds`: tần suất kiểm tra hết hạn; mở GUI/lệnh cũng kiểm tra ngay.

Xem thêm [[Collateral]].

---

## 13. messages.yml

- Hỗ trợ **MiniMessage**.
- Placeholder dạng `{key}`.
- Nếu thiếu key, plugin dùng messages mặc định trong jar, không hiện `bossbar.xxx`.
- Màu MiniMessage hợp lệ: `<red>`, `<blue>`, `<light_purple>`, `<gold>`, `<yellow>`, `<gray>`, `<dark_gray>`, `<dark_red>`, `<aqua>`, `<white>`, `<black>`, `<green>`.
- **Không dùng** `<purple>` hoặc `<pink>` (không hợp lệ). Plugin tự chuẩn hóa các tag này, nhưng nên dùng `<light_purple>`.

Ví dụ sai:

```yaml
rolling: "<purple>PHIÊN #{session}</purple>"
```

Ví dụ đúng:

```yaml
rolling: "<light_purple>PHIÊN #{session}</light_purple>"
```

---

## 14. Gợi ý cấu hình

### Phiên nhanh để test

```yaml
session:
  betting-seconds: 10
  lock-seconds: 5
  roll-seconds: 2
  result-seconds: 3
  next-round-delay-seconds: 3
```

### Thông báo toàn server

```yaml
room:
  announce-scope: server
```

### Tắt tiền tân thủ

```yaml
account:
  starting-balance-enabled: false
```

### Bossbar số đầy đủ

```yaml
bossbar:
  compact-numbers: false
```

### Cho phép thêm item thế chấp

Thêm vào `collateral.allowed-items`:

```yaml
    NETHERITE_INGOT:
      display: "Thỏi netherite"
      icon: NETHERITE_INGOT
      rate: 12000.0
```

Sau đó `/txa reload`.

Tiếp theo: [[Commands-and-Permissions]]
