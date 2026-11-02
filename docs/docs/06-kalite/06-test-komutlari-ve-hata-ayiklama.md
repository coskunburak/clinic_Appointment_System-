# Öğrenci tarafından manuel çalıştırılacak komutlar

## Başlangıç gereklilikleri
Mac/Linux terminalinde repo kökünden çalıştır. Java 21 ve Docker + PostgreSQL erişimi gerekir; Maven Wrapper ilk sprintte oluşturulacaktır. Komutlar **şu anki ZIP'teki planlama dokümanlarını** test etmez; ilgili uygulama dosyaları geliştirildiğinde kullanılır.

## Bir kez ortam doğrulama
```bash
java -version
docker version
./mvnw -version
```

## Normal sprint unit/integ test
```bash
./mvnw -B test
./mvnw -B verify
```

## Git doğrulama
```bash
git status --short
git diff --check
git log --format=%as | sort -u | wc -l
```

## Hata geri bildirim protokolü
Öğrenci başarısız testin komutunu, ilgili test ismini, ilk kök neden stacktrace'ini, `git status --short` ve ortam farklarını (gizli bilgiler silinmiş biçimde) ajana gönderir. Ajan yalnız ilgili kaynak/ilgili testleri inceler, test çalıştırdığını iddia etmez, güncellemeyi yapar ve yeniden tek yorumsuz komut bloğu döndürür.
