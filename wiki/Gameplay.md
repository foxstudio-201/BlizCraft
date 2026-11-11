# Luật chơi

## 1. Tài/Xỉu

3 xúc xắc, mỗi mặt `1-6`.

- Tổng `4-10`: **XỈU**
- Tổng `11-17`: **TÀI**
- Bộ ba (3 mặt giống nhau): **NHÀ CÁI THẮNG** theo mặc định.

Có thể cấu hình thêm tỉ lệ random nhà cái thắng:

```yaml
payout:
  house-random-chance-percent: 0.0
```

---

## 2. Chu kỳ phiên

```text
BETTING (N giây cuối tự khóa) → ROLLING → RESULT → WAITING → BETTING
```

| Giai đoạn | Việc xảy ra |
|---|---|
| BETTING | Mở đặt cược, bossbar hiện Tài/Xỉu/Hũ |
| BETTING (cuối) | `lock-seconds` giây cuối tự khóa, title đếm ngược 10→1, không nhận vé mới |
| ROLLING | Title hiển thị xúc xắc quay |
| RESULT | Công bố kết quả, trả thưởng, hiện tổng kết |
| WAITING | Đếm ngược trước phiên mới |

Thời gian mặc định:

```yaml
session:
  betting-seconds: 45
  lock-seconds: 10
  roll-seconds: 4
  result-seconds: 6
  next-round-delay-seconds: 5
```

---

## 3. Một vé mỗi phiên

- Mỗi người chỉ được đặt **một lần**.
- Không đặt thêm cùng cửa.
- Không đổi Tài ↔ Xỉu.
- Nếu đã đặt rồi, lệnh/GUI báo:

```text
Bạn đã đặt cược phiên #x rồi. Mỗi phiên chỉ được đặt MỘT lần và không thể đổi cửa.
```

---

## 4. Rời phòng / thoát server

Nếu đang có vé chưa có kết quả:

- Rời phòng hoặc thoát server: **mất vé**.
- Tiền vé vào hũ.
- Thống kê ghi nhận 1 trận thua.
- Vào lại phòng cũng không được đặt lại trong cùng phiên.

Cấu hình:

```yaml
room:
  forfeit-bet-on-leave: true
```

Đặt `false` nếu muốn vé vẫn được tính sau khi rời phòng.

---

## 5. Phiên rỗng

Nếu không có ai đặt cược:

- Phiên **vẫn khóa cược**.
- Vẫn quay xúc xắc.
- Vẫn công bố kết quả cho phòng.
- Vẫn ghi lịch sử với `bettors: 0`.

Không có cấu hình bỏ qua phiên rỗng.

---

## 6. Trả thưởng

Khi không phải nhà cái thắng:

```text
losingPool = tổng tiền người thua
jackpotAdd = losingPool × jackpot-contribution-percent%
houseCut   = losingPool × house-rake-percent% (nếu bật)
prizePool  = losingPool - jackpotAdd - houseCut
```

Mỗi người thắng nhận:

```text
baseProfit = prizePool × (tiền_đặt / tổng_tiền_đặt_của_người_thắng)
minimumPayout = tiền_gốc + ceil(tiền_gốc × minimum-win-profit-percent / 100)
payout = max(tiền_gốc + baseProfit, minimumPayout)   # nếu house-covers-shortfall=true
```

Mặc định `minimum-win-profit-percent: 100.0`:
- Thắng luôn nhận **gốc + 100% lời** (tổng 2x tiền cược).
- Nếu tiền người thua chia ra ít hơn mức này, **nhà cái bù phần thiếu**.
- Nếu tiền người thua chia ra nhiều hơn, người thắng nhận theo tỉ lệ cao hơn.

Ví dụ:

- Tài: A đặt 1.100, B đặt 3.000.
- Xỉu: C đặt 2.000.
- Kết quả Tài 11.
- `losingPool = 2.000`.
- Hũ 25% = 500, lãi nhà cái 5% = 100.
- `prizePool = 1.400`.
- Chia pool: A 375, B 1.025.
- Mức tối thiểu 100%: A phải nhận 2.200, B phải nhận 6.000.
- Nhà cái bù: A +725, B +1.975 (tổng 2.700).
- C mất 2.000; hũ +500.

Như vậy thắng **luôn có lời**, không còn báo hoà vốn khi bên thắng không có người thua đối diện.

Nếu muốn cơ chế chia pool thuần (có thể hoà vốn):

```yaml
payout:
  minimum-win-profit-percent: 0.0
  house-covers-shortfall: false
```

Khi đó:
- Không có người thua → chỉ nhận lại gốc.
- Thống kê có thể ghi hòa.

---

## 7. Nhà cái thắng

Xảy ra khi:
- Bộ ba và `house-win-on-triple: true`.
- Hoặc random theo `house-random-chance-percent`.

Khi đó:
- Không ai được trả.
- Toàn bộ tiền cược vào hũ.
- Người chơi nhận thông báo `NHÀ CÁI THẮNG`.

---

## 8. Hũ tích lũy

Nguồn hũ:
- Phần trăm tiền thua.
- Toàn bộ tiền khi nhà cái thắng.
- Tiền vé bị forfeit.
- Phần lãi nhà cái nếu `rake-to-jackpot: true`.

Nổ hũ:
- Random theo `explode-chance-percent`.
- Chỉ khi hũ đạt `min-explode-amount`.
- Chỉ chia cho **người thắng phiên đó**.
- Chia theo tỉ lệ tiền đặt.
- Hũ về 0, không để dư.

Người thua không nhận hũ.

---

## 9. Thông báo

### Trong phòng
- Phiên bắt đầu.
- Khóa cược.
- Đang quay.
- Kết quả chung.
- Tiền Tài/Xỉu/Hũ.
- Nổ hũ.

### Cá nhân
Người có vé nhận thêm:
- Thắng + số tiền.
- Thua - số tiền.
- Hòa.
- Nhà cái thắng.
- Nổ hũ (nếu được chia).

### Người ngoài phòng
- Không bossbar.
- Không title.
- Không thông báo phiên/kết quả.
- Chỉ nhận phản hồi trực tiếp khi tự chạy lệnh.

Nếu bật `room.announce-scope: server`, thông báo vào/ra phòng sẽ gửi toàn server (tùy chọn).

---

## 10. Tài khoản và thống kê

Tài khoản lưu:
- Số dư BlizCoin.
- Tổng đặt.
- Tổng nhận.
- Thắng/thua/hòa.
- Chuỗi thắng hiện tại/cao nhất.
- Thắng lớn nhất.
- Ngày tạo.
- Lần cược cuối.
- Lịch sử các phiên gần đây.

Tiếp theo: [[GUI-Guide]]


## 11. Bật/tắt title

Mỗi player có thể tắt title giữa màn hình:

```text
/tx title
/tx title on
/tx title off
```

Hoặc vào **Tài khoản** → nút **Bật/Tắt title**.

- Bật: title khóa cược, xúc xắc, kết quả, nổ hũ hiển thị như cũ.
- Tắt: chỉ còn bossbar và thông báo chat; title bị bỏ qua.
- Lựa chọn lưu theo tài khoản.
