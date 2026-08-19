# ADR-0035: Resmî tabii izleme bağlantılarının güvenli saklanması

## Durum

Accepted — 19.08.2026

## Bağlam

Film ve dizi detaylarında kullanıcıyı ilgili tabii yapımına götüren bir izleme eylemi
gereklidir. Bağlantının editör tarafından değiştirilebilmesi gerekir; ancak serbest bir
URL alanı kimlik avı alan adlarına, `javascript:` şemalarına veya yönlendirme amaçlı
sorgu parametrelerine kapı açar. Uygulama bu URL'yi sunucu tarafında çağırmadığı için
SSRF üretmez, fakat kullanıcı navigasyonu yine güven sınırı olarak ele alınmalıdır.

## Karar

- İçerikte isteğe bağlı `watchUrl` saklanır. Bağlantısı bulunmayan eski içerikler için
  düğme gösterilmez ve uydurma veri geri doldurulmaz.
- Yalnız `https`, tam olarak `tabii.com` veya `www.tabii.com` alan adı ve resmî
  `/detail/{sayısal-kimlik}` ya da `/{dil}/detail/{sayısal-kimlik}[/{slug}]`
  biçimindeki içerik yolu kabul edilir.
- Kullanıcı bilgisi, özel port, sorgu parametresi ve fragment içeren URL reddedilir.
- Kural domain katmanında uygulanır; PostgreSQL `CHECK` constraint'i ikinci savunma
  katmanıdır. İstemci, düğmeyi oluşturmadan önce aynı dar kuralı tekrar doğrular.
- Güncelleme yalnız `EDITOR` ve `ADMIN` rolüne açık ayrı bir endpoint'tir ve audit
  kaydı üretir.
- Bağlantı yeni sekmede `target="_blank"` ve `rel="noopener noreferrer"` ile açılır.

## Gerekçe

Tam alan adı eşleştirmesi `tabii.com.saldirgan.example` gibi son ek hilelerini engeller.
HTTPS taşıma güvenliğini zorunlu kılar. Dar yol deseni ile sorgu ve fragment yasağı,
haricî veya gelecekte eklenebilecek genel yönlendirme yüzeyini küçültür. Domain ve
veritabanı kontrollerinin birlikte bulunması, farklı bir yazma yolu uygulama kuralını
atlasa bile kalıcı verinin bozulmasını önler.

## Değerlendirilen alternatifler

- Frontend'e sabit URL yazmak: dağıtım gerektirmeden içerik başına yönetilemez ve
  domain bütünlüğü sağlamaz.
- Her HTTPS URL'yi kabul etmek: daha esnektir, fakat kimlik avı riskini gereksiz yere
  büyütür.
- Uygulama içi yönlendirme endpoint'i: tıklama ölçümünü kolaylaştırır; buna karşılık
  açık yönlendirme ve ek bakım yüzeyi yaratır. Mevcut gereksinimde doğrulanmış hedefi
  doğrudan göstermek daha şeffaftır.

## Sonuçlar

- Editör doğru resmî içerik bağlantısını sağlamadan izleme düğmesi görünmez.
- tabii alan adı veya içerik URL biçimi değişirse mevcut kural yeni biçimi kabul etmez;
  kontrollü bir kod ve migration güncellemesi gerekir.
- Hiçbir sistem mutlak anlamda “hacklenemez” değildir. Bu karar, bu özelliğin bilinen
  URL ve yeni sekme risklerini katmanlı kontrollerle sınırlar; tabii hesabı, bölgesel
  erişim ve dış platformun kullanılabilirliği uygulamanın kontrolü dışındadır.

## Yeniden değerlendirme tetikleyicileri

- tabii'nin resmî içerik URL sözleşmesini değiştirmesi
- Birden fazla resmî TRT/tabii alan adının desteklenmesi
- Tıklama analitiği veya imzalı bağlantı gereksiniminin doğması
