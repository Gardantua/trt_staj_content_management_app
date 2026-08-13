# Mimari - Kısa Referans

## Ana yaklaşım

Çekirdek sistem modüler monolith olarak tek uygulamada kalır. Aşama 11'de yalnız
leaderboard, bağımsız ölçek ve servis sınırı öğrenme hedefiyle ayrı dağıtılabilir
bir Spring Boot servisine çıkarılmıştır. Monolith XP ledger'ın; leaderboard
servisi kendi PostgreSQL projeksiyonu ve Redis read model'inin sahibidir.

## Modüller

- `identity`: Kullanıcı, rol, kimlik ve oturum.
- `content`: İçerik, sezon, bölüm ve yayın durumu.
- `media`: Değişmez medya kimliği, dosya metadata'sı ve storage adapter'ı.
- `quiz`: Quiz, sürüm, soru, seçenek ve yayınlama.
- `gameplay`: Attempt, cevap, süre, durum geçişi ve skor.
- `gamification`: Append-only XP ledger ve XP özeti.
- `messaging`: Outbox, Inbox, integration event ve RabbitMQ adapter'ları.
- `leaderboard`: Monolith içindeki geçiş/fallback adapter'ı ve ayrı
  `leaderboard-service` içindeki PostgreSQL/Redis sıralama sahibi.
- `admin`: Yetkili yönetim yüzeyi ve audit koordinasyonu.
- `shared`: Ortak hata, trace ve teknik yardımcı sözleşmeleri.

## Katmanlar

```text
api → application → domain
                  ↘ ports → infrastructure
```

- `domain`: Saf iş kuralları, entity, value object ve durum geçişleri.
- `application`: Use case'ler ve transaction sınırları.
- `ports`: Repository ve dış sistem arayüzleri.
- `infrastructure`: PostgreSQL, Redis, RabbitMQ ve framework adapter'ları.
- `api`: Controller ile request/response modelleri.

Controller iş kuralı taşımaz. Domain modeli Spring/JPA/broker ayrıntılarına
bağımlı değildir. Modüller birbirlerinin repository, infrastructure paketi veya
tablolarına doğrudan erişmez; yayınlanmış port ve sözleşmeler kullanılır.

## Kritik akışlar

### Quiz çözme

```text
Kullanıcı
 → API/controller
 → gameplay use case
 → quiz sürümü ve attempt doğrulaması
 → PostgreSQL transaction
 → cevap/timeout/complete sonucu
```

Sunucu; süreyi, sıradaki soruyu, doğru cevabı ve skoru belirler. Aynı soru için
unique constraint, tekrar komutlar için idempotency ve yarışan işlemler için
transaction/lock davranışı birlikte kullanılır.

### XP ve mesajlaşma

```text
Attempt tamamlanır
 → attempt + Outbox aynı PostgreSQL transaction'ında kaydedilir
 → publisher RabbitMQ'ya gönderir
 → Inbox korumalı consumer XP ledger'a yazar
```

RabbitMQ at-least-once teslimat yaptığı için consumer duplicate mesajlara karşı
idempotenttir. Broker kapalıyken attempt ve Outbox kaydı kaybolmaz.

### Leaderboard

Monolith yeni XP transaction'ını ve `xp.changed.v1` Outbox olayını aynı transaction'da
yazar. Ayrı servis olayı idempotent tüketir, kendi PostgreSQL projeksiyonundan
deterministik global/içerik sıralaması üretir. Redis bu sonucun yeniden oluşturulabilir
read model'idir. Dış API monolith üzerinde sabit kalır ve özellik bayrağıyla servise
yönlenir; geçiş süresince eski yerel sorgu rollback yoludur.

## Veri ve güvenlik kuralları

- PostgreSQL kalıcı doğru kaynaktır.
- Redis verisi kaybedilebilir ve yeniden üretilebilir olmalıdır.
- Yayınlanmış quiz sürümü immutable kabul edilir.
- Doğru cevap quiz başlamadan kullanıcı DTO'suna girmez.
- Kullanıcı yalnız kendi attempt ve sonuçlarına erişir.
- Secret, token, doğru cevap ve kişisel veri loglanmaz.
- Medya dosyası ile PostgreSQL metadata yazımı atomik değildir; production sahipsiz dosya temizliği ayrıca ele alınmalıdır.

## Arşiv

[Ayrıntılı mimari belge](archive/2026-08-11/ARCHITECTURE.md)
