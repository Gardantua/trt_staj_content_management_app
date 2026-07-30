# Proje Çalışma Kuralları

## Önce okunacak dosyalar

Her görevde uygulama değişikliği yapmadan önce sırasıyla şunları oku:

1. `README.md`
2. `docs/PROJECT_BRIEF.md`
3. `docs/ARCHITECTURE.md`
4. `docs/ROADMAP.md`
5. `docs/CURRENT_STATE.md`
6. Görevle ilgili `docs/DECISIONS/` kayıtları

## Kapsam disiplini

- Bir görevde yalnızca roadmap'teki tek aşamayı uygula.
- Kullanıcı istemeden bir sonraki aşamaya geçme.
- Eksik, taşınmış veya çalışma alanında bulunamayan bir dosyayı kullanıcıdan
  açık izin almadan yeniden oluşturma.
- Canlı TV, eğitim modu, sosyal özellikler ve yapay zekâ üretimini MVP'ye ekleme.
- Başlangıçta mikroservis oluşturma; modüler monolith sınırlarını koru.
- RabbitMQ ve Redis'i çekirdek quiz akışı tamamlanmadan ekleme.
- Teknoloji eklemek için gerçek bir problem ve kabul kriteri bulunmalıdır.

## Öğretici çalışma biçimi

Bu proje kullanıcının backend sistemine bütünüyle hâkim olmasını hedefler.
Değişiklik yapan ajan:

- Uygulamadan önce çözülen problemi ve dokunulan katmanları kısa biçimde açıklar.
- Yeni kavramları ilk kullanımda tanımlar; kullanıcının bildiğini varsaymaz.
- Önemli teknoloji ve mimari seçimlerinde en az bir makul alternatifi ve
  trade-off'u belirtir.
- Büyük kod yığınları yerine çalıştırılabilir, doğrulanabilir küçük adımlar
  kullanır.
- Yazılan her önemli sınıfın, metodun, yapılandırmanın ve kod bloğunun amacını;
  aldığı girdiyi, ürettiği çıktıyı, kullandığı bağımlılıkları, hata davranışını
  ve sistem akışındaki yerini açıklar.
- Yeni bir aşamaya geçmeden önce tamamlanan aşamadaki kodun nasıl çalıştığını,
  hangi iş kuralını uyguladığını ve ilgili testlerin neyi kanıtladığını
  kullanıcıya anlaşılır biçimde özetler.
- İş kuralının hangi testle korunduğunu açıkça eşleştirir.
- Çalıştırılan komutun amacını ve test sonucunun neyi kanıtladığını anlatır.
- Kullanıcı istemeden bir sonraki aşamayı veya ek teknolojiyi uygulamaz.

Kod açıklaması, kodun her satırını tekrar etmek değildir. Özellikle domain
kuralları, transaction sınırları, veri bütünlüğü, idempotency, güvenlik ve hata
davranışı öğretilmelidir. Açıklama sonunda kullanıcı kodun temel akışını kendi
cümleleriyle takip edebilecek durumda olmalıdır.

## Mimari kurallar

- İş kuralları controller içine yazılmaz.
- Domain modeli Spring, JPA veya broker ayrıntılarına bağımlı olmamalıdır.
- Modüller birbirlerinin tablolarına doğrudan erişmemelidir.
- Veritabanı şeması yalnız migration dosyalarıyla değiştirilmelidir.
- PostgreSQL kalıcı doğru kaynaktır.
- Redis verisi kaybedilebilir ve yeniden üretilebilir olmalıdır.
- Doğru cevap bilgisi quiz başlamadan istemciye gönderilmemelidir.
- Skor, süre ve attempt durumu sunucu tarafından belirlenmelidir.
- Tekrar gönderilebilecek komutlarda idempotency düşünülmelidir.
- Önemli veri bütünlüğü kuralları yalnız uygulama koduna değil, uygun
  veritabanı constraint'lerine de yazılmalıdır.

## Kod kalitesi

- Değişken, parametre, metot, sınıf ve dosya adlarını yaptığı işi anlatacak
  kadar açık ve domain diline uygun seç.
- `x`, `data`, `temp`, `obj`, `value` gibi bağlamı gizleyen genel adları, anlamın
  gerçekten açık olduğu çok dar kapsamlar dışında kullanma. Örneğin
  `remainingTimeInSeconds`, `quizAttempt` ve `submittedAnswer` gibi isimleri
  tercih et.
- Küçük, tek sorumluluklu sınıf ve fonksiyonlar kullan.
- Gereksiz generic abstraction, base service ve premature pattern üretme.
- Public API ve event sözleşmelerini sürümlendir.
- Hata yanıtlarında kararlı bir hata kodu ve trace ID kullan.
- Secret, token, doğru cevap ve kişisel verileri loglama.

## Görsel ve belge işleme

- PDF veya belge sayfalarını resme dönüştürme, OCR çalıştırma, görsel üretme,
  görsel düzenleme ya da sayfaları görsel olarak inceleme işlemlerinden önce
  kullanıcıdan açık izin al.
- Kullanıcı yalnız Markdown'dan PDF'e basit dönüşüm istediyse, görsel render ve
  sayfa incelemesini işin doğal devamı sayma; ayrıca izin iste.
- İzin verilmediyse yalnız istenen metin/dosya işlemini yap ve görsel kalite
  kontrolünün yapılmadığını açıkça belirt.

## Test beklentisi

Her özellik için riskine uygun test yaz:

- Domain kuralları için unit test
- Repository ve migration için PostgreSQL integration test
- API doğrulaması ve yetki için API test
- Duplicate cevap ve complete işlemleri için concurrency/idempotency test
- Broker eklendiğinde gerçek broker ile Testcontainers testi

Test geçmeden görev tamamlanmış sayılmaz.

## Komutlar

Sistemde kurulu global Maven yerine proje içindeki wrapper'ı kullan:

```text
./mvnw test
./mvnw verify
docker compose up -d
```

Windows ortamında:

```text
.\mvnw.cmd test
.\mvnw.cmd verify
docker compose up -d --wait
```

Uygulama varsayılan olarak `8081`, yerel PostgreSQL ise host üzerinde `5433`
portunu kullanır.

## Dokümantasyon ve teslim

Her anlamlı görev sonunda `docs/CURRENT_STATE.md` içinde:

- Tamamlanan işler
- Çalıştırılan testler ve sonuçları
- Alınan kararlar
- Öğrenilen kavramlar ve kullanıcıyla doğrulanması gereken noktalar
- Bilinen sorunlar
- Sıradaki tek iş

güncellenmelidir.

Kalıcı ve alternatifli mimari kararlar `docs/DECISIONS/` altında ADR olarak
kaydedilmelidir.
