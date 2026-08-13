# ADR-0024: Aktif quiz, quiz geçmişi ve XP koruma

## Durum

Accepted — 10.08.2026

## Bağlam

Admin arayüzündeki taslak ve teknik sürüm dili, editörün gerçek görevini
gereksiz biçimde karmaşıklaştırıyordu. Editörün ihtiyacı yeni quiz hazırlamak,
aktif quizleri düzenlemek ve eski quizleri geçmişte görebilmektir. Kullanıcının
daha önce kazandığı XP, quiz daha sonra kaldırıldığında kaybolmamalıdır.

## Karar

- Admin ana menüsünde `Quizler` ve `Quiz geçmişi` ayrı sayfalardır.
- Kullanıcıya açık olmayan çalışma durumu arayüzde `Hazırlanıyor` olarak sunulur;
  teknik draft/sürüm oluşturma adımları editöre gösterilmez.
- Hiç kullanıma açılmamış quiz kalıcı olarak silinebilir.
- Daha önce kullanıma açılmış quiz kalıcı silinmez; aktif yayın sürümü
  `ARCHIVED` durumuna taşınır ve varsa tamamlanmamış çalışma kopyası atılır.
- Soru değişikliğinde önceki yayın sürümü geçmişte kalır, yeni sürüm aktif olur.
- XP append-only ledger değişmeden kalır. Quiz kaldırma veya soru güncelleme
  kazanılmış XP üzerinde silme ya da düzeltme üretmez.
- Quiz geçmişi eski sürümlerin soru sayısını, tarihini ve cevap anahtarlı PDF'ini
  sunar.
- Kullanıcı dizi detayındaki demo sezon/bölüm rafı kaldırılır. Sezon ve bölüm
  verisi, admin quiz kapsamı için backend'de korunmaya devam eder.

## Gerekçe

UUID'yi tek başına XP kaydında bırakmak toplam XP'yi korur fakat eski puanın
hangi soru ve cevap anahtarıyla oluştuğunu açıklamaz. Mevcut değişmez quiz sürümü
zaten bu kanıtı tuttuğu için yeni snapshot tablosu eklemek yerine arşivleme
mekanizmasını kullanmak en küçük ve güvenli çözümdür.

## Sonuçlar

- Editör teknik draft ve sürüm yaşam döngüsünü yönetmez.
- Aktif ekran yalnız hazırlanmakta olan veya kullanıma açık quizleri gösterir.
- Geçmiş ekranı kaldırılan quizleri ve aktif quizlerin eski sürümlerini birlikte
  gösterir.
- Oynanmış quizler ve XP kayıtları açıklanabilir kalır.
- Kullanıcı dizi detayında yalnız içerik ve ilgili quizleri görür.

## Yeniden değerlendirme tetikleyicileri

- Quiz geçmişi için yasal saklama veya anonimleştirme süresi belirlenirse
- Çok yüksek sürüm hacmi arşiv sorgularını ölçülebilir biçimde yavaşlatırsa
- Eski attempt ayrıntılarının tamamen silinmesi yönünde hukukî gereksinim oluşursa
