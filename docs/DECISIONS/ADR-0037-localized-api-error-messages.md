# ADR-0037: Kararlı hata kodlarından yerelleştirilmiş API mesajı üretmek

## Durum

Accepted — 19.08.2026

## Bağlam

Arayüz ve domain içeriği Türkçe–İngilizce çalışırken backend hata zarfındaki `message`
alanı yalnız İngilizceydi. Hatalar yalnız controller advice katmanından çıkmıyor;
authentication, authorization, CSRF, geçici test kimliği ve rate-limit filtreleri de
MVC handler'a ulaşmadan doğrudan JSON yazıyor. Yerelleştirme bu yolların tamamında aynı
kurala uymalı, fakat kararlı `code`, HTTP durumu ve trace ID değişmemelidir.

## Karar

- İstemci dili standart `Accept-Language` başlığından seçilir; desteklenen diller `tr`
  ve `en`, varsayılan/fallback dil Türkçedir.
- Hata metinleri `api.error.{STABLE_CODE}` anahtarlarıyla Spring message bundle'larında
  tutulur. Domain ve application exception'ları dil bilgisi taşımaz.
- `GlobalExceptionHandler` seçilmiş MVC locale'ini, servlet filtreleri ise doğrudan
  request başlığını ortak `ApiErrorMessageResolver` üzerinden çözer.
- Sözlükte bulunmayan bir kod Türkçe istekte güvenli genel iş kuralı mesajına düşer;
  İngilizce istekte handler'ın mevcut güvenli fallback metni korunur.
- Dil seçimi hata zarfının yalnız `message` alanını etkiler. `code`, status, `traceId`
  ve timestamp sözleşmesi değişmez.
- Frontend'in bütün JSON/form/medya/authentication/CSRF istekleri saklanan arayüz dilini
  `Accept-Language` olarak gönderir.

## Gerekçe

Kararlı hata kodunu çeviri anahtarı yapmak, farklı exception sınıflarının metnine veya
Java türüne bağımlılığı kaldırır. Ortak resolver hem MVC hem filtre yolunda aynı sözlüğü
kullanır. Domain modeli kullanıcı dili, Spring locale context'i ve HTTP başlığından
habersiz kaldığı için katman sınırı korunur.

## Değerlendirilen alternatifler

- Exception oluşturulurken çevirmek: kullanıcının dili domain/application katmanına
  taşınır ve HTTP dışı kullanımda belirsizleşir.
- Yalnız frontend'de hata kodunu çevirmek: native/haricî API istemcilerine İngilizce
  backend metni bırakır ve filtrelerden gelen yeni kodlarda iki ayrı sözlük yönetir.
- Her filter/handler içinde koşullu metin yazmak: kısa vadede basittir; fakat aynı hata
  kodunun farklı güvenlik yollarında tutarsızlaşmasına neden olur.

## Sonuçlar

- Yeni kullanıcıya gösterilen hata kodu eklenirken iki backend sözlüğü de güncellenmelidir.
- İngilizce dışındaki desteklenmeyen dil tercihleri Türkçeye düşer.
- Çeviri işlemi status veya authorization kararını değiştirmez ve güvenlik ayrıntısı
  açığa çıkarmaz.
- Üçüncü dil eklenirse desteklenen locale listesi ve yeni message bundle birlikte
  genişletilmelidir.

## Yeniden değerlendirme tetikleyicileri

- Üçüncü bir backend API dilinin eklenmesi
- Haricî istemcilerin makine-okunur alan doğrulama ayrıntısı istemesi
- Hata sözlüklerinin ayrı bir çeviri yönetim sistemine taşınması
