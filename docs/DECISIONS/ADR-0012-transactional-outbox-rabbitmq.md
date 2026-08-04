# ADR-0012: Transactional Outbox ve RabbitMQ ile güvenilir XP üretimi

## Durum

Accepted

## Bağlam

Aşama 5'te quiz attempt'i ile XP aynı PostgreSQL transaction'ında senkron
kesinleşiyordu. RabbitMQ eklendiğinde attempt'i veritabanına yazıp olayı ayrıca
broker'a göndermek iki bağımsız yazma oluşturur. Veritabanı başarılı, broker
yazımı başarısız olursa XP olayı kaybolabilir; tersi durumda henüz kesinleşmemiş
bir sonuç yayımlanabilir. Broker'ın tekrar teslimat yapabilmesi de aynı attempt
için ikinci XP riskini doğurur.

## Karar

- Gameplay'in process-içi `QuizAttemptCompleted` domain olayı ile broker'a
  taşınan `quiz.completed` integration event'i ayrıdır. Dış sözleşme sürümü
  `v1`'dir.
- Attempt sonucu ve `outbox_events` satırı aynı PostgreSQL transaction'ında
  yazılır. RabbitMQ bu transaction'ın doğru kaynağı değildir.
- Event kimliği attempt kimliğinden deterministik üretilir. Aynı attempt yeniden
  işlendiğinde sabit iş alanları karşılaştırılır ve ilk olay zamanı korunur.
- Zamanlanmış publisher bekleyen satırları `FOR UPDATE SKIP LOCKED` ile küçük
  gruplar halinde kilitler, kalıcı RabbitMQ mesajı yollar ve publisher confirm
  aldıktan sonra satırı yayımlandı olarak işaretler. Hatalar artan gecikmeyle
  yeniden denenir.
- Dayanıklı direct exchange, XP queue'su ve dead-letter queue kullanılır.
  Teslimat garantisi **at-least-once**'dır; aynı mesaj birden fazla kez gelebilir.
- XP consumer'ı, `(consumer_name, event_id)` tekilliğine sahip Inbox kaydını ve
  XP ledger yazımını aynı PostgreSQL transaction'ında yapar. XP yazımı başarısız
  olursa Inbox claim'i de geri alınır. XP ledger kaynak tekilliği ikinci savunma
  hattıdır.
- Listener üç denemeyi artan gecikmeyle yapar; kalıcı hata mesajı DLQ'ya gider.
- Event ID, tür, sürüm ve trace ID mesaj header'larında taşınır. Publish,
  publish-failure, processed, duplicate ve listener-failure sayaçları Micrometer
  üzerinden sunulur.
- `SCORE_MATCH_V1` nedeniyle complete/son-answer yanıtındaki `earnedXp`, kesin
  sunucu skoruna eşit beklenen kazancı hemen gösterir. `/api/v1/me/xp` tüketici
  çalışana kadar kısa süreli eski değer döndürebilir.

## Değerlendirilen alternatifler

### PostgreSQL ve RabbitMQ'ya doğrudan çift yazma

Dağıtık transaction olmadan iki yazının atomikliği sağlanamadığı ve olay kaybı
oluşturabileceği için reddedildi.

### XP'yi senkron bırakıp yalnız bildirim olayı yayımlama

Daha basittir ancak Aşama 6'nın asenkron yan etki ve idempotent consumer öğrenme
hedefini karşılamaz. Aşama 5 testleriyle kanıtlanan XP sonucu regresyon testiyle
korunarak asenkron modele geçildi.

### Dağıtık transaction veya “exactly once” iddiası

Karmaşıklığı ve operasyon maliyeti MVP ihtiyacını aşar. RabbitMQ'nun doğal
tekrar teslimatına uygun at-least-once + idempotent consumer yaklaşımı seçildi.

## Sonuçlar

- RabbitMQ kapalıyken quiz tamamlanır ve olay PostgreSQL'de bekler; broker geri
  geldiğinde yayımlanabilir.
- Consumer hatası kesinleşmiş attempt sonucunu geri alamaz.
- XP özeti artık eventually consistent'tır; istemci kısa gecikmeye toleranslı
  olmalıdır.
- Tekrar teslimat Inbox ve XP unique constraint'leri sayesinde güvenlidir.
- DLQ'daki mesajlar için production replay/runbook politikası ayrıca
  belirlenmelidir; bu aşama otomatik replay eklemez.
- Publisher confirm beklerken Outbox satır kilidi tutulur. MVP hacminde kabul
  edilmiştir; throughput ve publisher gecikmesi ölçülmelidir.
- Test profilinde RabbitMQ health göstergesi kapalıdır; gerçek RabbitMQ
  entegrasyonu ayrı Testcontainers testleriyle çalışır. Normal profilde broker
  health bilgisi görünür kalır.

## Yeniden değerlendirme tetikleyicileri

- Outbox birikimi veya yayın gecikmesi kabul edilen SLA'yı aşarsa
- Event türü ve consumer sayısı önemli ölçüde büyürse
- Kurum event şema kayıt sistemi veya farklı broker standardı getirirse
- DLQ replay, Outbox/Inbox retention ve silme politikaları operasyonel SLA'ya
  bağlanırsa
- Publisher satır kilidi throughput darboğazına dönüşürse
