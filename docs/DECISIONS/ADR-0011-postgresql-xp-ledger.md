# ADR-0011: PostgreSQL üzerinde append-only XP işlem defteri

## Durum

Accepted

Quiz başına ödül uygunluğu ADR-0026 ile değiştirilmiştir; append-only ledger ve
attempt idempotency kararları geçerlidir.

## Bağlam

Tamamlanan bir quiz attempt'i kullanıcıya yalnız bir kez XP vermelidir. Aynı
complete isteği ağ tekrarıyla veya paralel olarak yeniden gelebilir. XP
düzeltmeleri geçmiş kazancı sessizce değiştirmemeli ve kim tarafından, neden
yapıldığı izlenebilmelidir. Aşama 5'te RabbitMQ henüz yoktur; çalışan gameplay
sonucu ile XP arasında kayıp oluşturacak bir dual-write sınırı kurulmamalıdır.

## Karar

- XP, `xp_transactions` tablosunda append-only işlem defteri olarak saklanır.
- `SCORE_MATCH_V1` politikasında tamamlanma XP'si sunucunun kesinleştirdiği
  skora eşittir. Sıfır puan da işlenmiş kaynağı temsil eden sıfır tutarlı bir
  ledger kaydı üretir.
- Her `QUIZ_COMPLETED` kaydı bir kaynak attempt'e bağlıdır. Kaynak attempt ve
  deterministik `QUIZ_ATTEMPT:<attemptId>` referansı veritabanında tekildir.
- Attempt'in tamamlanması ile XP kaydı aynı PostgreSQL transaction'ında
  gerçekleşir. Biri başarısız olursa ikisi de geri alınır.
- Eşzamanlı tekrarlar, `INSERT ... ON CONFLICT DO NOTHING` ve ardından mevcut
  kaydı okuma yaklaşımıyla aynı sonuca yönlendirilir.
- Kullanıcı XP özeti, imzalı ledger tutarlarının toplamı ve kayıt sayısıdır.
- Düzeltme yalnız `ADMIN` rolüyle yapılır. Özgün tamamlanma kaydı değiştirilmez;
  ona referans veren, sıfır olmayan pozitif veya negatif yeni
  `ADMIN_ADJUSTMENT` kaydı eklenir. Harici düzeltme referansı tekildir ve audit
  kaydı aynı transaction'da yazılır.
- Aşama 5'te broker veya process-içi event ile XP üretilmez. Outbox ve RabbitMQ
  sınırı Aşama 6'da, mevcut sonucu değiştirmeyen regresyon testleriyle eklenir.

## Gerekçe ve değerlendirilen alternatifler

- Yalnız kullanıcı toplamını güncellemek daha kısa bir şema olurdu; fakat
  geçmişi, düzeltme nedenini ve idempotency kaynağını kaybettirirdi.
- Spring event listener veya RabbitMQ ile hemen asenkron üretim, gameplay ile
  XP arasında teslimat boşluğu yaratırdı. Outbox henüz kurulmadığı için senkron
  tek transaction daha güvenilirdir.
- Sabit taban XP, bonus veya çarpan formülü eklenebilirdi. Ürün kuralı henüz
  bunu gerektirmediğinden sunucu skoruyla birebir eşleşme en sade, açıklanabilir
  ve sürümlenebilir başlangıç politikasıdır.

## Sonuçlar

- Ağ tekrarı ve paralel complete istekleri ikinci XP kazancı üretmez.
- Gameplay sonucu ile XP arasında broker olmadan atomik bütünlük sağlanır.
- Sıfır puanlı tamamlanma da işlendiğini kanıtlayan bir ledger kaydı bırakır.
- Yönetici düzeltmeleri toplam XP'yi sıfırın altına indirebilir; bu yetkili ve
  audit edilen bir operasyon kararıdır, otomatik alt sınır uygulanmaz.
- Uygulamanın public repository/API yüzeyi geçmiş kayıtları güncelleme veya
  silme işlemi sunmaz. Veritabanı süper kullanıcılarının operasyonel yetkileri
  ayrıca production erişim politikasıyla sınırlandırılmalıdır.
- Aşama 6'da senkron üretim asenkron tüketiciye taşınırken aynı attempt, politika
  ve toplam XP sonuçları korunmalıdır.

## Yeniden değerlendirme tetikleyicileri

- Ürün ekibi taban XP, bonus, seri veya zorluk çarpanı tanımlarsa
- Hileli/iptal attempt için otomatik geri alma politikası gerekirse
- Quiz dışındaki bir XP kaynağı eklenirse
- XP geçmişi için saklama, anonimleştirme veya hukukî silme gereksinimi oluşursa
- Aşama 6 Outbox/RabbitMQ transaction sınırını değiştirdiğinde
