# ADR-0015: OpenTelemetry, Prometheus ve yerel gözlemlenebilirlik profili

## Durum

Accepted

## Bağlam

Aşama 8 sonunda uygulama HTTP trace kimliği ile bazı iş sayaçlarına sahipti;
ancak standart span üretmiyor, trace backend'ine veri göndermiyor ve p95/p99
API gecikmesini dışarıdan izlenebilir biçimde sunmuyordu. İstemcinin
`X-Trace-Id` değeri ile gerçek dağıtık trace kimliğinin aynı MDC alanını
kullanması da loglarda gerçekte var olmayan bir trace devamlılığı izlenimi
oluşturabilirdi.

Gözlemlenebilirlik; log, metric ve trace sinyalleriyle çalışan sistemin iç
durumunu dışarıdan anlayabilmektir. Bu artım Aşama 9'un tamamı değil,
ölçüm altyapısının ilk doğrulanabilir sınırıdır.

## Karar

- Spring Boot 4.1'in yönettiği `spring-boot-starter-opentelemetry` ile HTTP
  istek span'leri ve OTLP trace export kullanılır.
- Varsayılan trace örnekleme oranı yüzde 10'dur. OTLP export varsayılan profilde
  kapalı, yalnız `observability` profilinde açıktır.
- `X-Trace-Id` geriye uyumlu istek korelasyon kimliği olarak
  `requestTraceId` MDC alanında kalır. OpenTelemetry'nin `traceId` ve `spanId`
  alanları üzerine yazılmaz.
- Outbox publish ve `quiz.completed` consumer işlemleri Micrometer
  `Observation` olarak ölçülür. Event trace kimliği yalnız high-cardinality
  trace niteliğidir; metric label'ı yapılmaz.
- Prometheus HTTP/JVM ve mevcut iş metriklerini `/actuator/prometheus`
  üzerinden çeker. Endpoint yalnız `observability` profilinde açılır.
- p95/p99 için HTTP histogramları ve 100 ms–2 s arası başlangıç SLO bucket'ları
  üretilir. Bunlar ürün SLO'su değil, ölçüm tabanıdır.
- Docker Compose `observability` profili OpenTelemetry Collector, Tempo,
  Prometheus ve provision edilmiş Grafana dashboard'unu yerelde çalıştırır.
- Prometheus ve Tempo verileri yerel geliştirme volume'larında sırasıyla yedi
  gün ve 24 saat tutulur; bunlar production retention kararı değildir.

## Değerlendirilen alternatifler

### OpenTelemetry Java Agent

Kod değişikliği az ve üçüncü taraf kütüphane kapsamı geniştir. Ancak bu öğrenme
projesinde Outbox/consumer iş sınırlarını adlandırmayı ve metric/span
cardinality kararını görünür kılmak için Spring Boot starter ve açık
`Observation` kullanımı seçildi. Production platformu merkezi agent zorunlu
kılarsa yeniden değerlendirilebilir.

### Yalnız log korelasyonu

Kurulumu daha küçüktür; fakat latency dağılımı, hata oranı ve span ilişkisini
sorgulanabilir hale getirmez. Aşama 9 kabul kriterini karşılamadığı için
reddedildi.

### Prometheus/Grafana'yı varsayılan Compose akışına eklemek

Tek komutu basitleştirirdi; fakat her backend geliştirme çalışmasında dört ek
servis ve kaynak tüketimi oluştururdu. Açık profil seçimi daha kontrollüdür.

## Sonuçlar

- HTTP istekleri gerçek OpenTelemetry trace/span kimliği üretir; loglar aynı
  kimlikleri taşır.
- Outbox publish ve consumer süre/sonuçları hem metric hem span üretmeye hazır
  adlandırılmış iş sınırlarıdır.
- Grafana dashboard'u istek hızı, 5xx oranı, p95/p99, mesajlaşma sonuçları,
  leaderboard fallback'i ve JVM heap kullanımını birlikte gösterir.
- Mevcut Outbox şeması yalnız trace ID saklar; W3C `traceparent` içindeki parent
  span bağlamını kalıcılaştırmaz. Bu nedenle HTTP → Outbox → consumer aynı
  dağıtık trace zinciri henüz tamamlanmış sayılmaz.
- Prometheus scrape endpoint'inin production erişimi ayrıca yönetim ağı,
  kimlik veya reverse proxy politikası gerektirir. Profil tek başına production
  güvenlik sınırı değildir.

## Yeniden değerlendirme tetikleyicileri

- Kurum merkezi OpenTelemetry agent/collector standardı bildirirse
- Gerçek trafik örnekleme oranını veya SLO bucket'larını yetersiz kılarsa
- Trace/metric maliyeti ve cardinality bütçesi belirlenirse
- Production metrik erişimi için mTLS, bearer token veya ayrı management ağı
  sözleşmesi kesinleşirse
- Outbox üzerinden tam W3C trace continuation uygulanırsa
