# ADR-0034: İçerik ve quiz metinlerinin yerelleştirilmiş sunum projeksiyonu

## Durum

Accepted

## Bağlam

Arayüz Türkçe ve İngilizce çalışsa da film/dizi, sezon/bölüm, quiz, soru, seçenek
ve erişilebilirlik metinleri tek Türkçe alanlarda tutuluyordu. Aynı quizin İngilizce
kopyasını oluşturmak quiz kimliğini değiştirerek kullanıcının ilk tamamlama, XP ve
geçmiş ilişkisini parçalayacaktı. Yayımlanmış sürümün oyun kuralı da yerinde
değiştirilemez.

## Karar

- Türkçe alanlar mevcut tablolarda ana ve fallback metin olarak kalacaktır.
- İngilizce metinler içerik, sezon, bölüm, quiz sürümü, soru ve seçenek kimliklerine
  bağlı ayrı çeviri tablolarında tutulacaktır.
- Çeviri tablosunda doğru cevap, zorluk, süre veya puanlama alanı bulunmayacaktır.
- Kullanıcı sorguları `Accept-Language: en` geldiğinde İngilizce projeksiyonu uygular;
  eksik çeviri öğesi Türkçe kaynağa düşer.
- Gameplay snapshot yalnız gösterilen metni yerelleştirir. Soru/seçenek kimlikleri ve
  doğru seçenek kimliği değişmez.
- Admin, aynı kayıt üzerinde İngilizce çeviriyi okuyup idempotent `PUT` ile kaydeder.
  Kaydetme işlemi audit kaydı üretir ve başka içeriğe ait alt kimlikleri reddeder.
- İlk migration, repoda kaynağı bulunan altı demo katalog özeti ile kullanıcı tarafından
  sağlanan Rocky içerik metni ve birinci soruyu İngilizce olarak geri doldurur.

## Gerekçe

Sunum çevirisini oyun kuralından ayırmak yayımlanmış sürüm değişmezliğini, attempt
sonuçlarını ve tek quiz başına ilk XP hakkını korur. Ayrı dil kopyası daha az tablo
gerektirirdi; fakat kullanıcı geçmişini iki farklı quiz kimliğine bölerdi.

## Sonuçlar

- Yeni sezon, bölüm veya soru eklendiğinde admin çeviri formunda boş İngilizce alanla
  görünür ve ayrıca çevrilmelidir.
- İngilizce çeviri eksikse kullanıcı bozuk/boş metin yerine Türkçe fallback görür.
- Üçüncü dil eklenirse migration constraint'i, admin dil seçenekleri ve doğrulama
  sözleşmesi genişletilmelidir.
- Backend hata mesajlarının yerelleştirilmesi bu kararın kapsamında değildir.
