# XpTransaction.java

Gerçek kaynak: `src/main/java/com/trt/contentengagement/gamification/domain/XpTransaction.java`

## Sınıfın görevi

Tek bir XP ledger gerçeğini temsil eden immutable record'dur. Oluşturulduktan sonra
alanları değişmez. Constructor, geçersiz bir XP işleminin domain içinde var olmasını
engeller.

## Alanların anlamı

- `id`: Ledger satırının kimliği.
- `userId`, `contentId`: XP'nin sahibi ve içerik kapsamı.
- `amount`: Pozitif, sıfır veya düzeltmede negatif olabilen imzalı tutar.
- `reason`, `policyVersion`: İşlemin nedeni ve hesaplama sözleşmesi.
- `referenceKey`: İdempotency için iş anlamlı benzersiz anahtar.
- `sourceAttemptId`: Quiz kazancının kaynak attempt'i.
- `relatedTransactionId`: Düzeltmenin bağlandığı özgün işlem.
- `createdBy`, `note`: Admin düzeltmesinin aktörü ve gerekçesi.
- `occurredAt`: Sunucunun kabul ettiği olay zamanı.

## Metotlar

### Compact constructor `XpTransaction { ... }`

Ortak zorunlu alanları doğrular, metinleri trim eder ve `reason` değerine göre quiz
tamamlama ya da admin düzeltme sözleşmesini uygular. Geçersiz durumda kararlı hata
kodlu `GamificationRuleViolationException` fırlatır.

### `forQuizCompletion(...)`

Quiz tamamlanmasından güvenli XP işlemi üretir. Politika ödül miktarını hesaplar,
`QUIZ_ATTEMPT:<attemptId>` referansını kurar ve admin düzeltmesine ait alanları boş
bırakır. Overload, eski varsayılan `SCORE_MATCH_V1` politikasına kısa yol sağlar.

### `adjustment(...)`

Özgün işlemi silmeden veya güncellemeden yeni `ADMIN_ADJUSTMENT` satırı üretir.
Admin kimliği, açıklama ve hedef işlem zorunludur.

### `validateQuizCompletion(...)`

Quiz XP'sinin negatif olmamasını, completion politikası kullanmasını, bir attempt'e
bağlanmasını ve admin alanları taşımamasını korur.

### `validateAdjustment(...)`

Düzeltmenin sıfır olmamasını, adjustment politikası kullanmasını, özgün işleme ve
admin aktörüne bağlanmasını korur.

### `requireText(...)`

Referans ve notun boş/yalnız boşluk olmamasını ve uzunluk sınırını uygular; normalize
edilmiş metni döndürür.
