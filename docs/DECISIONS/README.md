# Mimari Karar Kayıtları

Bu klasör, proje boyunca alınan kalıcı ve alternatifli teknik kararları tutar.

Her ADR şu bölümleri içermelidir:

- Durum
- Bağlam
- Karar
- Gerekçe
- Sonuçlar
- Yeniden değerlendirme tetikleyicileri

Dosya adlandırması:

```text
ADR-0001-kisa-karar-adi.md
```

Durum değerleri:

- Proposed
- Accepted
- Superseded
- Rejected

Mevcut kayıtlar:

- [ADR-0001: Modüler monolith ile başlama](ADR-0001-moduler-monolith.md)
- [ADR-0002: İş davranışını altyapıdan önce kanıtlama](ADR-0002-davranis-once-altyapi-sonra.md)
- [ADR-0003: Aşama 0 teknoloji temeli](ADR-0003-asama-0-teknoloji-temeli.md)
- [ADR-0004: RabbitMQ'nun hedef mimaride kullanılması](ADR-0004-rabbitmq-kurum-gereksinimi.md)
- [ADR-0005: Aşama 1 geçici kimlik yaklaşımı](ADR-0005-asama-1-gecici-kimlik.md)
- [ADR-0006: Redis'in öğrenme gereksinimi olarak kullanılması](ADR-0006-redis-ogrenme-gereksinimi.md)
- [ADR-0007: Aşama 2 content aggregate ve yayınlama sınırı](ADR-0007-asama-2-content-aggregate.md)
- [ADR-0008: Aşama 3 quiz aggregate ve değişmez yayın sürümü](ADR-0008-asama-3-quiz-surumleme.md)
- [ADR-0009: Aşama 4 sunucu otoriteli gameplay](ADR-0009-asama-4-gameplay.md)
- [ADR-0010: Erişilebilir medya ve kapsayıcı gameplay](ADR-0010-erisilebilir-medya-ve-gameplay.md)
- [ADR-0011: PostgreSQL üzerinde append-only XP işlem defteri](ADR-0011-postgresql-xp-ledger.md)
- [ADR-0012: Transactional Outbox ve RabbitMQ ile güvenilir XP üretimi](ADR-0012-transactional-outbox-rabbitmq.md)
- [ADR-0013: PostgreSQL leaderboard ürün ve sıralama kuralları](ADR-0013-postgresql-leaderboard-kurallari.md)
- [ADR-0014: Redis leaderboard read model ve fallback](ADR-0014-redis-leaderboard-read-model.md)
- [ADR-0031: Leaderboard mikroservisine güvenli ayrıştırma](ADR-0031-leaderboard-microservice-extraction.md)
- [ADR-0015: OpenTelemetry, Prometheus ve yerel gözlemlenebilirlik profili](ADR-0015-opentelemetry-prometheus-observability.md)
- [ADR-0016: Aşama 9 production hazırlığı koruma sınırları](ADR-0016-stage-9-production-readiness-guardrails.md)
- [ADR-0017: Admin web uygulaması ve yerel kimlik sınırı](ADR-0017-admin-web-frontend-siniri.md)
- [ADR-0018: Kullanıcı web kataloğu için ayrı giriş noktası](ADR-0018-user-web-catalog.md)
- [ADR-0019: Kullanıcı webinde sunucu otoriteli gameplay akışı](ADR-0019-user-gameplay-web-flow.md)
- [ADR-0020: Admin medya yükleme ve kapak bağlama akışı](ADR-0020-admin-media-and-cover-flow.md)
- [ADR-0021: Admin quiz kapsamı ve yeniden kullanılabilir medya kütüphanesi](ADR-0021-admin-quiz-scope-and-media-library.md)
- [ADR-0022: Dört seçenekli soru, soru başına süre ve cevap anahtarlı PDF](ADR-0022-four-option-question-timer-and-pdf.md)
- [ADR-0023: İçerik yönetimi ve güvenli kalıcı silme](ADR-0023-content-management-and-safe-hard-delete.md)
- [ADR-0024: Aktif quiz, quiz geçmişi ve XP koruma](ADR-0024-quiz-active-history-and-xp-retention.md)
- [ADR-0025: Sezon ve bölümlerin atomik toplu planla oluşturulması](ADR-0025-atomic-season-episode-plan.md)
- [ADR-0026: Aynı quizde yalnız ilk tamamlamanın XP kazandırması](ADR-0026-first-completion-xp.md)
- [ADR-0027: Yayımlanmış dizi hiyerarşisine yalnız sona ekleme](ADR-0027-published-series-additive-hierarchy.md)
- [ADR-0028: Yerel demo gameplay verisini API üzerinden üretme](ADR-0028-local-demo-gameplay-data.md)
- [ADR-0029: Yerel kullanıcı hesapları ve sunucu oturumu](ADR-0029-local-user-accounts-and-server-session.md)
- [ADR-0030: Tek kullanımlık şifre sıfırlama ve MailerSend SMTP](ADR-0030-password-reset-and-mailersend-smtp.md)
- [ADR-0032: Tek sunucu production dağıtımı ve ilk yönetici hesabı](ADR-0032-single-server-production-and-admin-bootstrap.md)
- [ADR-0033: Frontend arayüz yerelleştirmesi](ADR-0033-frontend-interface-localization.md)
