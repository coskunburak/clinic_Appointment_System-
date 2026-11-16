# Sunum sözleşmeleri ve başlangıç envanteri

16 Kasım 2026 · `main` · `579179539790cb254722aee2360adb0ac6c5fb33` · başlangıç çalışma ağacı temiz.

Teknoloji: pom Java 21 / Boot 3.4.2; Thymeleaf 3 ve Security 6 Boot tarafından yönetilir; yerel Bootstrap 5.3.3, vanilla JS; compose PostgreSQL 17. Eski ajan belgesindeki Boot 4.1 ifadesi kaynakla uyuşmuyor. Maven bağımlılık çözümü çalıştırılmadı. GitHub kamu deposu incelendi; uzaktaki HEAD eşitliği fetch ile doğrulanmadı. S10 501 PASS tarihsel kanıt, bu değişikliklerin kanıtı değildir.

## Koruma matrisi

| Şablon | Eylem / bağımlılık | Koruma / denetim |
|---|---|---|
| `admin/branch-hours.html` | `POST @{/admin/branches/{id}/hours/{weekday}/delete(id=${branch.id},weekday=${h.weekday})}`; `POST @{/admin/branches/{id}/hours(id=${branch.id})}` ${hoursForm} | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `admin/branches.html` | `POST @{/admin/branches/{id}/deactivate(id=${b.id})}`; `POST @{/admin/branches/{id}/activate(id=${b.id})}`; `POST @{/admin/branches}` ${branchForm}; `POST #`; `POST #` | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `admin/dashboard.html` | Mevcut bağlantılar ve render koşulları | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `admin/departments.html` | `GET @{/admin/departments}`; `POST @{/admin/departments/{id}/deactivate(id=${d.id})}`; `POST @{/admin/departments/{id}/activate(id=${d.id})}`; `POST @{/admin/departments}` ${departmentForm}; `POST #` | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `admin/services.html` | `GET @{/admin/services}`; `POST @{/admin/services/{id}/duration(id=${bs.id})}`; `POST @{/admin/services/{id}/pricing(id=${bs.id})}`; `POST @{/admin/services/{id}/deactivate(id=${bs.id})}`; `POST @{/admin/services/{id}/activate(id=${bs.id})}`; `POST @{/admin/services/assign}` ${branchServiceForm}; `POST @{/admin/services/definitions/{id}/deactivate(id=${sd.id})}`; `POST @{/admin/services/definitions/{id}/activate(id=${sd.id})}`; `POST @{/admin/services/definitions}` ${serviceDefinitionForm}; `POST #` | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `admin/staff-availability.html` | `POST @{/admin/staff/assignments/{assignmentId}/weekly-availability/{availabilityId}/delete(assignmentId=${selectedAssignmentId},availabilityId=${w.id})}`; `POST @{/admin/staff/assignments/{assignmentId}/weekly-availability(assignmentId=${selectedAssignmentId})}` ${weeklyForm}; `POST @{/admin/staff/assignments/{assignmentId}/exceptions/{exceptionId}/delete(assignmentId=${selectedAssignmentId},exceptionId=${ex.id})}`; `POST @{/admin/staff/assignments/{assignmentId}/exceptions(assignmentId=${selectedAssignmentId})}` ${exceptionForm} | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `admin/staff.html` | `POST @{/admin/staff}` ${staffForm}; `POST @{/admin/staff/assign}` ${assignmentForm}; `POST @{/admin/staff/eligibility}` ${eligibilityForm} | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `auth/login.html` | `POST @{/login}` | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `error/400.html` | Mevcut bağlantılar ve render koşulları | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `error/403.html` | Mevcut bağlantılar ve render koşulları | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `error/404.html` | Mevcut bağlantılar ve render koşulları | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `error/500.html` | Mevcut bağlantılar ve render koşulları | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `fragments/layout.html` | `POST @{/logout}` | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `patient/appointments.html` | `POST ` | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `patient/book.html` | `POST @{/patient/book}` ${bookingForm} | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `patient/dashboard.html` | Mevcut bağlantılar ve render koşulları | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `staff/dashboard.html` | Mevcut bağlantılar ve render koşulları | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |
| `staff/schedule.html` | `GET @{/staff/schedule}`; `POST @{/staff/appointments/{id}/approve(id=${app.id})}`; `POST @{/staff/appointments/{id}/complete(id=${app.id})}`; `POST @{/staff/appointments/{id}/approve(id=${app.id})}`; `POST @{/staff/appointments/{id}/complete(id=${app.id})}`; `POST ` | HEAD ile Thymeleaf, ID, alan, data ve form nitelikleri karşılaştırması; runtime sahibi tarafından |

