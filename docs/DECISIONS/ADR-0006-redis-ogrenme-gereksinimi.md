# ADR-0006: Redis'in Öğrenme Gereksinimi Olarak Kullanılması

## Durum

Accepted

## Bağlam

Önceki karar Redis'i yalnız ölçülmüş performans ihtiyacı varsa uygulanacak
opsiyonel bir read model olarak tutuyordu. Kullanıcı Redis'i bu proje içinde
uygulayarak veri yapısını, cache/read-model tutarlılığını, rebuild ve kesinti
davranışını öğrenmeyi açık bir proje hedefi olarak belirledi.

Redis'in zorunlu olması, kalıcı iş verisinin Redis'e taşınmasını veya PostgreSQL
üzerindeki doğru leaderboard davranışından önce eklenmesini gerektirmez.

## Karar

- Redis hedef mimarinin zorunlu öğrenme bileşenidir.
- Redis yol haritasındaki Aşama 8'de uygulanacaktır.
- Leaderboard kuralları ve deterministik doğru sonuç önce Aşama 7'de PostgreSQL
  üzerinde tamamlanacaktır.
- Redis yalnız PostgreSQL'den yeniden oluşturulabilir leaderboard read model
  olarak başlayacaktır.
- PostgreSQL ve Redis sonuç eşitliği, rebuild, stale veri ve Redis kesintisi
  gerçek Redis container'ıyla test edilecektir.
- Cache ve rate limiting bu karar nedeniyle otomatik olarak kapsama eklenmez.

## Gerekçe

- PostgreSQL ile Redis'in farklı sorumlulukları çalışan kod üzerinde öğrenilir.
- Önce/sonra ölçümü performans kazancı ile tutarlılık maliyetini görünür kılar.
- Rebuild testi Redis'in doğru kaynak olmadığını pratik olarak kanıtlar.
- Ayrı aşama, leaderboard iş kuralı hatası ile Redis adapter hatasını ayırır.

## Alternatifler

- Redis'i yalnız performans ihtiyacına bağlı tutmak üretim sadeliği açısından
  daha ucuzdur; fakat projenin açık öğrenme hedefini karşılamaz.
- Redis'i Aşama 1'de kurmak teknolojiye daha erken temas sağlar; fakat henüz
  cache'lenecek doğrulanmış leaderboard davranışı olmadığı için anlamlı kabul
  kriteri ve karşılaştırma üretmez.
- Redis'i kalıcı skor deposu yapmak çift doğru kaynak ve veri kaybı riski
  oluşturur.

## Sonuçlar

- Aşama 8 opsiyonel değildir.
- Redis yine Aşama 0 veya Aşama 1 çalışma ortamına eklenmez.
- PostgreSQL kalıcı doğru kaynak olarak kalır.
- Redis yokken yazma ve gameplay doğruluğu bozulmamalıdır.

## Yeniden değerlendirme tetikleyicileri

- Kurum Redis yerine farklı bir read-model/cache teknolojisini zorunlu kılarsa
- Leaderboard ürün kapsamından çıkarılırsa
- Güvenlik veya operasyon politikası yerel/CI Redis kullanımını engellerse
