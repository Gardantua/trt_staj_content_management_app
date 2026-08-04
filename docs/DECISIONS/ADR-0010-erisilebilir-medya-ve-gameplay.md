# ADR-0010: Erişilebilir Medya ve Kapsayıcı Gameplay Sözleşmesi

## Durum

Accepted

## Bağlam

Quiz ekranında her soruda bir görsel gösterilecektir. Bazı sorular görseldeki
bilgiye dayanır; diğerlerinde içerik kapağı kullanılır. Yalnız `imageUrl` ve
klasik alt metin eklemek yeterli değildir: görseli tanımak cevabın parçasıysa
ayrıntılı alt metin cevabı sızdırabilir, yetersiz alt metin ise görmeyen
kullanıcıya eşdeğer soru sağlamaz. Beş dakikalık sabit süre de görme, motor
veya bilişsel farklılıkları olan kullanıcılar için engel oluşturabilir.

Proje API-first olduğu için frontend'den önce gerekli veri ve davranış
sözleşmeleri kurulmalıdır. WCAG 2.2 AA mühendislik hedefidir; tek başına
hukukî uygunluk veya sertifika iddiası değildir.

## Karar

- Aşama 4 ile Aşama 5 arasına ayrı Aşama 4.1 eklenecektir.
- Medya varlıkları değişmez kimlik ve storage referansıyla temsil edilecek;
  dosya değişikliği yeni medya kimliği üretecektir.
- PostgreSQL medya metadatası ve iş referansları için doğru kaynak olacak;
  dosya içeriği storage portu arkasında kalacaktır.
- Yayınlanan content kapak görseline sahip olacaktır. Soruya özel görsel
  isteğe bağlıdır; yoksa yayın anındaki kapak quiz sürümüne sabitlenir.
- Medya `INFORMATIVE` veya `DECORATIVE` rolü taşır. Bilgi taşıyan görselde
  alternatif metin zorunludur; dekoratif görsel frontend'e semantik olarak
  dekoratif olduğunu bildirecek şekilde döner.
- Görseli tanımanın sorunun parçası olduğu durumda editör, aynı doğru cevap ve
  şıklarla çalışan, cevabı sızdırmayan eşdeğer erişilebilir soru metni
  sağlayacaktır. Publish doğrulaması bunu zorunlu kılacaktır.
- Gameplay API, frontend'in ayrı modüllerden veri birleştirmesine gerek
  bırakmadan çözümlenmiş görseli ve erişilebilirlik metadatasını döndürecektir.
- Feedback API sözleşmesi `CORRECT`/`INCORRECT` gibi semantik durum taşıyacak;
  frontend renk yanında metin ve simge kullanacak ve sonucu ekran okuyucuya
  durum mesajı olarak duyuracaktır.
- Kullanıcı attempt başlamadan standart veya en az on kat uzun süre modunu
  seçebilecektir. Seçim herkese açık olacak, engel/sağlık bilgisi tutulmayacak,
  attempt'te sürümlü sabitlenecek ve skor bonusu üretmeyecektir.
- Mevcut MVP ses veya video sorusu sunmayacaktır. Gelecekte eklenirse anlamlı
  ses için caption/transcript ve anlamlı görsel bilgi için audio description veya
  betimleyici transcript publish önkoşulu olacaktır.
- Frontend aşaması WCAG 2.2 AA için klavye, odak, kontrast, renk dışı ipucu,
  metin büyütme/reflow, hareket azaltma ve dokunma hedefi testlerini ayrıca
  uygulayacaktır.

## Gerekçe

- Eşdeğer soru sunumu, alt metnin hem cevap güvenliği hem erişilebilirlik
  sorununu tek başına çözemediği görsel soruları kapsar.
- Medya değişmezliği, quiz sürümlemesinin geçmiş attempt'leri koruma amacını
  görsel içerik için de sürdürür.
- Herkese açık süre tercihi hassas sağlık verisi toplamadan daha fazla
  kullanıcının quizi tamamlamasını sağlar.
- Semantik API durumu, frontend tasarımını belirli bir renge bağlamadan ekran
  okuyucu ve farklı görsel sunumları destekler.
- Storage portu, henüz bilinmeyen kurum medya altyapısını domain modeline
  bağlamaz.

## Alternatifler

- Yalnız `imageUrl` eklemek daha kısa olurdu; güvenilmeyen dış adres, kırık
  bağlantı, dosya değişikliği ve erişilebilirlik bütünlüğü sorunlarını
  çözmezdi.
- Her görsele yalnız alt metin vermek daha basit olurdu; görsel tanıma
  sorularında ya cevabı sızdırır ya da eşdeğer deneyim sağlamazdı.
- Erişilebilirlik tercihini engel raporuna bağlamak rekabet kurallarını daha
  sıkı tutabilirdi; hassas veri, doğrulama ve ayrımcılık riski oluştururdu.
- Sabit beş dakikayı korumak gameplay'i değiştirmezdi; zamanın oyunun
  esası olmadığı ve süre bonusu bulunmadığı mevcut quizde gereksiz engel
  oluştururdu.
- Bütün erişilebilirliği frontend'e bırakmak backend'i küçültürdü; fakat
  eksik alternatif metin, eşdeğer prompt, medya ve süre politikası frontend
  tarafından güvenilir biçimde üretilemezdi.

## Sonuçlar

- Content, quiz, gameplay ve yeni media sınırındaki kontrollü genişletme Aşama
  4.1 olarak uygulandı.
- Dosya içeriği PostgreSQL'e gömülmedi. İlk adapter yerel dosya sistemidir ve
  geliştirme/test içindir; production object storage veya CDN seçimi port
  arkasında değiştirilecektir.
- JPEG/PNG, 5 MB, 4096×4096, dosya imzası, çözülebilir içerik ve SHA-256
  kontrolleri storage sınırında uygulanır.
- Admin içerik girişi alternatif metin ve gerektiğinde eşdeğer soru metni
  hazırlama sorumluluğu taşıyacaktır.
- Aşama 7 leaderboard tie-break kararı tamamlama hızına dayanmamalı; farklı
  süre modları aynı skor hesabını kullanmalıdır.
- Frontend yapılmadan tam WCAG uygunluğu kanıtlanamaz; backend aşaması
  yalnız gerekli sözleşme ve kuralları garanti eder.
- Yerel dosya yazımı ile PostgreSQL transaction'ı tek atomik işlem değildir;
  production adapter'ında başarısız metadata kaydı sonrası sahipsiz dosya
  temizliği veya telafi mekanizması gerekir.

## Referanslar

- [W3C Web Content Accessibility Guidelines 2.2](https://www.w3.org/TR/WCAG22/)
- [W3C Making Audio and Video Media Accessible](https://www.w3.org/WAI/media/av/)
- [W3C Timing Adjustable açıklaması](https://www.w3.org/WAI/WCAG22/Understanding/timing-adjustable.html)

## Yeniden değerlendirme tetikleyicileri

- Kurumun zorunlu erişilebilirlik, KVKK veya medya storage standardını bildirmesi
- Canlı, sesli veya videolu quizlerin kapsama alınması
- Zamanın oyunun esası olduğu rekabetçi/canlı modun eklenmesi
- Ayrı erişilebilir soru varyantının skor adaletini etkilediğini gösteren
  kullanıcı araştırması
- Media aggregate veya storage maliyetini değiştirecek ölçülmüş hacim