## Alanlar ve kimlikler

Aşağıdaki envanter yeniden tasarım öncesidir. Tüm `th:if/unless/each`, model ifadeleri, `th:field/object/action/href`, CSRF alanları, fragment adı ve dinamik data nitelikleri HEAD karşılaştırmasının parçasıdır.

### admin/branch-hours.html

**ID:** `weekday`, `startTime`, `endTime`

**Girdi (ID / binding):** `weekday / *{weekday}`, `startTime / *{startTime}`, `endTime / *{endTime}`

### admin/branches.html

**ID:** `organizationId`, `branchName`, `timeZone`, `address`, `editOrganizationModal`, `editOrganizationForm`, `editOrganizationModalLabel`, `editOrgName`, `editBranchModal`, `editBranchForm`, `editBranchModalLabel`, `editBranchName`, `editBranchTimeZone`, `editBranchAddress`

**Girdi (ID / binding):** `organizationId / *{organizationId}`, `branchName / *{name}`, `timeZone / *{timeZone}`, `address / *{address}`, `editOrgName / name`, `editBranchName / name`, `editBranchTimeZone / timeZone`, `editBranchAddress / address`

### admin/dashboard.html

**ID:** 

**Girdi (ID / binding):** 

### admin/departments.html

**ID:** `filterBranchId`, `branchId`, `name`, `editDepartmentModal`, `editDepartmentForm`, `editDepartmentModalLabel`, `editDeptName`

**Girdi (ID / binding):** `filterBranchId / branchId`, `branchId / *{branchId}`, `name / *{name}`, `editDeptName / name`

### admin/services.html

**ID:** `serviceBranchFilter`, `assignBranchId`, `serviceDefinitionId`, `durationMinutes`, `fee`, `currency`, `defOrgId`, `defName`, `defDescription`, `editServiceDefModal`, `editServiceDefForm`, `editServiceDefModalLabel`, `editDefName`, `editDefDescription`

**Girdi (ID / binding):** `serviceBranchFilter / branchId`, `'duration-' + ${bs.id} / durationMinutes`, `'fee-' + ${bs.id} / fee`, `assignBranchId / *{branchId}`, `serviceDefinitionId / *{serviceDefinitionId}`, `durationMinutes / *{durationMinutes}`, `fee / *{fee}`, `currency / *{currency}`, `defOrgId / *{organizationId}`, `defName / *{name}`, `defDescription / *{description}`, `editDefName / name`, `editDefDescription / description`

### admin/staff-availability.html

**ID:** `weekday`, `startTime`, `endTime`, `localDate`, `exceptionKind`, `exceptionTimeFields`, `excStart`, `excEnd`, `reason`

**Girdi (ID / binding):** `weekday / *{weekday}`, `startTime / *{startTime}`, `endTime / *{endTime}`, `localDate / *{localDate}`, `exceptionKind / *{kind}`, `excStart / *{startTime}`, `excEnd / *{endTime}`, `reason / *{reason}`

### admin/staff.html

**ID:** `branchId`, `fullName`, `email`, `password`, `staffType`, `specialtyRequiredMarker`, `staffSpecialty`, `phone`, `assignStaffId`, `assignBranch`, `assignDepartment`, `eligAssignment`

**Girdi (ID / binding):** `branchId / *{branchId}`, `fullName / *{fullName}`, `email / *{email}`, `password / *{password}`, `staffType / *{staffType}`, `staffSpecialty / *{specialty}`, `phone / *{phone}`, `assignStaffId / *{staffId}`, `assignBranch / *{branchId}`, `assignDepartment / *{departmentId}`, `eligAssignment / *{assignmentId}`, `${'srv-' + srv.id} / branchServiceIds`

### auth/login.html

