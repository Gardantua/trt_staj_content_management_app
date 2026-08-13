# ADR-0020: Admin medya yükleme ve kapak bağlama akışı

## Durum

Accepted

## Bağlam

Yayınlanabilir bir içerikte kapak görseli ve alternatif metin zorunludur. Backend
zaten JPEG/PNG türünü, 5 MB boyut sınırını, 4096 × 4096 piksel üst sınırını,
dosya imzasını ve çözülebilir görsel içeriğini denetleyen ayrı medya yükleme
endpoint'ine; ardından medya kimliğini içerik kapağına bağlayan ayrı bir
endpoint'e sahiptir. Admin web bu önkoşulu tamamlayamadığı için taslak publish
edilemiyordu.

## Karar

- Admin ekranı görseli önce `POST /api/v1/admin/media/images` ile multipart
  olarak yükler; tarayıcı `Content-Type` değerini elle kurmaz, form sınırını
  kendisi üretir.
- Başarılı yüklemenin döndürdüğü değişmez medya kimliği, alternatif metinle
  birlikte `PUT /api/v1/admin/contents/{contentId}/cover` isteğine gider.
- Kullanıcı, mevcut kapağın alternatif metnini yeni dosya yüklemeden de
  güncelleyebilir. Yeni dosya yüklendiğinde yeni medya kimliği bağlanır.
- UI'nin yalnız taslak içerikte bu işlemleri göstermesi ADR-0023 ile
  değiştirilmiştir; yayımlanmış içeriğin kapağı ve metadata'sı da güncellenebilir.
  Dosya doğrulaması ve publish
  yetkisi backend'de kalır.
- Admin ve kullanıcı istemcileri aynı `contentUrl` için binary blob isteğini oturum boyunca paylaşır.
  Her render kendi kısa ömürlü object URL'sini kullanır; başarısız indirme cache'den
  çıkarılarak sonraki denemenin ağa gitmesine izin verilir.

## Gerekçe

İki adım, medya varlığının içerikten bağımsız ve değişmez olmasını korur;
görselin metadata ve güvenlik denetiminden geçtiğini bağlama işleminden önce
açıkça gösterir. Editör ayrıca alternatif metni kapak kararının parçası olarak
görür; bu alan dosya adıyla karışmaz.

## Alternatifler

- Tek bir "içeriği kaydet" multipart isteği daha kısa bir ekran akışı sunardı;
  ancak mevcut media/content API sınırlarını birleştirir, hata ayrımını zayıflatır
  ve değişmez medya modelini istemciye gizlerdi.
- Dosyayı yalnız istemci boyut ve tür kontrolüyle kabul etmek hızlı görünürdü;
  güvenilmeyen tarayıcı verisine dayandığı için imza ve decode doğrulamasını
  atlayamaz.
- Kapak için serbest bir dış URL kabul etmek depolama işini azaltırdı; kırık
  bağlantı, kullanım hakkı ve dosya değişmezliği risklerini artırdığı için
  seçilmedi.

## Sonuçlar

- Admin, yayın önkoşulunu yeni backend yazmadan tamamlayabilir.
- Kullanıcı arayüzü doğrulanmış medya kimliği ve alternatif metin üzerinden
  kapak okuyabilir.
- Yükleme ile metadata kaydının atomik olmaması, production storage adapter'ı
  tasarlanırken ele alınacak mevcut backend riskidir; bu arayüz onu gizlemez.

## Yeniden değerlendirme tetikleyicileri

- Kurumun object storage/CDN veya görsel kullanım hakkı politikasını açıklaması
- Çoklu görsel, kırpma, önizleme veya medya kütüphanesi ihtiyacı oluşması
- Production kimlik ve hosting topolojisinin belirlenmesi
