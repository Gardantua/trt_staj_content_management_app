# ADR-0031: Leaderboard mikroservisine güvenli ayrıştırma

## Durum

Accepted

## Bağlam

Leaderboard modülü PostgreSQL XP ledger'dan deterministik sonuç üretiyor ve Redis'i
yeniden kurulabilir read model olarak kullanıyordu. Modül sınırı korunmuş olsa da
aynı Spring Boot süreci ve aynı PostgreSQL veritabanı içinde çalışıyordu.

Leaderboard okumasının bağımsız ölçeklenmesi ve RabbitMQ tüketen ayrı servis
sınırının öğrenilmesi istendi. Ayrı servisin monolith `xp_transactions` tablosunu
okuması veri sahipliğini ihlal ederdi. XP ile doğrudan Redis güncellemek ise XP
transaction'ı ile cache arasında dual-write riski üretirdi.

## Karar

- Monolith, append-only XP ledger'ın kalıcı sahibi olmaya devam eder.
- Yeni bir XP satırı ile sürümlü `xp.changed` v1 integration event'i aynı PostgreSQL
  transaction'ında Outbox'a yazılır. Event kimliği XP transaction kimliğidir.
- Ayrı `leaderboard-service` uygulaması olayı kendi RabbitMQ kuyruğundan tüketir.
  Kendi PostgreSQL'indeki `leaderboard_xp_entries` tablosuna yazar; monolith
  tablolarına veya repository'lerine erişmez.
- `event_id` primary key ve `transaction_id` unique constraint, at-least-once
  teslimatta aynı XP değişikliğinin ikinci kez uygulanmasını engeller.
- Servis sıralamayı toplam XP azalan, ilk XP zamanı artan ve kullanıcı UUID'si
  artan düzeniyle kendi PostgreSQL projeksiyonundan üretir.
- Redis, servis PostgreSQL'inden generation bazlı yeniden oluşturulabilir read
  modeldir. Redis okunamazsa servis kendi PostgreSQL'ine düşer.
- Dış `GET /api/v1/leaderboards/**` sözleşmesi monolith üzerinde kalır. Monolith,
  `LEADERBOARD_REMOTE_ENABLED=true` olduğunda kimliği doğrulayıp servisin internal
  API'sine çağrı yapar. Özellik kapalıyken mevcut yerel modül rollback yoludur.
- Eski XP satırları için ADMIN yetkili `POST
  /api/v1/admin/leaderboards/replay-xp-events` endpoint'i idempotent Outbox olayları
  üretir. Tekrar çağrı event çoğaltmaz.

## Değerlendirilen alternatifler

### İki uygulamanın aynı XP tablosunu okuması

İlk geçişi kısaltırdı; ancak bağımsız veri sahipliği sağlamaz, migration ve sorgu
değişikliklerinde servisleri gizlice birbirine bağlardı.

### XP transaction'ından sonra Redis'i doğrudan artırmak

Daha az bileşen gerektirirdi; ancak PostgreSQL commit olup Redis yazımı başarısız
olduğunda veya mesaj tekrar geldiğinde tutarsızlık üretirdi.

### Eski leaderboard modülünü aynı anda silmek

Kod tekrarını hemen azaltırdı; fakat veri replay'i ve sonuç eşitliği görülmeden
rollback yolunu kaldırırdı. Güvenli geçiş için geçici ikili okuma yeteneği korundu.

## Sonuçlar

- Leaderboard 8082 portunda ayrı dağıtılabilir Spring Boot servisidir.
- Servisin PostgreSQL'i 5434, Redis'i 6381 yerel portundan çalışır.
- Quiz çözme ve XP kazanma, leaderboard servisi kapalıyken devam eder; Outbox olayı
  bekler ve servis döndüğünde yayımlanır.
- Sıralama kısa süre eventual consistent olabilir. Redis yenileme aralığı varsayılan
  beş saniyedir.
- Production'da internal API için ağ politikası ve servisler arası kimlik doğrulama
  ayrıca kesinleştirilmelidir. Mevcut internal endpoint yerel/öğrenme topolojisi
  içindir.
- Replay bütün mevcut ledger'ı tek işlemde tarar; mevcut proje hacmi için sadedir.
  Production hacmi büyürse cursor/batch tabanlı replay gerekir.

## Test kanıtı

- Servis PostgreSQL testi duplicate olayı, adjustment toplamını ve deterministik
  tie-break'i doğrular.
- Gerçek RabbitMQ Testcontainers testi aynı mesajın iki tesliminde tek projeksiyon
  satırı oluştuğunu doğrular.
- Gerçek Redis testi projeksiyonun servis PostgreSQL'inden yeniden kurulduğunu ve
  sorgunun Redis kaynağından döndüğünü doğrular.
- Monolith messaging testi XP satırıyla `xp.changed` Outbox olayının birlikte
  oluştuğunu ve replay'in ikinci olay üretmediğini doğrular.