**ID:** `email`, `password`, `login-button`

**Girdi (ID / binding):** `email / email`, `password / password`

### error/400.html

**ID:** 

**Girdi (ID / binding):** 

### error/403.html

**ID:** 

**Girdi (ID / binding):** 

### error/404.html

**ID:** 

**Girdi (ID / binding):** 

### error/500.html

**ID:** 

**Girdi (ID / binding):** 

### fragments/layout.html

**ID:** `clinicNavbar`, `nav-patient-book`, `nav-patient-appointments`, `nav-staff-daily`, `nav-staff-weekly`, `nav-staff-pending`, `nav-admin-dashboard`, `nav-admin-branches`, `nav-admin-hours`, `nav-admin-departments`, `nav-admin-services`, `nav-admin-staff`, `logout-button`, `main-content`

**Girdi (ID / binding):** 

### patient/appointments.html

**ID:** `cancelAppointmentModal`, `cancelAppointmentForm`, `cancelModalLabel`, `cancelAppointmentSummary`, `cancelReason`, `btn-confirm-cancel`

**Girdi (ID / binding):** `cancelReason / reason`

### patient/book.html

**ID:** `booking-form`, `branchId`, `branchHelp`, `branchServiceId`, `staffId`, `appointmentDate`, `slots-loading`, `slots-container`, `no-slots-msg`, `slots-list`, `summary-branch`, `summary-service`, `summary-staff`, `summary-date`, `summary-time`, `summary-duration`, `summary-fee`, `btn-confirm-booking`

**Girdi (ID / binding):** `branchId / *{branchId}`, `branchServiceId / *{branchServiceId}`, `staffId / *{staffId}`, `appointmentDate / *{appointmentDate}`, `${'slot-' + slot} / startTime`

### patient/dashboard.html

**ID:** 

**Girdi (ID / binding):** 

### staff/dashboard.html

**ID:** 

**Girdi (ID / binding):** 

### staff/schedule.html

**ID:** `scheduleDateInput`, `btn-approve-appointment`, `rejectModal`, `rejectForm`, `rejectModalLabel`, `rejectPatientName`, `rejectReason`, `btn-confirm-reject`

**Girdi (ID / binding):** `scheduleDateInput / date`, `rejectReason / reason`

## JavaScript ve yüksek riskli akışlar

- `initPatientBookingWizard`: şube/hizmet değişiminde GET navigasyonu; hekim/tarihte `/patient/slots?staffId=&branchServiceId=&date=` JSON; startTime radyoları ve yedi summary ID; mevcut yükleme/boş/hata akışı. Fonksiyon gövdesi değiştirilmeyecek.
- İptal: `cancelAppointmentModal`, `cancelAppointmentForm`, `cancelAppointmentSummary`, `cancelReason`, data-action-prefix ve data-appointment-id; submit gerekçesi/CSRF korunur.
- Ret: `rejectModal`, `rejectForm`, `rejectPatientName`, `rejectReason`; `/staff/appointments/{id}/reject`, reason/CSRF korunur.
- Yönetici: editOrganization/Branch/Department/ServiceDef modal ve form ID; data-org/branch/dept/def değerleri ve action üretimi korunur.
- Bağımlı alanlar: assignBranch → assignDepartment; eligAssignment → .elig-service-item; exceptionKind → excStart/excEnd; staffType → staffSpecialty. Mevcut dinleyiciler değiştirilmeyecek.
- Üç inline onchange aynı form.submit çağrısıyla clinic.js içindeki dinleyiciye taşınabilir; endpoint/GET alanları aynı kalır.
- Sayfalama page/size; takvim date/view; şube branchId; uygunluk assignmentId/staffId korunur.
- canCancel, canApprove, canComplete, orgWideOrgIds ve canManageOrgCatalog yetki/durum koşulları korunur.

## İlgili kabul kanıtları

`FR-027`, `FR-028`, `NFR-009`; `TC-027`, `TC-028`, `TC-045`. Mevcut RoleBasedWebMvcIT, hasta/personel/admin MVC testleri ve CustomErrorControllerMvcTest değiştirilmez. Statik envanter veri doğruluğu, render başarısı veya işlem yetkisi testi yerine geçmez.
