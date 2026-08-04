# ADR-0009: Aşama 4 Sunucu Otoriteli Gameplay

## Durum

Accepted

## Bağlam

Attempt süresi, cevap değişmezliği, doğru cevap açıklaması ve skor istemciye
bırakılırsa kullanıcı payload'ı değiştirerek sonucu etkileyebilir. Paralel veya
tekrarlanan istekler de aynı soruya birden fazla cevap üretebilir.

## Karar

- `QuizAttempt`, answer ve ACTIVE/COMPLETED/EXPIRED yaşam döngüsünün aggregate
  root'u olacaktır.
- Kimlik request body'den değil `CurrentActorProvider` bağlamından alınacaktır.
- Attempt yayınlanmış quiz sürümünü ve `STANDARD_V1` politika kimliğini sabitler.
- `STANDARD_V1` için attempt süresi sunucu `Clock` kaynağıyla beş dakika, doğru
  cevap 100 puan, yanlış cevap 0 puandır; süre bonusu yoktur.
- Sorular sunucu sırasıyla ve tek kez cevaplanır.
- Doğru seçenek yalnız cevap kalıcılaştırıldıktan sonra answer response'unda
  açıklanır; sonraki soru aynı response ile açılır.
- Son cevap attempt'i otomatik tamamlar. Complete endpoint'i tamamlanan sonucu
  tekrar döndürerek idempotent kalır.
- Aynı kullanıcı ve quiz için en fazla bir ACTIVE attempt bulunur; completed
  veya expired attempt sonrasında tekrar çözmeye izin verilir.
- Answer istekleri `Idempotency-Key` ister. Aynı key/aynı payload aynı sonucu,
  aynı key/farklı payload `IDEMPOTENCY_KEY_CONFLICT` üretir.
- PostgreSQL unique constraint'leri aynı soru, idempotency key ve aktif attempt
  yarışlarında son savunmadır.
- Tamamlanma gerçeği process-içi `QuizAttemptCompleted` domain event sözleşmesi
  olarak tanımlanır; bu olay tek başına güvenilir dış teslimat anlamına gelmez.

## Gerekçe

- Sunucu saati ve puanı istemci manipülasyonunu önler.
- Cevap kaydından sonra doğru şıkkı açıklamak geri bildirim sağlar; cevap
  değişmez olduğu için açıklama skoru değiştirmek için kullanılamaz.
- Sabit ve basit puanlama, süre bonusu ürün kararı olmadan karmaşıklık eklemez.
- Domain ön kontrolü anlamlı hata, PostgreSQL constraint'i gerçek yarış
  güvenliği sağlar.

## Alternatifler

- Doğru cevapları yalnız quiz sonunda göstermek daha az bilgi açardı; kullanıcı
  her sorudan sonra geri bildirim istediği için reddedildi.
- Süre/zorluk bonusu daha oyunlaştırılmış olabilirdi; açıklanabilir ve
  deterministik ilk politika için ertelendi.
- Tek sefer çözme leaderboard'u sadeleştirebilirdi; tekrar çözme kuralı ilgili
  leaderboard aşamasında ayrıca belirlenecektir.

## Sonuçlar

- Frontend doğru seçeneği yeşil, yanlış seçimi kırmızı gösterebilir; erişilebilirlik
  için renk yanında metin/simge kullanmalıdır.
- Beş dakikalık süre quiz bazlı değildir; ürün ihtiyacı oluşursa süre quiz
  sürümüne taşınmalı ve politika sürümlenmelidir.
- XP Aşama 5'te domain event gerçeğinden PostgreSQL ledger üretir.

## Yeniden değerlendirme tetikleyicileri

- Quiz bazlı veya soru bazlı süre ihtiyacı
- Süre/zorluk bonusu ürün kararı
- Aynı attempt'te geri dönüş ve cevap değiştirme ihtiyacı
- Farklı tekrar çözme veya tek-attempt politikası
