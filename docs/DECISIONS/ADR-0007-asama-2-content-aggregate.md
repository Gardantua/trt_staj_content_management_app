# ADR-0007: Aşama 2 Content Aggregate ve Yayınlama Sınırı

## Durum

Accepted

## Bağlam

İçerik kataloğunda içerik, sezon ve bölüm numaralarının tutarlı kalması;
yayınlanmamış kayıtların kullanıcıya görünmemesi ve kritik editör işlemlerinin
izlenebilmesi gerekir. Domain nesnelerini doğrudan JPA entity yapmak framework
bağımlılığını iş kurallarına taşır. Her tabloyu bağımsız repository ile
değiştirmek ise hiyerarşi kurallarının farklı transaction'lara dağılmasına yol
açar.

## Karar

- `Content`, `Season` ve `Episode` hiyerarşisinin aggregate root'u `Content`
  olacaktır.
- Domain modeli Spring ve JPA'dan bağımsız saf Java olarak kalacaktır.
- JPA entity'leri ve domain modeli infrastructure adapter'ında birbirine
  dönüştürülecektir.
- İçerik başlangıçta `DRAFT` olur. Yayınlanmış içerik yerinde değiştirilemez.
- `SERIES` en az bir sezon ve her sezon en az bir bölüm içermeden yayınlanamaz.
  `FILM` sezon içeremez.
- Sezon ve bölüm numarası tekilliği hem domain kuralı hem PostgreSQL unique
  constraint'i ile korunacaktır.
- İçerik değişikliği ile ilgili audit kaydı aynı application transaction'ında
  yazılacaktır. Audit kaydı silinen kaynağın ardından korunabilsin diye kaynak
  tablosuna foreign key ile bağlanmayacaktır.
- Kullanıcı listesi ilk sürümde `page`/`size` tabanlı standart pagination ve
  en fazla 100 kayıt sınırı kullanacaktır.

## Gerekçe

- Aggregate, bir istekte değişmesi gereken hiyerarşiyi tek iş kuralı sınırında
  tutar.
- Saf domain modeli kuralları veritabanı ve web framework'ü olmadan hızlı test
  etmeyi sağlar.
- Veritabanı constraint'i paralel isteklerde uygulama ön kontrolünün tek başına
  kapatamayacağı yarış aralığını korur.
- Transaction içi audit, iş değişikliği kesinleşip audit kaydının kaybolması
  şeklindeki kısmi başarıyı önler.
- Offset pagination başlangıç için anlaşılırdır; çok büyük kataloglarda oluşan
  maliyet ölçülmeden cursor karmaşıklığı eklenmez.

## Alternatifler

- Domain sınıflarını JPA entity yapmak daha az dönüşüm kodu üretirdi; ancak
  domain'i Hibernate yaşam döngüsü ve annotation'larına bağlardı.
- Sezon ve bölümleri bağımsız aggregate yapmak daha küçük kayıt güncellemeleri
  sağlayabilirdi; fakat yayınlama bütünlüğü için ek kilit ve koordinasyon
  gerektirirdi.
- İlk günden cursor pagination kullanmak büyük ve hızla değişen listelerde daha
  kararlı olurdu; mevcut katalog hacmi bilinmediği için offset yaklaşımı daha
  öğretici ve yeterli seçildi.
- Audit'i yalnız uygulama loguna yazmak daha ucuz olurdu; kalıcı ve sorgulanabilir
  yönetim geçmişi sağlamazdı.

## Sonuçlar

- Büyük sezon/bölüm koleksiyonlarında aggregate'in tamamını yükleme maliyeti
  ölçülmelidir; gerçek sorun oluşursa aggregate sınırı yeniden değerlendirilir.
- Yayınlanmış içerik düzeltmesi ileride yeni içerik/metadata sürümü davranışı
  gerektirebilir.
- Silme işlemi katalog satırlarını kaldırır, audit kaydını korur.
- API'deki standart pagination sözleşmesi sürümlü `/api/v1` yolu altında
  kararlı tutulur.

## Yeniden değerlendirme tetikleyicileri

- Tek içerikte aggregate yüklemeyi pahalılaştıran ölçülmüş sezon/bölüm hacmi
- Yayındaki metadata için düzenleme veya sürümleme ürün ihtiyacı
- Katalog listeleme sorgularında offset pagination kaynaklı ölçülmüş gecikme
- Audit saklama/KVKK politikasının belirlenmesi
