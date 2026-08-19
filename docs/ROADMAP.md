# Geliştirme Yol Haritası - Kısa Referans

Her görevde yalnızca tek aşama uygulanır. Kullanıcı yeni aşamayı açıkça seçmeden
sonraki aşamaya, yeni teknolojiye veya MVP dışı özelliğe geçilmez.

## Tamamlanan ana aşamalar

- Aşama 0: Proje temeli, Spring Boot, PostgreSQL, Flyway, Docker ve CI.
- Aşama 1: Kimlik, roller, yetkilendirme ve geçici local/test actor adapter'ı.
- Aşama 2: İçerik, sezon, bölüm ve yayınlama.
- Aşama 3: Quiz authoring, sürümleme ve immutable yayın sürümü.
- Aşama 4: Server-authoritative gameplay, deadline, attempt ve idempotency.
- Aşama 4.1: Medya, erişilebilirlik ve ayarlanabilir süre.
- Aşama 5: PostgreSQL üzerinde XP ledger.
- Aşama 6: Transactional Outbox, RabbitMQ, Inbox, retry ve DLQ.
- Aşama 7: PostgreSQL leaderboard doğruluğu.
- Aşama 8: Redis leaderboard read model ve PostgreSQL fallback.
- Aşama 9: Trace, metric, rate limit, güvenlik taraması, yük ve disaster/recovery kontrolleri.
- Aşama 10A-10AA: Admin/kullanıcı web akışları, medya, quiz yazarlığı, PDF, keşif,
  demo veri ve yerel kullanıcı hesabı.
- Aşama 11: Leaderboard'un ayrı Spring Boot servisine güvenli ayrıştırılması,
  `xp.changed.v1`, bağımsız PostgreSQL/Redis ve özellik bayraklı geçiş.
- Aşama 12: Oracle Always Free hedefli tek sunucu production paketi, aynı-origin
  HTTPS, CSRF koruması, gerçek admin oturumu ve tek kullanımlık ilk admin oluşturma.
- Aşama 13A: Kullanıcı ve yönetici web arayüzlerinin tip kontrollü Türkçe–İngilizce
  sözlük, kalıcı dil tercihi, erişilebilir dil seçici ve yerelleştirilmiş PDF şablonuyla
  iki dilli hale getirilmesi.
- Aşama 13C: Film/dizi, sezon/bölüm ve quiz metinlerinin aynı domain kimliklerine bağlı
  İngilizce sunum projeksiyonları, admin çeviri alanları ve `Accept-Language` seçimi.
- Aşama 13D: Film ve dizi detaylarında editörün yönettiği, yalnız HTTPS resmî tabii
  içerik sayfalarını kabul eden ve güvenli yeni sekmede açılan izleme bağlantısı.

## Güncel ürün durumu

- Kullanıcı e-posta/şifre ile yerel hesap oluşturabilir; hesap yalnızca `USER` rolü alır.
- Admin ve kullanıcı webi ayrı giriş noktalarıyla aynı backend API'sini kullanır.
- Quizler dört seçenekli ve soru başına 30 saniyelik server deadline ile çalışır.
- İlk tamamlamada XP kazanılır; sonraki tamamlamalar alıştırmadır ve ek XP üretmez.
- İlk tamamlamada 0 puan alan kullanıcı da leaderboard'a tek ledger kaydıyla katılır.
- İçerik yönetimi, quiz keşfi, içerik araması, profil XP'si ve leaderboard akışları tamamlanmıştır.

## Uygulama sırası

```text
İçerik/quiz yayınla
 → Kullanıcı quiz keşfeder
 → Attempt başlatır
 → Cevap veya timeout gönderir
 → Attempt tamamlanır
 → XP Outbox/RabbitMQ ile işlenir
 → xp.changed.v1 ayrı leaderboard servisine gider
 → Leaderboard servis PostgreSQL/Redis'i üzerinden okunur
```

## Test beklentisi

- Domain kuralları için unit test.
- Repository, migration ve constraint'ler için PostgreSQL integration test.
- API ve yetki için API test.
- Duplicate, complete ve yarış durumları için concurrency/idempotency test.
- Broker kullanıldığında gerçek RabbitMQ Testcontainers testi.
- Frontend için strict TypeScript, Vitest ve production build.

## Sıradaki iş

Aşama 13D tamamlandı. Kullanıcı yeni dil dilimini seçerse sıradaki tek geliştirme işi
Aşama 13B'de kararlı hata kodu ve trace ID sözleşmesini koruyarak backend API,
authentication, authorization ve CSRF hata mesajlarına `Accept-Language` desteği
eklemektir. Oracle VM smoke testi ve ilk dış backup/restore provası ayrı operasyon
işi olarak açık kalır.

## Arşiv

[Ayrıntılı roadmap ve aşama kabul kriterleri](archive/2026-08-11/ROADMAP.md)
