# XpController.java

Gerçek kaynak: `src/main/java/com/trt/contentengagement/gamification/api/XpController.java`

## Sınıfın görevi

HTTP dünyasını gamification uygulama katmanına çevirir. URL, request body, rol
kontrolü ve response şekli burada bulunur; XP iş kuralları `XpService` ve domain
nesnelerindedir.

## Üstten alta açıklama

### Constructor `XpController(XpService xpService)`

- Spring'in oluşturduğu `XpService` bağımlılığını alır ve saklar.
- Controller'ın repository veya JDBC'ye doğrudan gitmesini engeller.

### 1. `currentUserSummary()`

- HTTP karşılığı: `GET /api/v1/me/xp`.
- Amaç: Oturum açmış kullanıcının toplam XP'sini ve işlem sayısını döndürmek.
- Girdi: Metot parametresi yoktur; kullanıcı kimliği güvenilir oturum bilgisinden
  service içinde alınır.
- Çıktı: `XpSummary(userId, totalXp, transactionCount)`.
- Hata davranışı: Kullanıcı kimliği kurulamazsa güvenlik/kimlik katmanı isteği
  reddeder; veritabanı hatası genel hata sözleşmesine çevrilir.
- Akış: `XpService.currentUserSummary()` çağrılır.

Yalancı kod:

```text
oturumdaki kullanıcının XP özetini service'ten iste
özeti HTTP cevabı olarak döndür
```

### 2. `adjust(transactionId, request)`

- HTTP karşılığı: `POST /api/v1/admin/xp-transactions/{transactionId}/adjustments`.
- Yetki: Yalnız `ADMIN`; `@PreAuthorize` service çağrısından önce kontrol edilir.
- Girdi: Düzeltilecek özgün XP transaction kimliği ile tutar, benzersiz referans
  anahtarı ve açıklama.
- Doğrulama: Referans boş olamaz ve en fazla 150; not boş olamaz ve en fazla 500
  karakterdir.
- Çıktı: Kaydedilmiş domain nesnesinin dış API için daraltılmış hali.
- Hata davranışı: Hedef yoksa not-found; hedef özgün quiz kazancı değilse domain
  hatası; aynı referans farklı içerikle kullanılmışsa conflict davranışı oluşur.
- Akış: `XpService.adjust(...)` çağrılır, sonuç `XpTransactionResponse.from(...)`
  ile API modeline çevrilir.

### `AdjustmentRequest`

İstemciden kabul edilen alanların sözleşmesidir. `@Valid`, controller metodu
çalışmadan önce annotation kurallarını uygular. İş kuralının tamamı değildir;
örneğin tutarın sıfır olamaması domain tarafından korunur.

### `XpTransactionResponse.from(transaction)`

Domain nesnesini dış API cevabına çevirir. `contentId`, `sourceAttemptId`, admin
kimliği ve iç not gibi dışarı açılması gerekmeyen alanları response'a koymaz.
