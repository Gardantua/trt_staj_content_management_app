# ADR-0026: Aynı quizde yalnız ilk tamamlamanın XP kazandırması

## Durum

Accepted (11.08.2026)

## Bağlam

Gameplay aynı quizin tamamlanmış bir attempt sonrasında yeniden çözülmesine izin
verir. Önceki politika her farklı attempt'in skorunu XP ledger'a ekliyordu. Bu,
aynı soruları tekrar tekrar çözerek leaderboard toplamını artırmayı mümkün kılıyor
ve quiz başarısından çok tekrar sayısını ödüllendiriyordu.

## Karar

- Aynı `(userId, quizId)` için yalnız ilk tamamlanan attempt XP kazanır.
- İlk tamamlamanın XP'si kendi kesin sunucu skoruna eşittir. Daha sonraki
  tamamlamalar alıştırmadır; skor üretir fakat `earnedXp = 0` döndürür ve XP
  transaction'ı oluşturmaz.
- Kural quiz sürümünden bağımsızdır. Sorular yeni bir sürümle güncellense bile aynı
  `quizId` ikinci bir ilk-tamamlama ödülü oluşturmaz.
- İlk ödül hakkı `gameplay_quiz_reward_claims` tablosunda `(user_id, quiz_id)`
  primary key'iyle korunur. Veritabanı tekilliği, eşzamanlı tamamlamalarda iki
  ödülü engeller.
- Attempt'in `earned_xp` alanı API sonucunu taşır. `quiz.completed` v2 ayrıca
  ilk-tamamlama ödül hakkını boolean olarak taşır; böylece ilk 0 puan ile 0 XP'li
  alıştırma ayrılır. Eski v1 olayları geriye uyumlu okunur.
- Kural kazanılmış XP'yi geri almaz; her kullanıcı/quiz için geçmişteki ilk attempt'i
  gelecekteki ödülleri engelleyen hak sahibi olarak kaydeder. V14 yalnız eksik
  sıfır-puanlı ilk tamamlamalara sıfır tutarlı ledger kaydı ekler.

## Değerlendirilen alternatifler

### Her attempt'e XP vermeye devam etmek

Tekrar oynama motivasyonunu artırır; fakat aynı quiz üzerinden sınırsız XP üretir
ve leaderboard'u içerik bilgisinden çok tekrar sayısına yaklaştırır.

### Yalnız en yüksek skoru ledger'a yansıtmak

Gelişme motivasyonu verir; ancak append-only ledger'da önceki ödülün farkını
düzeltmelerle yönetmek ve eşzamanlı sonuçları sıralamak daha karmaşıktır.

### Tekrar çözmeyi tamamen engellemek

XP istismarını önler; fakat kullanıcının alıştırma yapmasını gereksiz yere
engeller. Bu nedenle tekrar çözüm korunmuştur.

## Sonuçlar

- Quiz tekrar çözülebilir; ikinci ve sonraki sonuç ekranı `0 XP` gösterir.
- Leaderboard yalnız ilk-tamamlama ödülü ve yönetici düzeltmelerinden etkilenir.
- Eski XP geçmişi append-only kalır; geriye dönük puan silinmez.
- İlk tamamlamanın skoru sıfırsa ödül hakkı yine tüketilir. Bu, “ilk tamamlama”
  kuralının bilinçli sonucudur.

## Test kanıtı

- `QuizAttemptTest`: ödülün kaydedilmesini, `0 XP` alıştırma kararını ve kararın
  sonradan değiştirilememesini korur.
- `QuizCompletedIntegrationEventV1Test`: eski olay sözleşmesini korur.
- `QuizCompletedIntegrationEventV2Test`: ilk-tamamlama hakkı ile alıştırmanın
  birbirinden ayrıldığını korur.
- `GameplayIntegrationTest.onlyFirstCompletionOfSameQuizAwardsXp`: iki farklı
  attempt sonunda tek claim, tek XP transaction ve değişmeyen XP özetini gerçek
  PostgreSQL ile kanıtlar.
- `GameplayIntegrationTest.zeroScoreFirstCompletionCreatesOneParticipantLedgerEntry`:
  ilk 0 puanın tek sıfır ledger kaydı oluşturduğunu ve alıştırmanın çoğaltmadığını
  kanıtlar.
