# Kabul / test senaryo kataloğu

**Kullanım:** her TC maddesinin otomatize mi manuel mi olduğu ilgili sprint sırasında işaretlenir; henüz hiçbiri çalıştırılmış değildir.

| Test | Gereksinim | Adımlar / fixture | Beklenen sonuç |
|---|---|---|---|
| TC-001 | FR-001 | ADMIN yeni kurum oluşturur; ad ve aktiflik alanlarını kaydeder | kayıt kalıcı, aynı kimlikle geri okunabilir (`OrganizationAdminServiceIT.shouldCreateAndUpdateOrganization` — Otomatize / OWNER_TEST_PENDING) |
| TC-002 | FR-002 | Tek ve iki şubeli kurum fixture kur; iki şube ayrı listelenir | şubeler aynı kurumla ilişkilidir, ek Java değişikliği gerekmez (`OrganizationAdminServiceIT.shouldSupportSingleAndMultiBranchOrganizations` — Otomatize / OWNER_TEST_PENDING) |
| TC-003 | FR-003 | Pzt 09:00–17:00 tanımla; 17:00 dışındaki slotu iste | mesai kaydı görünür; dışarıdaki slot reddedilir (`OrganizationAdminServiceIT.shouldConfigureWeeklyOpeningHoursAtomically` — Otomatize / OWNER_TEST_PENDING) |
| TC-004 | FR-004 | Merkez şubesine Kardiyoloji ve Radyoloji ekle | kayıtlar yalnız ilgili şubede gösterilir (`OrganizationAdminServiceIT.shouldManageDepartmentsAcrossBranches` — Otomatize / OWNER_TEST_PENDING) |
| TC-005 | FR-005 | Genel Muayene kataloğu oluştur; başka kurum hesabından iste | katalog görünür, scope dışından erişilemez (`CatalogManagementServiceIT.shouldDefineAndRetrieveOrganizationService` — Otomatize / OWNER_TEST_PENDING) |
| TC-006 | FR-006 | 30 dakika/800.00 TRY kaydet; 0dk ve negatif fiyat gönder | geçerli kabul; geçersiz alan reddedilir (`CatalogManagementServiceIT.shouldAllowDifferentPricingAndDurationsAcrossBranches`, `BranchServiceTest` — Otomatize / OWNER_TEST_PENDING) |
| TC-007 | FR-007 | Doktor ve teknisyen kaydet | iki kayıt staff_type ile ayrılır |
| TC-008 | FR-008 | Doktora Kardiyoloji uzmanlığı gir; boş uzmanlıkla doktor kaydet | uzmanlık gösterilir; doktor için boş alan 422 |
| TC-009 | FR-009 | Doktora Muayene ver, Ultrason verilmeyen hizmeti iste | yetkili listede görünür; yetkisiz hizmet reddedilir |
| TC-010 | FR-010 | Pzt 09–12 weekly availability kaydet | uygunluk adayları yalnız bu aralıkta |
| TC-011 | FR-011 | 10–11 izin ekle; 10:15 slot iste | slot bulunamaz veya rezervasyon reddedilir |
| TC-012 | FR-012 | 09–12 mesaisinde 11:45 başlangıç/30dk deneyin | işlemin tüm süresi sığmadığı için reddedilir |
| TC-013 | FR-013 | Hasta şube/hizmet/personel/tarih seçer | kayıt seçilen yetkili bileşenlerle saklanır |
| TC-014 | FR-014 | 09–09:30 pending varken 09:15–09:45 dene; 09:30–10 dene | ilk istek 409; sınırdaki ikinci kabul edilir |
| TC-015 | FR-015 | iki ayrı DB transaction aynı staff/intervale barrier ile gönder | DB üzerinde en fazla 1 aktif kayıt; diğer kontrollü conflict |
| TC-016 | FR-016 | 30dk/800 TRY randevu oluştur ve yeniden oku | işlem/şube/çalışan/süre/ücret/start/end kalıcıdır |
| TC-017 | FR-017 | hasta yeni randevu oluştur | PENDING ve başlangıç audit kaydı |
| TC-018 | FR-018 | atanmış doktor PENDING randevuyu onaylar | CONFIRMED ve actor history kaydı (`AppointmentApprovalServiceIT.authorizedDoctorCanApprovePendingAppointment` — Otomatize / OWNER_TEST_PENDING) |
| TC-019 | FR-019 | atanmış doktor PENDING randevuyu reddeder | REJECTED ve slot açılmıştır (`AppointmentApprovalServiceIT.doctorRejectsAppointmentWithReason` — Otomatize / OWNER_TEST_PENDING) |
| TC-020 | FR-020 | sahibi başlangıç öncesi pending/confirmed randevuyu iptal eder | CANCELLED, sonraki aynı interval rezervasyonu olabilir (`AppointmentApprovalServiceIT.patientCancelsOwnPendingAppointment` — Otomatize / OWNER_TEST_PENDING) |
| TC-021 | FR-021 | bitişi geçen onaylı randevuyu personel tamamlar | COMPLETED; gelecekteki randevu için reddedilir (`AppointmentApprovalServiceIT.doctorCompletesAfterAppointmentEnd` — Otomatize / OWNER_TEST_PENDING) |
| TC-022 | FR-022 | Hasta A, Hasta B appointment UUIDsini GET yapar | 403/404; başka hastanın içeriği sızmaz |
| TC-023 | FR-023 | Dr A, Dr B randevularını görmeye/onaylamaya çalışır | kendi listeye erişir, diğerini değiştiremez |
| TC-024 | FR-024 | yetkili şube yöneticisi hizmet ve mesai günceller | scope içinde başarılı, dışında 403/404 |
| TC-025 | FR-025 | kayıt/giriş yap; DB parola kolonunu incele; hatalı şifre dene | plaintext yok, yanlış şifre giriş yapamaz |
| TC-026 | FR-026 | başka organization branchId ile admin formu gönder | işlem reddedilir ve başka tenant değişmez |
| TC-027 | FR-027 | hasta booking ve staff approval ekranlarını gezin | navigasyon, durum ve hata mesajları doğru (`Sprint08AcceptanceSpecificationTest`, `PatientBookingControllerMvcTest`, `StaffScheduleControllerMvcTest`, `RoleBasedWebMvcIT` — Otomatize / `CODE_COMPLETE_AWAITING_OWNER_TEST`) |
| TC-028 | FR-028 | admin ekranından şube/hizmet/staff/availability yönet | temel CRUD değişiklikleri kalıcıdır (`AdminDashboardControllerMvcTest`, `AdminBranchControllerMvcTest`, `AdminDepartmentControllerMvcTest`, `AdminServiceControllerMvcTest`, `AdminStaffControllerMvcTest`, `RoleBasedWebMvcIT` — Otomatize / `CODE_COMPLETE_AWAITING_OWNER_TEST`) |
| TC-029 | FR-029 | aynı hasta iki farklı staff ile çakışan slot talep eder | SHOULD uygulandıysa ikinci istek reddedilir |
| TC-030 | FR-030 | onay, iptal/ret ve tamamla geçişi tetikle | actor/from/to/time bilgisi append-only (`AppointmentApprovalServiceIT.statusHistoryRecordsAllTransitionsInOrder` — Otomatize / OWNER_TEST_PENDING) |
| TC-031 | FR-031 | aynı requestKey ile formu iki kez gönder; gövde değiştir | aynı içerik aynı sonuç; farklı içerik 409 |
| TC-032 | FR-032 | fiyatı 800den 900e değiştir; eski appointment getir | eski 800, yeni 900 snapshot |
| TC-033 | FR-033 | IANA zone local->UTC dönüşümünü; DST gap/overlap test et | belirsiz/olmayan saat açık hataya dönüşür |
| TC-034 | FR-034 | demo db reset + seed uygula | 1 org 2 şube, kurmaca hesaplar ve sahte hizmetler hazır |
| TC-035 | FR-035 | personel onayından sonra patient panelini kontrol et | COULD uygulandıysa in-app notification var |
| TC-036 | FR-036 | şube/hizmet filtresiyle raporu görüntüle | COULD uygulandıysa yalnız scope içi sonuç |
| TC-037 | NFR-001 | CSRF olmadan form gönder; session cookie kontrol et | koruma etkin ve parola hashli |
| TC-038 | NFR-002 | service katmanını yetkisiz actor ile doğrudan çağır | controller bypass edilse de erişim reddedilir |
| TC-039 | NFR-003 | boş PostgreSQL üzerinde Flyway migrate; ikinci kez migrate | şema kurulur, ikinci çağrı idempotent ve validate başarılı |
| TC-040 | NFR-004 | BookingServiceTest + BookingConcurrencyIT çalıştır | kritik kurallar yeşil ve çıktı arşivli |
| TC-041 | NFR-005 | fresh clone + compose + run + demo hesapla login | README doğru ve sistem sıfırdan açılır |
| TC-042 | NFR-006 | git log tarihlerini gün olarak grupla | en az 10 farklı gerçek gün, GitHub push/commit görülebilir |
| TC-043 | NFR-007 | GitHub URL, SABİS yüklemesi, sunum katılımı doğrula | üç teslim kanıtı tamamlanmış |
| TC-044 | NFR-008 | 1000 örnek randevu/50 çalışanla yerel ölçüm al | hedef p95<1sn; ortama göre ölçüm raporlanır |
| TC-045 | NFR-009 | hatalı UUID, negatif fiyat, çakışma gönder | kullanıcıya anlamlı kod; stacktrace/SQL yok (`Sprint08AcceptanceSpecificationTest.customErrorPageHandles403`, `CustomErrorControllerMvcTest`, `WebExceptionHandler` — Otomatize / `CODE_COMPLETE_AWAITING_OWNER_TEST`) |
| TC-046 | NFR-010 | mimari bağımlılıkları denetle; controller taraması yap | controller iş mantığı ve direkt repo bağımlılığı içermez |

## Özellikle gözden geçirilecek sınırlar
- `[09:00,09:30)` ve `[09:30,10:00)` çakışmaz; milisaniye veya saniye sınırını test et.
- PENDING randevu slotu bloke eder; REJECTED/CANCELLED slot açar; COMPLETED geçmişi korur.
- İki personel aynı saatte çalışabilir; bir personelin iki şubede aynı anda randevusu olamaz.
- Tek bir role kontrolü yeterli değildir; patient ownership ve branch scope gerekir.
- Döviz ve fiyat değişikliği snapshot verisini etkilemez.
- Eşzamanlılık gerçek DB üzerinde koşulmalıdır; mock lock kanıt sayılmaz.
