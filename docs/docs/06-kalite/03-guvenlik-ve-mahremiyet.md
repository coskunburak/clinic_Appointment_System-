# Güvenlik modeli / mahremiyet sınırı

## Tehditler ve savunmalar
| Tehdit | Kontrol | Kanıt |
|---|---|---|
| Hesap ele geçirme | güvenli parola hash, login başarısızlığı mesajı, rate limiting SHOULD | login negatif testi |
| CSRF | state-changing POST'larda Spring Security CSRF token | 403 CSRF testi |
| XSS | Thymeleaf escape, güvenilmez HTML kullanmama | şüpheli input testleri |
| SQL Injection | JPA parametreleri, string SQL birleştirmemek | code review |
| IDOR | kaynak sahibi + membership scope service katmanında | TC-022,023,026,038 |
| Çift rezervasyon | transaction lock + overlap + gerçek DB testi | TC-015 |
| Yetki yükseltme | rol/organization ID'si formdan güvenilmez | TC-024,038 |
| Hassas verinin loglanması | maskeleme, minimal profil, kurmaca seed | log review |
| Kötü exception handling | genel kullanıcı hatası, requestId, iç log | TC-045 |

## Güvenlik uygulama noktaları
- `SecurityFilterChain`: yetkisiz URL'ler kapalı, public yalnız login/statik sayfalar; CSRF açık.
- `AccessPolicy`: `UserPrincipal` + hedef kaynak doğrulaması; çalışan veya yönetici scope'u test edilir.
- Parola BCrypt veya Argon2 (Spring Security `PasswordEncoder`); demo parolaları Git'te bulunmaz.
- Güvenli cookie/oturum ayarları TLS dağıtımında açılır (`Secure`, `HttpOnly`, `SameSite` doğru kararlarla).
- Production profile'da Docker seed kullanıcıları otomatik yaratılmaz.
- Hasta profilinde yalnız randevu için gereken minimum veri tutulur. Gerçek hasta dosyası, TCKN, tıbbi geçmiş eklenmez.

## Resmî çerçeve
OWASP ASVS 5.0.0 uygulama güvenliği için başvuru standardıdır: https://owasp.org/www-project-application-security-verification-standard/ . Bu öğrenci projesinin bir sertifikası veya hukukî uygunluk beyanı yoktur. Türkiye'deki kişisel veri mevzuatına uyum iddiası için ayrı uzman değerlendirmesi gerekir.

## Demo güvenliği
Tüm isimler sahte olmalıdır. Demo sırasında gerçek telefon/e-posta veya hastalık/teşhis bilgisi girilmez. `.env` ve DB dump GitHub'a yüklenmez.
