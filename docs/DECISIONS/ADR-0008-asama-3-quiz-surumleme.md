# ADR-0008: Aşama 3 Quiz Aggregate ve Değişmez Yayın Sürümü

## Durum

Accepted

## Bağlam

Quiz soruları, cevap seçenekleri, doğru cevap ve puanlama politikası geçmiş
attempt sonuçlarını belirler. Yayındaki bu veriler yerinde değiştirilirse aynı
quiz sürümünü kullanan iki attempt farklı kurallarla değerlendirilir. Ayrıca
editörün eksik bir draft üzerinde çalışabilmesi, fakat eksik yapının
yayımlanamaması gerekir.

## Karar

- `Quiz`, `QuizVersion`, `Question` ve `AnswerOption` hiyerarşisinin aggregate
  root'u olacaktır.
- Domain modeli Spring/JPA'dan bağımsız saf Java kalacaktır.
- İlk sürüm `DRAFT` olarak oluşturulacaktır.
- Yayın için en az bir soru, her soruda en az iki seçenek ve tam bir doğru
  seçenek zorunlu olacaktır.
- `PUBLISHED` ve `ARCHIVED` sürümlerin başlık, soru ve seçenek içeriği yerinde
  değiştirilemeyecektir.
- Yeni düzenleme, aktif yayın sürümünü yeni kimliklerle kopyalayan ve sürüm
  numarasını artıran yeni bir draft oluşturacaktır.
- Yeni draft yayımlandığında önceki aktif yayın sürümü `ARCHIVED` olacaktır.
- Puanlama davranışının ayrıntısı Aşama 4'te uygulanacak; ancak sürüm sınırı
  şimdiden `STANDARD_V1` kimliğiyle quiz sürümünde sabitlenecektir.
- Yönetim DTO'su doğru cevap bilgisini taşırken kullanıcı DTO'su `correct`
  alanını hiç tanımlamayacaktır.
- Quiz değişikliği ve admin audit kaydı aynı application transaction'ında
  kesinleşecektir.
- Quiz, içeriğin varlığını content modülünün `ContentReferenceVerifier`
  application portundan doğrulayacak; content infrastructure paketine veya
  tablosuna doğrudan sorgu yapmayacaktır.
- PostgreSQL; foreign key, sıra tekilliği, tek draft ve tek doğru seçenek
  constraint/index'leriyle yapısal bütünlüğü koruyacaktır.

## Gerekçe

- Değişmez yayın sürümü geçmiş ve aktif attempt'lerin aynı soru ve puanlama
  sözleşmesine bağlı kalmasını sağlar.
- Aggregate, yayınlanacak soru ağacını tek transaction ve iş kuralı sınırında
  doğrular.
- Ayrı kullanıcı DTO'su doğru cevabın yanlış serialization ayarıyla sızma
  riskini azaltır.
- Application portu modüller arası bağımlılığı infrastructure yerine kararlı
  use-case sözleşmesine yöneltir.
- Veritabanı constraint'leri uygulama dışı yazma ve yarış aralıklarında son
  savunmayı sağlar.

## Alternatifler

- Soruları bağımsız aggregate yapmak daha küçük güncellemeler sağlayabilirdi;
  fakat yayın anında bütün quiz ağacını kilitleme ve doğrulama koordinasyonunu
  artırırdı.
- Yayındaki sürümü yerinde güncellemek daha az satır üretirdi; geçmiş
  attempt'lerin anlamını değiştirirdi.
- Doğru cevap alanını tek DTO'da görünmez annotation ile saklamak daha az DTO
  üretirdi; sözleşme yanlışlığında hassas alanın sızma riskini korurdu.
- Puan değerlerini doğrudan sorulara yazmak Aşama 3'ü kısa tutabilirdi; farklı
  puanlama davranışlarının geçmiş sonuçları değiştirmemesi için politika
  kimliğini sürümde sabitlemek daha açık bir sınır sağladı.

## Sonuçlar

- Her yeni düzenleme yeni quiz, soru ve seçenek kimlikleri üretir; storage
  büyümesi değişmezlik karşılığında kabul edilir.
- Aynı quizde en fazla bir draft bulunur.
- Aktif yayın arşivlenirse kullanıcı sorgusunda quiz görünmez.
- Doğru seçeneği değiştirirken PostgreSQL tek-doğru index'i korunur; adapter
  aynı transaction içinde eski doğru bayraklarını temizleyip doğrulanmış
  aggregate durumunu yazar ve seçenek sıra kimliklerini korur.
- Attempt Aşama 4'te `quiz_version_id` ve `scoring_policy_version` değerlerini
  sabitlemelidir.

## Yeniden değerlendirme tetikleyicileri

- Tek quizde aggregate yüklemeyi pahalılaştıran ölçülmüş soru/sürüm hacmi
- Aynı draft üzerinde eşzamanlı çok editör gereksinimi
- Birden fazla aktif yayın varyantı veya A/B testi ihtiyacı
- Soru bankası ve soruların quizler arasında paylaşılması ihtiyacı
- Puanlama politikası sözleşmesinin `STANDARD_V1` dışında genişlemesi
