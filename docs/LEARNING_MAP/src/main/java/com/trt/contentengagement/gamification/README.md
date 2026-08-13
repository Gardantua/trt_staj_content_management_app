# Gamification modülü

Gerçek paket: `src/main/java/com/trt/contentengagement/gamification`

## Modülün sorumluluğu

Bu modül XP'yi değiştirilebilir bir kullanıcı toplamı olarak değil, sonuna yeni
kayıt eklenen bir işlem defteri olarak tutar. Bu yapıya **append-only ledger**
denir. Önceki kazanç değiştirilmez; gerekiyorsa ayrı bir düzeltme kaydı eklenir.

## Katmanlar

```text
HTTP isteği
  -> api / XpController
  -> application / XpService
  -> domain / XpTransaction + XpPolicyVersion
  -> application port / XpLedgerRepository
  -> infrastructure / JdbcXpLedgerRepositoryAdapter
  -> PostgreSQL xp_transactions
```

Yeni bir XP satırı oluşursa aynı transaction içinde `XpTransactionEventOutbox`
üzerinden `xp.changed.v1` olayı da hazırlanır. Bu olay daha sonra ayrı leaderboard
servisine taşınır.

## İki ana kullanıcı akışı

### Kullanıcı XP özetini okur

```text
GET /api/v1/me/xp
  -> XpController.currentUserSummary
  -> XpService.currentUserSummary
  -> CurrentActorProvider oturumdaki kullanıcıyı bulur
  -> XpLedgerRepository.summarize
  -> PostgreSQL SUM(amount) ve COUNT(*) hesaplar
  -> XpSummary döner
```

### Admin XP düzeltmesi ekler

```text
POST /api/v1/admin/xp-transactions/{id}/adjustments
  -> ADMIN rolü kontrol edilir
  -> request alanları doğrulanır
  -> XpService.adjust
  -> özgün quiz kazancı bulunur
  -> XpTransaction.adjustment domain kurallarını doğrular
  -> ledger'a yeni satır idempotent eklenir
  -> yeni satırsa Outbox olayı ve admin audit kaydı yazılır
  -> işlem tek PostgreSQL transaction'ında kesinleşir
```

## Önemli sınırlar

- Controller yalnız HTTP çevirisi ve yetki sınırıdır; XP kuralı taşımaz.
- Service kullanım senaryosunu ve transaction sınırını yönetir.
- Domain modeli geçerli bir XP işleminin nasıl görüneceğini belirler.
- Repository interface'i uygulama katmanını JDBC ayrıntısından ayırır.
- PostgreSQL unique kuralları tekrarlı veya yarışan yazımlara karşı son savunmadır.
- RabbitMQ veya Redis, XP ledger'ın kalıcı doğru kaynağı değildir.

## Alternatif ve trade-off

Kullanıcı tablosunda yalnız `total_xp` alanını artırmak daha kısa kod olurdu.
Ancak hangi attempt'in ne kadar XP ürettiğini, admin düzeltmesinin nedenini ve
duplicate isteğin daha önce işlenip işlenmediğini kaybettirirdi. Ledger daha fazla
satır ve sorgu maliyeti karşılığında audit ve idempotency sağlar.
