# XpService.java

Gerçek kaynak: `src/main/java/com/trt/contentengagement/gamification/application/XpService.java`

## Sınıfın görevi

XP kullanım senaryolarını sıraya koyar ve transaction sınırını belirler. Domain
nesnesini üretir, portlar üzerinden veri okur/yazar ve yeni kayıt oluştuğunda
Outbox ile audit yan etkilerini aynı transaction'a dahil eder.

## Bağımlılıklar

- `XpLedgerRepository`: XP ledger okuma/yazma portu.
- `CurrentActorProvider`: Güvenilir oturum sahibini verir.
- `AdminAuditLog`: Admin düzeltmesini denetim kaydına yazar.
- `QuizContentReferenceProvider`: Quiz'in ait olduğu içeriği bulur.
- `XpTransactionEventOutbox`: Yeni XP kaydını leaderboard olayına hazırlar.
- `Clock`: Test edilebilir sunucu zamanı verir.

## Metotlar

### `awardQuizCompletion(...)`

- Amaç: Tamamlanan attempt için geçerli XP işlemini idempotent kaydetmek.
- Girdi: Kullanıcı, quiz, attempt, ödül, politika ve tamamlanma zamanı.
- İşlem: Quiz'den content kimliğini bulur; domain factory ile aday işlem üretir;
  `appendIfAbsent` ile ekler veya mevcut kaydı okur.
- Güvenlik kuralı: Aynı attempt daha önce farklı XP ayrıntılarıyla kullanılmışsa
  `XP_SOURCE_CONFLICT` fırlatır.
- Yan etki: Yalnız gerçekten yeni satır oluştuysa Outbox olayı hazırlar.
- Transaction: Ledger ve Outbox birlikte commit olur veya birlikte geri alınır.

### `findAwardForAttempt(attemptId)`

- Amaç: Bir attempt'in XP işlemini bulmak.
- Çıktı: `XpTransaction`.
- Hata: Kayıt yoksa `XpTransactionNotFoundException`.
- Transaction: Salt okunur.

### `currentUserSummary()`

- Amaç: Oturum sahibinin ledger toplamını okumak.
- Kimlik: `userId` request'ten alınmaz; `CurrentActorProvider` verir.
- Çıktı: Toplam XP ve işlem sayısı.
- Transaction: Salt okunur.

### `replayLeaderboardEvents()`

- Amaç: Mevcut XP geçmişi için leaderboard olaylarını idempotent biçimde yeniden
  Outbox'a hazırlamak.
- Çıktı: Taranan transaction sayısı.
- Not: Büyük production verisinde tek seferlik tam tarama yerine batch/cursor
  gerekir; mevcut proje hacmi için sade yaklaşım seçilmiştir.

### `adjust(...)`

- Amaç: Özgün quiz kazancını değiştirmeden imzalı yeni düzeltme satırı eklemek.
- İşlem: Hedefi bulur; hedefin `QUIZ_COMPLETED` olduğunu doğrular; oturumdaki admin
  ve sunucu zamanı ile domain nesnesi üretir; idempotent kaydeder.
- Çakışma: Aynı reference key farklı düzeltme ayrıntılarıyla kullanılırsa
  `XP_REFERENCE_CONFLICT`.
- Yan etkiler: Yalnız yeni kayıtta Outbox ve audit kaydı oluşturur.
- Transaction: XP, Outbox ve audit birlikte kesinleşir.

### `sameCompletion(...)` ve `sameAdjustment(...)`

Repository duplicate nedeniyle mevcut satırı döndürdüğünde, bunun gerçekten aynı
komutun güvenli tekrarı mı yoksa aynı benzersiz anahtarın farklı içerikle kötüye
kullanımı mı olduğunu ayırır. Birincisi aynı sonucu döndürür; ikincisi hata verir.
