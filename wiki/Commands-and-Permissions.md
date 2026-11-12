# Lệnh và quyền

## Lệnh người chơi

Alias chính: `/tx`, `/btx`, `/bliztaixiu`, `/bp`, `/blizpocket`.

| Lệnh | Mô tả |
|---|---|
| `/tx` | Mở menu chính (dialog hoặc form Bedrock) |
| `/tx menu` | Giống `/tx` |
| `/tx taikhoan` | Xem số dư, thắng/thua, chuỗi thắng, lịch sử gần đây |
| `/tx dangky` | Mở GUI tạo tài khoản |
| `/tx dangky confirm` | Tạo tài khoản trực tiếp, không mở GUI |
| `/tx vaophong` | Vào phòng cược và mở GUI đặt cược |
| `/tx thoatphong` | Rời phòng; nếu đang có vé chưa có kết quả sẽ mất vé |
| `/tx cuoc tai 1000` | Đặt Tài 1.000 BlizCoin |
| `/tx cuoc xiu 1000` | Đặt Xỉu 1.000 BlizCoin |
| `/tx cuoc tai all` | Đặt Tài bằng số dư (giới hạn theo max) |
| `/tx doicuoc` | Mở GUI quy đổi |
| `/tx doicuoc vault nap` | Mở thẳng dialog nạp Vault |
| `/tx doicuoc diamond rut` | Mở thẳng dialog rút kim cương |
| `/tx nap vault 100` | Nạp 100 Vault → BlizCoin |
| `/tx rut vault 50` | Rút 50 Vault, trừ BlizCoin theo tỉ giá |
| `/tx nap playerpoints 10` | Nạp 10 PlayerPoints |
| `/tx nap diamond 2` | Nạp 2 kim cương |
| `/tx thechap` | Mở GUI thế chấp |
| `/tx thechap dat diamond 3` | Thế chấp 3 kim cương |
| `/tx thechap dat diamond all` | Thế chấp tất cả kim cương |
| `/tx thechap tra 1` | Chuộc hợp đồng #1 |
| `/tx thechap list` | Xem hợp đồng thế chấp |
| `/tx lichsu` | Lịch sử phiên toàn server |
| `/tx lichsu 2` | Trang 2 lịch sử |
| `/tx cau` | Xem cầu Tài/Xỉu |
| `/tx hu` | Xem hũ |
| `/tx title [on\|off]` | Bật/tắt title giữa màn hình |
| `/tx top` | Top 10 BlizCoin |
| `/tx help` | Trợ giúp trong game |

### Lưu ý đặt cược

- Mỗi người chỉ được **một vé** trong một phiên.
- Không thể đặt thêm hoặc đổi cửa.
- Chỉ đặt được trong giai đoạn `BETTING`.
- Cần có tài khoản và vào phòng (nếu `room.require: true`).
- Vé tối thiểu/tối đa theo `betting.min/max`.
- Người có `bliztaixiu.bypass.bet-limit` bỏ qua giới hạn max.

---

## Lệnh admin

Alias: `/txa`, `/btxa`.

