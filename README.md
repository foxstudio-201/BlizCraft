<p align="center">
  <img src="assets/logo.png" alt="BlizPocket logo" width="240">
</p>

<h1 align="center">BlizPocket</h1>

<p align="center">
  Plugin Tài Xỉu (Sic Bo) cho <b>Paper 26.2</b>, dùng tiền nội bộ <b>BlizCoin</b>,<br>
  hỗ trợ nhiều nguồn tiền, GUI Java Dialog, form Bedrock native, hũ tích lũy và thế chấp vật phẩm.
</p>

<p align="center">
  <b>Paper 26.2</b> &nbsp;·&nbsp;
  <b>Java 25</b> &nbsp;·&nbsp;
  <b>BlizCoin</b> &nbsp;·&nbsp;
  <b>GPL-3.0</b>
</p>

<p align="center">
  <a href="https://github.com/foxstudio-201/BlizCraft/wiki"><b>Wiki</b></a> &nbsp;·&nbsp;
  <a href="https://github.com/foxstudio-201/BlizCraft/releases"><b>Releases</b></a> &nbsp;·&nbsp;
  <a href="https://github.com/foxstudio-201/BlizCraft/issues"><b>Issues</b></a>
</p>

---

## Tính năng chính

### Phiên cược Tài Xỉu
- 3 xúc xắc, tổng `4-10` là **XỈU**, `11-17` là **TÀI**.
- Chu kỳ phiên: mở cược → khóa cược → quay xúc xắc → công bố kết quả → chờ phiên mới.
- Đếm ngược khóa cược bằng title `5 → 1`.
- Xúc xắc quay trực tiếp trên title bar.
- Bossbar hiển thị phiên, thời gian, tổng cược **Tài/Xỉu** và **Hũ**.
- Không có ai đặt cược vẫn quay xúc xắc và ghi lịch sử, không bỏ qua phiên.

### Luật cược
- Mỗi người chỉ được đặt **một vé** trong một phiên.
- Đã đặt Tài thì không đặt thêm/đổi sang Xỉu và ngược lại.
- Rời phòng hoặc thoát server khi vé chưa có kết quả sẽ **mất toàn bộ tiền cược**; tiền đi vào hũ.
- Người không ở trong phòng không nhận bossbar, title hay thông báo phiên.
- Thông báo vào/ra phòng có thể giới hạn trong phòng hoặc toàn server.

### Trả thưởng và hũ
- Tiền thua được chia theo cấu hình:
  - một phần vào **hũ tích lũy**,
  - một phần **lãi nhà cái** (có thể tắt),
  - phần còn lại chia cho người thắng theo tỉ lệ tiền đặt.
- Thắng luôn được cộng lời tối thiểu (mặc định 100% tiền cược). Nếu tiền người thua không đủ, nhà cái bù phần thiếu.
- Người đặt càng nhiều thì phần thắng càng lớn.
- Bộ ba hoặc tỉ lệ random cấu hình → **nhà cái thắng**, không ai được trả.
- Hũ có thể **nổ random**, chỉ chia cho người thắng theo tỉ lệ cược; người thua không nhận.
- Toàn bộ phép chia dùng phần dư chính xác, không làm mất tiền lẻ.

### Tài khoản và lịch sử
- Tạo tài khoản bằng GUI.
- Bật/tắt tiền tân thủ và cấu hình số dư ban đầu.
- Lưu số dư, thắng/thua/hòa, chuỗi thắng, thắng lớn nhất, lịch sử cá nhân.
- Lịch sử phiên toàn server, xem chi tiết từng phiên và xem **cầu Tài/Xỉu**.

### Nhiều loại tiền
- **BlizCoin**: tiền nội bộ dùng để đặt cược.
- **Vault / EssentialsX / CMI / economy provider khác**.
- **PlayerPoints**.
- **Cấp kinh nghiệm** (tuỳ chọn).
- **Vật phẩm** (kim cương, ngọc, vàng... cấu hình tỉ giá).
- **Custom currency** qua PlaceholderAPI + lệnh console.

Quy đổi hai chiều:
- Nạp ngoại tệ → BlizCoin.
- Rút BlizCoin → ngoại tệ, có chọn loại tiền, mốc sẵn, tất cả và số lượng tùy chỉnh.
- Toàn bộ dùng **Dialog GUI**, không dùng anvil.

