# ADR-0022: Dört seçenekli soru, soru başına süre ve cevap anahtarlı PDF

## Durum

Accepted

## Bağlam

Quiz yazarlığı 2–6 seçenek ve attempt genelinde tek deadline destekliyordu. Ürün
kararı her sorunun tam dört seçenekli olması, her yeni soruda en fazla 30 saniye
verilmesi ve kullanıcının erken cevap vererek hemen ilerleyebilmesidir. Editörün
de yayın öncesi veya sonrasında içerik bilgileriyle birlikte doğru cevapları
işaretlenmiş gerçek bir PDF indirebilmesi gerekir.

## Karar

- Her soru tam dört seçenek taşır. Kural domain constructor'ında, admin API
  doğrulamasında ve transaction sonunda çalışan PostgreSQL deferred constraint
  trigger'ında korunur.
- Zorluk uygulama tarafından her zaman `MEDIUM` kaydedilir. Normal zorluk kullanıcı
  ekranına ayrıca gösterilmez.
- Quiz soru sayısı sabit değildir. Yayın için en az bir soru gerekir; on veya daha
  fazla soru da aynı dört seçenek ve 30 saniye kurallarıyla desteklenir.
- Yeni attempt için sunucu saatinden 30 saniyelik `questionDeadline` üretilir. Cevaplanan
  veya zaman aşımına uğrayan soru sonrasında attempt, kullanıcı sonuç kartında beklerken
  deadline tutmaz; kullanıcı sonraki soruya geçmeyi seçtiğinde sunucu yeni 30 saniyelik
  deadline üretir. Toplam quiz süresi yoktur.
- Süre dolan soru nullable `selectedOptionId`, sıfır puan ve yanlış sonuçla bir kez
  kaydedilir. Cevap ve timeout aynı pessimistic attempt kilidini kullanır; yarışta
  yalnız ilk geçerli komut sonuç üretir.
- Cevap anahtarlı PDF yalnız admin sözleşmesindeki doğru cevap verisinden, tarayıcıda
  dinamik yüklenen `pdfmake` ile oluşturulur ve `.pdf` olarak indirilir. Başlık,
  içerik türü/adı/açıklaması, quiz kimliği ve sürümü, kapsamı, soru sayısı, süre
  kuralı ve işaretli doğru cevaplar belgeye yazılır. Her soru quiz çözme ekranına
  yakın ayrı bir sayfa olarak düzenlenir; soruya özel görsel varsa o, yoksa içerik
  kapağı yetkili medya isteğiyle indirilip PDF içine gömülür.

## Gerekçe

Süreyi yalnız istemcide tutmak kolaydır fakat istemci güvenilir değildir; sekme
uyutma, saat değiştirme veya paralel istek ek süre ve çift cevap üretebilir. Sunucu
deadline'ı ve satır kilidi skorun tek otoritesini korur. Deferred constraint,
seçeneklerin persistence sırasında ayrı satırlar halinde yazılmasına izin verirken
transaction sonunda eksik/fazla seçenekli canlı soru kalmasını önler.

PDF'yi backend'de PDFBox benzeri bir kütüphaneyle üretmek merkezi şablon ve sunucu
font kontrolü sağlardı; buna karşılık gameplay/domain'e yeni belge üretim yükü ve
endpoint eklerdi. Adminin zaten yetkili cevap sözleşmesini aldığı bu aşamada,
dinamik istemci üretimi daha küçük bir backend değişikliği ve istek başına sunucu
maliyeti olmaması nedeniyle seçildi. Büyük PDF/font paketleri ilk ekran paketine
değil yalnız indirme tıklamasına yüklenir.

Sabit on soru yayın kuralı editör akışını basitleştirebilirdi; fakat film, bölüm ve
sezon quizlerinin doğal kapsamları farklıdır. Değişken sayı korunurken kapanış
uyarısı yalnız gerçekten kaydedilmemiş soru değişikliği olduğunda gösterilir.

## Sonuçlar

- Eski sorular migration sırasında normal zorluğa çekilir; dört seçenek kuralı
  bundan sonraki her transaction'da korunur.
- Kullanıcı erken cevap verirse puanı görür; sonraki sorunun 30 saniyesi yalnız
  `Sonraki soruya geç` komutunun sunucuda kabul edilmesiyle başlar.
- Timeout cevap kaydı denetlenebilir ve idempotenttir; doğru cevap soru açılmadan
  kullanıcı istemcisine gönderilmez.
- PDF gerçek indirilebilir dosyadır, fakat belge şablonu istemci sürümüyle birlikte
  dağıtılır.
- Aynı içerik kapağı birden fazla soruda kullanıldığında PDF üretimi görseli bir
  kez indirip tekrar kullanır; tek bir görsel yükleme hatası metin ve cevap
  anahtarının indirilmesini engellemez.

## Yeniden değerlendirme tetikleyicileri

- Soru türleri dört seçenek dışında veri girişi, çoklu doğru veya açık uçlu cevap isterse
- Erişilebilir süre politikası yeniden ürün kapsamına alınırsa
- PDF'nin imzalanması, merkezi arşivlenmesi, kurumsal fontu veya sunucu taraflı audit'i gerekirse
