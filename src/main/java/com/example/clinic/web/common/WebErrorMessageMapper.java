package com.example.clinic.web.common;

import com.example.clinic.booking.application.AppointmentConflictException;
import com.example.clinic.booking.application.BookingValidationException;
import com.example.clinic.booking.application.PatientOverlapException;
import com.example.clinic.booking.domain.AppointmentDomainException;
import com.example.clinic.common.exception.CurrencyMismatchException;

import java.util.Locale;

/**
 * Translates domain and application exceptions into safe, user-comprehensible Turkish error messages
 * without leaking internal stack traces, entity identifiers, class names, or SQL statements (NFR-009).
 */
public final class WebErrorMessageMapper {

    private WebErrorMessageMapper() {
    }

    public static String mapBookingError(Exception ex) {
        if (ex instanceof AppointmentConflictException) {
            return "Seçilen hekim için bu tarih ve saatte randevu çakışması tespit edildi. Lütfen farklı bir zaman dilimi seçiniz.";
        }
        if (ex instanceof PatientOverlapException) {
            return "Aynı tarih ve saat aralığında başka bir aktif randevunuz bulunmaktadır.";
        }
        if (ex instanceof BookingValidationException) {
            String msg = safeLower(ex.getMessage());
            if (msg.contains("past") || msg.contains("future")) {
                return "Geçmiş bir tarih veya saat için randevu oluşturulamaz.";
            }
            if (msg.contains("closed") || msg.contains("opening hours") || msg.contains("interval")) {
                return "Seçilen zaman dilimi şubenin çalışma saatleri dışındadır.";
            }
            if (msg.contains("eligible")) {
                return "Seçilen hekim bu hizmeti sunmaya yetkili değildir.";
            }
            if (msg.contains("available") || msg.contains("weekly") || msg.contains("exception") || msg.contains("leave")) {
                return "Seçilen hekim bu tarih ve saatte müsait değildir (izin veya mesai dışı).";
            }
            if (msg.contains("inactive")) {
                return "Seçilen şube, hizmet veya hekim şu anda aktif değildir.";
            }
            return "Randevu kuralları doğrulanamadı. Lütfen şube, hizmet, hekim ve zaman seçimini kontrol ediniz.";
        }
        if (ex instanceof AppointmentDomainException || ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            return mapGenericDomainError(ex, "Randevu talebi işlenemedi. Lütfen seçimlerinizi kontrol ediniz.");
        }
        return "Randevu işlemi gerçekleştirilirken bir hata oluştu. Lütfen tekrar deneyiniz.";
    }

    public static String mapLifecycleError(Exception ex, String defaultMessage) {
        if (ex instanceof AppointmentConflictException) {
            return "Randevu zaman diliminde çakışma tespit edildiği için işlem tamamlanamadı.";
        }
        if (ex instanceof AppointmentDomainException || ex instanceof IllegalStateException || ex instanceof IllegalArgumentException) {
            String msg = safeLower(ex.getMessage());
            if (msg.contains("reason")) {
                return "Gerekçe alanı boş bırakılamaz ve en fazla 500 karakter olabilir.";
            }
            if (msg.contains("completed") || msg.contains("after") || msg.contains("end time") || msg.contains("before")) {
                return "Bu işlem yalnızca randevunun zaman/durum kuralları sağlandığında gerçekleştirilebilir.";
            }
            if (msg.contains("not found")) {
                return "İlgili randevu kaydı bulunamadı.";
            }
            return "Randevu durumu bu işlem için uygun değil.";
        }
        return defaultMessage;
    }

    public static String mapAdminError(Exception ex, String defaultMessage) {
        if (ex instanceof CurrencyMismatchException) {
            return "Para birimi tutarsızlığı nedeniyle ücret güncellenemedi.";
        }
        String msg = safeLower(ex.getMessage());
        if (msg.contains("already exists") || msg.contains("duplicate") || msg.contains("already assigned") || msg.contains("already offers")) {
            return "Aynı isim veya tanımla kayıt zaten mevcut.";
        }
        if (msg.contains("time zone")) {
            return "Geçersiz IANA zaman dilimi seçildi (örnek: Europe/Istanbul).";
        }
        if (msg.contains("precede") || msg.contains("overlap") || msg.contains("must be before")) {
            return "Başlangıç saati bitiş saatinden önce olmalı ve aynı gün içindeki saat dilimleri çakışmamalıdır.";
        }
        if (msg.contains("requires both") || msg.contains("full-day leave")) {
            return "Kısmi izin ve fazla mesai türleri için başlangıç ve bitiş saati zorunludur; tam gün izinde saat belirtilmez.";
        }
        if (msg.contains("inactive")) {
            return "Pasif durumdaki bir kurum, şube veya hizmet üzerinde bu işlem yapılamaz.";
        }
        if (msg.contains("doctor") || msg.contains("specialty")) {
            return "Hekim (DOCTOR) kaydı için uzmanlık alanı zorunludur.";
        }
        if (msg.contains("cross-tenant") || msg.contains("belong")) {
            return "Seçilen kayıtlar aynı kurum veya şube kapsamına ait olmalıdır.";
        }
        if (msg.contains("positive") || msg.contains("duration") || msg.contains("fee") || msg.contains("negative") || msg.contains("currency")) {
            return "Süre, ücret veya para birimi değeri geçersiz.";
        }
        if (msg.contains("password")) {
            return "Şifre en az 6 karakter uzunluğunda ve güvenlik kurallarına uygun olmalıdır.";
        }
        if (msg.contains("not found")) {
            return "İstenen kayıt bulunamadı veya artık mevcut değil.";
        }
        return defaultMessage;
    }

    private static String mapGenericDomainError(Exception ex, String fallback) {
        String msg = safeLower(ex.getMessage());
        if (msg.contains("reason")) {
            return "Gerekçe alanı geçersiz.";
        }
        return fallback;
    }

    private static String safeLower(String message) {
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }
}
