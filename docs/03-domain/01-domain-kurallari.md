# Domain iş kuralları — değişmezler (invariants)

## Kurum, şube, bölüm
- **ORG-01:** `Organization` aktif organizasyonlar `org_id` ile izole edilir. Yönetici sadece `Membership` kapsamındaki organizasyonu yönetebilir.
- **ORG-02:** `Branch.organizationId` zorunlu; `Department.branchId` zorunlu; `BranchService.branchId` zorunlu.
- **ORG-03:** Branch timeZone IANA ID olmalı (`Europe/Istanbul` örneği). `BranchOpeningHours`: gün + başlangıç ve bitiş saatleri, `start < end`; gece mesaisi MVP dışında; gerekiyorsa iki güne bölünür.
- **ORG-04:** Tek kurum, tek şube desteklenmeli; yeni şube eklemek Java kodu değiştirmemeli.

## Katalog ve para
- **CAT-01:** `ServiceDefinition` organizasyona aittir; `BranchService` servis için şubeye özgü `durationMinutes` ve `Money(amount,currency)` taşır.
- **CAT-02:** Süre `1..480` dakika (varsayım). Para `BigDecimal` ≥ 0; iki ondalık basamağa normalize edilir; `TRY` varsayılan ama sabit kodlanmaz.
- **CAT-03:** Fiyat değişikliği yalnız yeni randevuları etkiler; geçmiş `Appointment` snapshot sabit kalır.
- **CAT-04:** İnaktif hizmete yeni randevu alınamaz; eski kayıt erişilebilir.

## Personel
- **STA-01:** `StaffProfile.type` doktor/hemşire/teknisyen; doktor için `specialty` zorunlu, diğerleri için opsiyonel.
- **STA-02:** Personel birden çok şubeye `StaffAssignment` ile atanabilir; her `StaffServiceEligibility` aynı şubedeki `BranchService` ile eşleşmelidir.
- **STA-03:** Hizmet uygunluğu ve aktiflik hem boş saat sorgusunda hem nihai rezervasyon transaction'ında kontrol edilir.
- **STA-04:** Bir personelin farklı şubelerde aynı saatte randevusu olamaz.

## Uygunluk
- **SCH-01:** Slot, şubenin açık olduğu saatlere **tamamıyla** sığmalıdır.
- **SCH-02:** Slot, ilgili `WeeklyAvailability` penceresine tamamen sığmalıdır; 10:00–10:30 slotu 10:15'te başlayan mesaiye uymaz.
- **SCH-03:** `AvailabilityException` tam gün izin veya belirli kapalı aralık olarak rutin uygunluğu daraltır; ekstra mesai açmak için açıkça `ADDITIONAL_AVAILABILITY` türü tanımlanabilir (MVP'de opsiyonel).
- **SCH-04:** Başlangıç `now` sonrasındadır; **UTC** ile kıyaslanır. 15 dk slot adımı yalnız arayüz aday üretiminde; randevu süresi hizmete göre belirlenir.
- **SCH-05:** Başlangıç zamanı şube saat dilimiyle çözülür; DST nedeniyle olmayan/çift anlamlı local saat belirsizse API açık hata verir; gizli otomatik kaydırma yapılmaz.

## Randevu
- **BKG-01:** `end = start + service duration` gerçek an üzerinden hesaplanır; `[start,end)` aralığı yarı açıktır.
- **BKG-02:** İki aralık ancak `existing.start < proposed.end && existing.end > proposed.start` ise çakışır.
- **BKG-03:** Aynı çalışanda PENDING/CONFIRMED/(geçmiş) COMPLETED randevu varken çakışan yeni rezervasyon yasaktır.
- **BKG-04:** Farklı personelin aynı zaman aralığında işlemi olabilir. Aynı hastanın iki randevusu olmaması SHOULD kapsamındadır.
- **BKG-05:** Yeni randevu PENDING açılır; onay yetkili personel veya yönetici tarafından yapılır.
- **BKG-06:** Randevu iptal/ret statüleri slotu serbest bırakır; tekrar açmak için yeni istek gerekir. İptal randevu başlangıcından önce gerçekleştirilmelidir (`now < startAt`; S02'de tartışılan 2 saatlik kısıt önerisi genel kural lehine korunmuştur).
- **BKG-07:** Yanlış durum geçişi 409; yetkisiz kişi için 403 veya kaynak varlığını gizlemek amacıyla 404.
- **BKG-08:** İptal/ret/completed işlemleri audit history kaydeder ve aynı transaction'da commit edilir.
- **BKG-09:** Sunucudan gelen hasta kimliği oturumdan türetilir, formdan güvenilmez.
- **BKG-10:** Randevu geçmiş kayıtları fiziksel silinmez; imha/anonimleştirme ileride politika ile yönetilir.

## Etkinlik/yaşam döngüsü
`PENDING -> CONFIRMED | REJECTED | CANCELLED`;
`CONFIRMED -> CANCELLED | COMPLETED`;
`REJECTED,CANCELLED,COMPLETED -> terminal`.
`COMPLETED` yalnız randevu bitişinden sonra işaretlenebilir (tasarım tercihi).