| Lệnh | Mô tả |
|---|---|
| `/txa help` | Danh sách lệnh admin |
| `/txa reload` | Reload config/messages/currencies/collateral |
| `/txa save` | Ghi ngay accounts/sessions/collateral ra file |
| `/txa give <player> <amount>` | Cộng BlizCoin |
| `/txa take <player> <amount>` | Trừ BlizCoin |
| `/txa set <player> <amount>` | Đặt số dư |
| `/txa reset <player>` | Reset tài khoản (số dư theo cấu hình tân thủ) |
| `/txa hu info` | Xem hũ |
| `/txa hu set <amount>` | Đặt hũ |
| `/txa hu add <amount>` | Cộng hũ |
| `/txa hu take <amount>` | Trừ hũ |
| `/txa forceend` | Kết thúc phiên hiện tại và trả kết quả ngay |
| `/txa cancel` | Hủy phiên, hoàn tiền các vé đang hoạt động |
| `/txa start` | Bắt đầu phiên mới khi đang chờ/kết quả |
| `/txa stats <player>` | Thống kê chi tiết |
| `/txa room` | Danh sách người trong phòng |
| `/txa collateral <player>` | Xem hợp đồng thế chấp |
| `/txa collateral clear <player>` | Tịch thu toàn bộ tài sản thế chấp |
| `/txa cau set <d1> <d2> <d3>` | Ép kết quả xúc xắc cho phiên hiện tại hoặc phiên kế tiếp (vd: `3 3 3`) |
| `/txa cau info` | Xem cầu đang đặt |
| `/txa cau off` | Hủy cầu đang đặt |
| `/txa cau clear` | Xóa lịch sử cầu, reset cầu về 0 |

Admin có thể dùng từ console, trừ các lệnh yêu cầu người chơi trực tiếp.

### Đặt cầu

- `/txa cau set <d1> <d2> <d3>` ép xúc xắc của **phiên đang mở cược**; nếu đặt lúc phiên đã quay thì áp dụng cho **phiên kế tiếp**.
- Mỗi xúc xắc phải trong khoảng `1..session.dice.sides`.
- Đặt lại sẽ thay cầu cũ. Cầu được lưu xuống `sessions.yml`, dùng một lần rồi tự xóa.
- Thông báo đặt cầu **chỉ gửi cho người chạy lệnh** (admin/console); người chơi không thấy.
- `/txa cau off` hủy cầu chưa dùng.
- `/txa cau clear` xóa toàn bộ lịch sử phiên dùng để vẽ cầu; cầu trở về 0 (trống). Hũ, số dư và lịch sử cá nhân của người chơi **không bị ảnh hưởng**.

---

## Permission

| Permission | Default | Mô tả |
|---|---|---|
| `bliztaixiu.use` | true | Dùng `/tx` |
| `bliztaixiu.play` | true | Vào phòng/đặt cược |
| `bliztaixiu.account` | true | Tài khoản |
| `bliztaixiu.exchange` | true | Quy đổi |
| `bliztaixiu.collateral` | true | Thế chấp |
| `bliztaixiu.title` | true | Bật/tắt title |
| `bliztaixiu.admin` | op | Admin chung + `/txa` |
| `bliztaixiu.admin.reload` | op | Reload |
| `bliztaixiu.admin.economy` | op | Give/take/set/reset |
| `bliztaixiu.admin.jackpot` | op | Quản lý hũ |
| `bliztaixiu.admin.session` | op | Forceend/cancel/start |
| `bliztaixiu.admin.cau` | op | Đặt cầu / xem / hủy / xóa lịch sử cầu |
| `bliztaixiu.admin.stats` | op | Xem stats |
| `bliztaixiu.admin.collateral` | op | Quản lý thế chấp |
| `bliztaixiu.bypass.bet-limit` | false | Bỏ giới hạn cược |

### LuckPerms ví dụ

```text
/lp group default permission set bliztaixiu.use true
/lp group default permission set bliztaixiu.play true
/lp group default permission set bliztaixiu.account true
/lp group default permission set bliztaixiu.exchange true
/lp group default permission set bliztaixiu.collateral true

/lp group admin permission set bliztaixiu.admin true
```

EssentialsX dùng permission Bukkit chuẩn; có thể dùng LuckPerms/GroupManager để cấp các node trên.

---

## Tab completion

Có sẵn cho:
- Subcommand `/tx`, `/txa`.
- `tai/xiu` khi đặt cược.
- Tên currency khả dụng khi nạp/rút.
- Tên item được phép thế chấp.
- ID hợp đồng thế chấp.
- Tên người chơi trong lệnh admin.
- Số trang lịch sử/cầu.

Tiếp theo: [[Gameplay]]