### GUI
- GUI đặt cược dạng rương 54 ô, chia 2 bên Tài/Xỉu.
- Mức cược sẵn, tăng/giảm, x2, đặt lại, tùy chỉnh.
- Tài khoản, lịch sử, cầu, quy đổi, hũ, thế chấp, thoát phòng.
- Java: Paper **Dialog API 26.2**.
- Bedrock: form native qua **Cumulus / Floodgate**.
- Mọi thao tác chọn GUI/dialog đều có âm thanh click cấu hình được.

### Thế chấp vật phẩm
- Chỉ các vật phẩm nằm trong `collateral.allowed-items` mới được thế chấp.
- Nhận BlizCoin ngay theo tỉ giá.
- **Enchant làm tăng giá trị**: mỗi cấp enchant cộng thêm phần trăm cấu hình.
- Phải trả lãi và chuộc trước hạn.
- Quá hạn không chuộc: vật phẩm **mất vĩnh viễn**.
- Vật phẩm gốc (NBT/enchant) được lưu lại để trả đúng khi chuộc.

### Dữ liệu bền vững
- Tài khoản: `accounts.yml`.
- Lịch sử phiên + hũ: `sessions.yml`.
- Thế chấp + item: `collateral.yml`.
- Log giao dịch: `transactions.log`.
- Có hàng đợi ghi tránh ghi đè dữ liệu cũ; có lệnh `/txa save` để ghi ngay.

### Quản trị
- Reload nóng config/messages/currencies/collateral.
- Give/take/set/reset số dư.
- Quản lý hũ.
- Kết thúc/hủy/bắt đầu phiên.
- Xem thống kê, danh sách phòng, hợp đồng thế chấp.

---

## Yêu cầu

| Thành phần | Bắt buộc | Ghi chú |
|---|---|---|
| Paper 26.2 | Có | Test với build 135 |
| Java 25 | Có | Server và build |
| Vault + economy provider | Khuyến nghị | EssentialsX đã test |
| PlayerPoints | Tuỳ chọn | Hỗ trợ quy đổi point |
| PlaceholderAPI | Tuỳ chọn | Cho custom currency |
| Geyser + Floodgate | Tuỳ chọn | Cho người chơi Bedrock |
| ViaVersion | Tuỳ chọn | Khi Geyser yêu cầu protocol mới hơn server |

---

## Cài đặt nhanh

1. Tải `BlizPocket-1.0.0.jar` từ repo hoặc build từ source.
2. Bỏ vào `plugins/`.
3. Cài các dependency cần dùng (Vault/EssentialsX, PlayerPoints, Geyser/Floodgate...).
4. Khởi động server.
5. Cấp quyền hoặc `op` cho tài khoản admin.
6. Vào game:
   - `/tx dangky` — tạo tài khoản.
   - `/tx vaophong` — vào phòng và mở GUI đặt cược.
   - `/tx` — menu chính.

Cấu hình được tạo tại:

```text
plugins/BlizPocket/config.yml
plugins/BlizPocket/messages.yml
```

Dữ liệu:

```text
plugins/BlizPocket/accounts.yml
plugins/BlizPocket/sessions.yml
plugins/BlizPocket/collateral.yml
plugins/BlizPocket/transactions.log
```

---

## Build từ source

Yêu cầu:
- JDK 25
- Maven 3.9+

```bash
mvn clean package
```

Jar đầu ra:

```text
target/BlizPocket-1.0.0.jar
```

Repo không chứa thư viện server, jar Paper hay dữ liệu chạy thử.

---

## Lệnh

### Người chơi

