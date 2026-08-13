# Gamification destekleyici tipleri

Bu dosya, davranışı kısa olan yardımcı dosyaları gerçek paket sırasıyla açıklar.

## `application`

### `XpLedgerRepository`

Application katmanının XP saklama portudur. Service'in JDBC sınıfına bağlanmasını
engeller. Ekleme, kimlikle/attempt ile bulma, replay listesi ve özet sözleşmelerini
tanımlar; SQL içermez.

### `XpTransactionEventOutbox`

Yeni XP satırını aynı transaction içinde dayanıklı integration event'e hazırlama
portudur. Service RabbitMQ veya Outbox tablo ayrıntısını bilmez.

### `XpSummary`

Bir kullanıcı kimliği, toplam XP ve ledger işlem sayısını taşıyan immutable sonuçtur.

### `XpTransactionNotFoundException`

İstenen XP işlemi bulunamadığında transaction kimliğini koruyan application hatasıdır.

### `XpLeaderboardQuery`

Global/içerik leaderboard sorguları ile read-model yeniden kurma okumalarını tanımlar.
Gamification ledger'ından okuyan geçiş portudur; yeni ayrı servis devreye alınırken
yerel fallback olarak korunur.

### `XpLeaderboardResult`

Top N liderleri, mevcut kullanıcının satırı ve toplam katılımcı sayısını taşır.
Constructor listeyi kopyalayarak dışarıdan değiştirilmesini engeller.

### `RankedXpEntry`

Pozisyon, kullanıcı, toplam XP ve ilk XP zamanından oluşan tek sıralama satırıdır.

## `domain`

### `XpReason`

İşlemin quiz tamamlanmasından mı admin düzeltmesinden mi geldiğini sınırlar. Serbest
metin yerine enum kullanılması geçersiz nedenleri engeller.

### `XpPolicyVersion`

XP hesaplama sözleşmesini sürümler. `xpForCompletedQuiz` yalnız completion
politikalarında çalışır ve negatif skoru reddeder. Eski ledger satırlarının hangi
kuralla üretildiği böylece kaybolmaz.

### `GamificationRuleViolationException`

Domain kuralı ihlalinde hem insanın okuyacağı mesajı hem API'nin kararlı davranışa
çevirebileceği `errorCode` değerini taşır.
