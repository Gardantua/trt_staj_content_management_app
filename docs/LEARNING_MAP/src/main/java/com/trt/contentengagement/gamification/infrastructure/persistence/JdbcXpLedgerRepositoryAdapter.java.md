# JdbcXpLedgerRepositoryAdapter.java

Gerçek kaynak:
`src/main/java/com/trt/contentengagement/gamification/infrastructure/persistence/JdbcXpLedgerRepositoryAdapter.java`

## Sınıfın görevi

Uygulama portlarını PostgreSQL/JDBC ile gerçekleştirir. SQL burada bulunur; domain ve
service JDBC bilmez. Hem XP ledger repository'sini hem geçiş dönemindeki yerel
leaderboard sorgu portunu uygular.

## Metotlar

### Constructor

Spring'in yapılandırdığı `JdbcTemplate` nesnesini alır. Bağlantı açma, parametre
bağlama ve result-set yönetiminin tekrarını azaltır.

### `appendIfAbsent(xpTransaction)`

`INSERT ... ON CONFLICT DO NOTHING` kullanır. Yeni satırsa onu geri okur. Unique
constraint'e çarparsa reference key veya source attempt üzerinden daha önceki satırı
okuyup döndürür. Böylece tekrar istek aynı sonuca yaklaşır. Çakışıp okunabilir satır
bulunamazsa veri bütünlüğü varsayımı bozulduğu için `IllegalStateException` verir.

### `findById(...)` ve `findBySourceAttemptId(...)`

Tek satırı kimlik veya kaynak attempt ile arar; sonuç yoksa boş `Optional` döndürür.

### `findAllForLeaderboardProjection()`

Replay için bütün ledger'ı olay zamanı ve kimlikle deterministik sırada okur.

### `summarize(userId)`

PostgreSQL'de `SUM(amount)` ve `COUNT(*)` hesaplar. Satır yoksa `COALESCE` toplamı
sıfır yapar.

### `findGlobal(...)` ve `findByContent(...)`

Ortak `findLeaderboard` metoduna global veya içerik filtresiyle gider.

### `findAllGlobal()`, `findRankedContentIds()`, `findAllByContent(...)`

Redis read modelini yeniden kurma ve yerel leaderboard için tam sıralama verisini
üretir. İçerik kimliklerini ledger'dan türetir.

### `findLeaderboard(...)`

SQL önce kullanıcı başına toplamı ve ilk XP zamanını hesaplar. Sonra
`totalXp DESC -> firstXpAt ASC -> userId ASC` düzeninde `ROW_NUMBER` üretir. Tek
sorguda hem Top N'i hem mevcut kullanıcının Top N dışındaki satırını seçer.

### `findAllLeaderboardEntries(...)`

Aynı sıralama kuralıyla limitsiz projeksiyon listesi üretir.

### Mapper ve yardımcı metotlar

`mapRankedRow` SQL satırını sıralama nesnesine, `mapTransaction` ledger satırını
domain nesnesine çevirir. `single`, `findByReferenceKey` ve `requireByReferenceKey`
tekrarlanan tek-satır okuma davranışını toplar.

## Veri bütünlüğü notu

Service seviyesindeki karşılaştırmalar anlamlı hata verir; veritabanı unique
constraint'leri ise iki uygulama işlemi aynı anda yarışsa bile ikinci fiziksel
kaydı engeller. İki koruma farklı riskleri çözer ve birlikte gereklidir.