| Lệnh | Mô tả |
|---|---|
| `/tx` | Mở menu chính |
| `/tx taikhoan` | Xem tài khoản/thống kê |
| `/tx dangky [confirm]` | Tạo tài khoản |
| `/tx vaophong` | Vào phòng cược và mở GUI |
| `/tx thoatphong` | Rời phòng cược |
| `/tx cuoc <tai\|xiu> <so_tien\|all>` | Đặt cược nhanh |
| `/tx doicuoc [loai_tien] [nap\|rut]` | Mở GUI quy đổi / mở thẳng bảng số lượng |
| `/tx nap <loai> <so_luong\|all>` | Nạp ngoại tệ thành BlizCoin |
| `/tx rut <loai> <so_luong\|all>` | Rút BlizCoin ra ngoại tệ |
| `/tx thechap` | Mở GUI thế chấp |
| `/tx thechap dat <item> <so_luong\|all>` | Thế chấp item |
| `/tx thechap tra <id>` | Chuộc hợp đồng |
| `/tx thechap list` | Danh sách hợp đồng |
| `/tx lichsu [trang]` | Lịch sử phiên |
| `/tx cau [trang]` | Xem cầu |
| `/tx hu` | Xem hũ |
| `/tx top` | Top BlizCoin |
| `/tx help` | Trợ giúp |

Alias: `/tx`, `/btx`, `/bliztaixiu`, `/bp`, `/blizpocket`.

### Admin

| Lệnh | Mô tả |
|---|---|
| `/txa reload` | Reload config/messages/currencies/collateral |
| `/txa save` | Ghi ngay toàn bộ dữ liệu |
| `/txa give <player> <amount>` | Cộng BlizCoin |
| `/txa take <player> <amount>` | Trừ BlizCoin |
| `/txa set <player> <amount>` | Đặt số dư |
| `/txa reset <player>` | Reset tài khoản |
| `/txa hu info` | Xem hũ |
| `/txa hu set\|add\|take <amount>` | Sửa hũ |
| `/txa forceend` | Kết thúc phiên ngay |
| `/txa cancel` | Hủy phiên + hoàn tiền vé hợp lệ |
| `/txa start` | Bắt đầu phiên mới |
| `/txa stats <player>` | Thống kê chi tiết |
| `/txa room` | Danh sách người trong phòng |
| `/txa collateral <player>` | Xem hợp đồng thế chấp |
| `/txa collateral clear <player>` | Tịch thu toàn bộ tài sản thế chấp |
| `/txa help` | Trợ giúp admin |

---

## Quyền

| Permission | Mặc định | Mô tả |
|---|---|---|
| `bliztaixiu.use` | true | Dùng `/tx` cơ bản |
| `bliztaixiu.play` | true | Vào phòng và đặt cược |
| `bliztaixiu.account` | true | Tạo/xem tài khoản |
| `bliztaixiu.exchange` | true | Quy đổi tiền |
| `bliztaixiu.collateral` | true | Thế chấp/chuộc item |
| `bliztaixiu.admin` | op | Quyền admin chung |
| `bliztaixiu.admin.reload` | op | Reload |
| `bliztaixiu.admin.economy` | op | Sửa số dư |
| `bliztaixiu.admin.jackpot` | op | Quản lý hũ |
| `bliztaixiu.admin.session` | op | Điều khiển phiên |
| `bliztaixiu.admin.stats` | op | Xem thống kê |
| `bliztaixiu.admin.collateral` | op | Quản lý thế chấp |
| `bliztaixiu.bypass.bet-limit` | false | Bỏ giới hạn cược |

Có thể dùng LuckPerms, GroupManager hoặc permission plugin tương thích Bukkit/EssentialsX.

---

## Cấu hình nhanh

Các mục quan trọng trong `config.yml`:

```yaml
session:
  betting-seconds: 45
  lock-seconds: 5
  roll-seconds: 4
  result-seconds: 6
  next-round-delay-seconds: 5

betting:
  min: 100
  max: 1000000
  default-amount: 1000
  presets: [100, 500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000]

account:
  starting-balance-enabled: true
  starting-balance: 1000

room:
  announce-join-leave: true
  announce-scope: room
  forfeit-bet-on-leave: true

payout:
  minimum-win-profit-percent: 100.0
  house-covers-shortfall: true

bossbar:
  compact-numbers: true

collateral:
  duration-hours: 24.0
  interest-percent: 10.0
  enchant-bonus-percent-per-level: 10.0
```

Sau khi sửa:

```text
/txa reload
```

