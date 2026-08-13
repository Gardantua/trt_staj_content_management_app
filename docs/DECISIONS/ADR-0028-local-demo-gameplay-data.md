# ADR-0028: Yerel demo gameplay verisini API üzerinden üretme

## Durum

Accepted

## Bağlam

Leaderboard ve gelecekteki yüzdelik sunumunu elle tek kullanıcıyla sınamak yeterli
örneklem üretmiyordu. Projede kalıcı kullanıcı profili yoktur; yerel kimlik UUID
header'ıyla temsil edilir. Test verisinin gameplay, ilk-tamamlama ve asenkron XP
kurallarını bozması kabul edilemez.

## Karar

- Sekiz sabit demo UUID ve hedef doğruluk oranı JSON manifestinde tutulur.
- Yerel PowerShell scripti doğru cevap anahtarını EDITOR API'sinden okur; attempt ve
  cevapları her demo UUID adına public gameplay API'sine gönderir.
- Script en fazla ilk üç yayımlanmış quizle sınırlıdır ve mevcut XP kaydı bulunan
  kullanıcıyı varsayılan olarak atlar.
- XP işlenmesi kısa süre kontrollü beklenir; ardından ADMIN API'siyle Redis
  leaderboard projeksiyonu yenilenir.
- Demo etiketleri yalnız geliştirme manifestidir; production kullanıcı profili
  veya görünen ad sözleşmesi değildir.

## Alternatifler

### PostgreSQL tablolarına doğrudan SQL yazmak

Daha az HTTP isteği üretirdi; fakat aggregate, idempotency, reward claim, Outbox ve
XP consumer akışlarını atlayarak gerçek davranışı temsil etmeyen veri oluştururdu.
Reddedildi.

### Uygulama açılışında otomatik veri üretmek

Elle komut gerektirmezdi; ancak her local başlangıcı yan etkili yapar ve geliştirici
verisini istemeden değiştirirdi. Açıkça çalıştırılan script seçildi.

## Sonuçlar

- Demo sıralama verisi gerçek application sınırlarından geçer ve tekrar üretilebilir.
- UUID'lerin adı uygulamada görünmez; profil özelliği hâlâ MVP dışıdır.
- Sıfır XP completion'ın ledger'a yazılmaması mevcut ayrı bir davranış farkıdır;
  script leaderboard örneklemi için pozitif skorlu kullanıcılar kullanır.

## Yeniden değerlendirme tetikleyicileri

- Kalıcı kullanıcı profili veya production kimlik sağlayıcısı eklendiğinde
- Test verisi temizleme/sıfırlama aracı gerektiğinde
- Yüzdelik sıralama için daha büyük veya belirli dağılımlı örneklem gerektiğinde
