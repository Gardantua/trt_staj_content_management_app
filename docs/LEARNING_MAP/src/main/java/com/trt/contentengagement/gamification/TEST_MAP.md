# Gamification iş kuralı - test eşleştirmesi

## Doğrudan modül testleri

- `XpTransactionTest.scoreMatchPolicyAwardsXpEqualToServerScore`: Completion
  politikasında XP'nin sunucu skoruna eşit olduğunu kanıtlar.
- `XpTransactionTest.zeroScoreCompletionStillProducesAnIdempotencyLedgerFact`:
  sıfır skorun bile işlenmiş kaynağı temsil eden geçerli ledger gerçeği olduğunu
  kanıtlar.
- `XpTransactionTest.adjustmentMustBeNonZeroAndReferenceOriginalTransaction`:
  sıfır tutarlı veya hedefsiz admin düzeltmesinin domain tarafından reddedildiğini
  kanıtlar.
- `XpTransactionTest.adjustmentIsANewSignedLedgerEntry`: Düzeltmenin özgün kaydı
  değiştirmek yerine yeni imzalı kayıt olduğunu kanıtlar.
- `GamificationModuleArchitectureTest`: Modülün başka modüllerin iç ayrıntılarına
  yasak bağımlılık kurmadığını denetler.

## Akışın başka modüllerdeki kanıtları

- `GameplayIntegrationTest.onlyFirstCompletionOfSameQuizAwardsXp`: Aynı quizde
  yalnız ilk tamamlamanın ledger kaydı ürettiğini gerçek PostgreSQL ile kanıtlar.
- `GameplayIntegrationTest.zeroScoreFirstCompletionCreatesOneParticipantLedgerEntry`:
  İlk sıfır puanın tek sıfır ledger kaydı oluşturduğunu kanıtlar.
- `MessagingIntegrationTest`: XP satırı ve `xp.changed` Outbox olayının birlikte
  oluşmasını, replay'in olayı çoğaltmamasını kanıtlar.
- `LeaderboardIntegrationTest`: Ledger toplamından deterministik sıralama üretildiğini
  kanıtlar.

## Test türleri neden farklı?

Unit test domain kuralını hızlı ve altyapısız kanıtlar. Integration test ise SQL,
transaction ve unique constraint gibi ancak gerçek PostgreSQL ile görülen davranışı
kanıtlar. Mimari test, kod çalışsa bile katman sınırının yanlış bağımlılıkla aşılmasını
engeller.