Xem giải thích đầy đủ từng key tại [Configuration](https://github.com/foxstudio-201/BlizCraft/wiki/Configuration).

---

## Bedrock

Server đã test với:
- Geyser-Spigot `2.12.1`
- Floodgate `2.2.5`
- ViaVersion `5.12.3`

Cấu hình Geyser:

```yaml
bedrock:
  port: 19132

remote:
  auth-type: floodgate
```

- Java: `IP:25565`
- Bedrock: `IP:19132`
- Mở Internet cần forward **TCP + UDP 19132** (Geyser 2.12 dùng NetherNet).
- Plugin tự nhận người chơi Floodgate và dùng form Bedrock native.

Chi tiết: [Bedrock Setup](https://github.com/foxstudio-201/BlizCraft/wiki/Bedrock-Setup).

---

## Release & Update

- Workflow `.github/workflows/release.yml` build Maven và tạo GitHub Release theo tag/version.
- Phát hành bằng tag:
  ```bash
  git tag v1.0.1
  git push origin v1.0.1
  ```
- Hoặc chạy tay: **Actions → Release → Run workflow**.
- Plugin kiểm tra bản mới **một lần khi server khởi động xong** bằng `ServerLoadEvent` (`STARTUP`), không polling định kỳ. Restart server sẽ kiểm tra lại.
- Cấu hình:
  ```yaml
  update-checker:
    enabled: true
    github-repo: "foxstudio-201/BlizCraft"
    notify-admins: true
    timeout-seconds: 10
  ```

Chi tiết: [Release & Updates](https://github.com/foxstudio-201/BlizCraft/wiki/Release-and-Updates).

## Wiki

> GitHub Wiki cần trang đầu tiên được tạo thủ công trên web. Bản Markdown đầy đủ luôn có trong thư mục [`wiki/`](wiki/Home.md). Sau khi tạo trang wiki đầu tiên, workflow `Publish Wiki` sẽ đồng bộ toàn bộ nội dung.

- [Home](https://github.com/foxstudio-201/BlizCraft/wiki)
- [Installation](https://github.com/foxstudio-201/BlizCraft/wiki/Installation)
- [Configuration](https://github.com/foxstudio-201/BlizCraft/wiki/Configuration)
- [Commands & Permissions](https://github.com/foxstudio-201/BlizCraft/wiki/Commands-and-Permissions)
- [Gameplay](https://github.com/foxstudio-201/BlizCraft/wiki/Gameplay)
- [GUI Guide](https://github.com/foxstudio-201/BlizCraft/wiki/GUI-Guide)
- [Currency & Exchange](https://github.com/foxstudio-201/BlizCraft/wiki/Currency-and-Exchange)
- [Collateral](https://github.com/foxstudio-201/BlizCraft/wiki/Collateral)
- [Data & Backups](https://github.com/foxstudio-201/BlizCraft/wiki/Data-and-Backups)
- [Bedrock Setup](https://github.com/foxstudio-201/BlizCraft/wiki/Bedrock-Setup)
- [Release & Updates](https://github.com/foxstudio-201/BlizCraft/wiki/Release-and-Updates)
- [Troubleshooting](https://github.com/foxstudio-201/BlizCraft/wiki/Troubleshooting)

---

## Cấu trúc repo

```text
src/main/java/com/blizcraft/blizpocket/
  bedrock/       Form Bedrock (Cumulus/Floodgate)
  collateral/    Thế chấp item
  command/       /tx và /txa
  config/        Settings/Messages
  data/          Account/Session/DataStore
  economy/       Vault/PlayerPoints/Item/Custom/PAPI
  game/          GameManager/Payout/Jackpot
  gui/           Inventory GUI + Java Dialog
  listener/      Player/GUI listener
  util/          Text/Number/Item/Time
src/main/resources/
  config.yml
  messages.yml
  plugin.yml
```

---

## License & Credits

- License: **GNU GPL-3.0** (xem `LICENSE`).
- Paper API, Adventure, Vault, PlayerPoints, Floodgate, Cumulus, Geyser.
- Tác giả: foxstudio-201 / BlizCraft.

Nếu gặp lỗi, tạo Issue kèm:
- phiên bản Paper/Java,
- log `latest.log`,
- nội dung `config.yml`/`messages.yml` liên quan (che dữ liệu nhạy cảm),
- plugin đi kèm (Vault/Essentials/PlayerPoints/Geyser/Floodgate).
