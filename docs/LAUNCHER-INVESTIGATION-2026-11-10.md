# Android ortamı ve S25 Ultra ikon incelemesi — 10 Kasım 2026

## Kalıcı geliştirme ortamı

Önceki `/tmp/rotam-toolchain` mevcut değildi. Kullanıcı ortamın düzeltilmesini onayladı. Java 17.0.21, Android CLI 19.0, SDK 35, build-tools 35.0.0 ve platform-tools proje içindeki `.toolchain/` konumuna kuruldu. Bu dizin ve Gradle önbelleği Git tarafından yok sayılır. Sistem Java ayarı/Homebrew değiştirilmedi. Android Studio, emülatör, Android sistem imajı ve Xcode indirilmedi.

- Kurulum: `python3 scripts/setup_android.py --install` (yalnız Apple Silicon/macOS).
- Durum: `python3 scripts/setup_android.py`.
- Derleme: `scripts/with-android.sh ./gradlew testDebugUnitTest lintDebug assembleDebug`.
- Sonraki çevrimdışı kullanım: `scripts/with-android.sh ./gradlew --offline ...`.
- SDK yolu `local.properties` içinde güncellendi; imzalama ayarları korundu.
- Build tools sürümü açıkça 35.0.0 olarak seçildi; örtük ikinci sürüm indirmesi önlenir.
- İndirme arşivleri checksum doğrulanır ve yeniden kullanılmak üzere saklanır.

Yerel derleme/lint geçti. 28 JVM testinin gerçekten yeniden çalışması için çevrimdışı `testDebugUnitTest --rerun-tasks` çalıştırıldı ve geçti. Bu turda yerel emülatör testi yapılmadı. GitHub'da incelenen önceki son commit `9cbc881` için [42647006772](https://github.com/Nihad64/Rota/actions/runs/42647006772) çalışmasındaki verify/device/backend işleri başarılıdır; bu, yeni değişikliklerin CI sonucu değildir.

## GitHub ve paket bulguları

- `feature/rotam-3-redesign` son düzeltmeleri içerir; `main` eski uygulamadır. Main'e bakmak yeni paketi incelemekle aynı değildir. Henüz birleştirme yapılmadı.
- 3.0.3-dev (36) APK: paket `com.rota.app`, eski başlatıcı adı `com.rota.app.MainActivity`, hedef etkinlik `com.nihad64.rotam.MainActivity`.
- Gerçek derlenmiş manifestte tek MAIN/LAUNCHER vardır. Application icon, roundIcon ve alias icon bağlantıları incelendi.
- Normal/yuvarlak adaptif ikonun foreground katmanı özgün onaylı R görseline, monochrome katmanı onun maskesine bağlıdır. Kaynak ve APK içi PNG özetleri eşleşir.
- APK imza SHA-256: `04e5b446963903a0bf62e7ae6bada441fc523c3df415fd79e54c3a2bf060396d` (önceki kurulum kimliği korunuyor).
- Eski kullanılmayan drawable'ların pakette bulunması, başlatıcının onları kullandığı anlamına gelmez. Bu incelemede onlara giden başlatıcı bağlantısı bulunmadı.
- `audit_launcher.py` artık yalnız dosya varlığını değil derlenmiş manifest → adaptive icon → bitmap bağlantılarını denetler. Normal, yuvarlak ve temalı yollar kapsanır. Doğru PNG'leri içeren ama manifestte yanlış drawable'a yönelen negatif APK denemesi reddedildi.

## Fiziksel cihaz — kök neden bulundu ve düzeltildi

Kullanıcı S25 Ultra'da varsayılan ikon kullandığını ve eski ikonun hem ana ekranda hem uygulama listesinde göründüğünü belirtti. USB bağlantısıyla gerçek cihaz (SM-S938B, Android 17, Samsung One UI Home) incelendi. Kurulu APK 3.0.3-dev (36) idi; üç ikon bağlantısı ve özgün görseller bu APK'da da doğruydu. Main dalının eski olması bu telefondaki sorunun açıklaması değildi.

**Kök neden:** Samsung'un `current_sec_appicon_theme_package` sistem alanında 28 Ekim'de oluşturulmuş Theme Designer ikon katmanı etkin kaldığı görüldü; genel tema alanı ise `null` idi. Theme Park içinde Rotam'a atanmış eski özel görsel bulundu. Kullanıcının varsayılan görünüm kullandığını düşünmesiyle bu teknik durum farklıydı; baştan tema kullandığı varsayılmadı.

**Doğrudan neden-sonuç doğrulaması:** Theme Park'ta yalnız Rotam'ın özel görseli için `Reset` seçilince, uygulamaya yeni APK kurmadan onaylanan krem R ve turuncu elmas önizlemede ortaya çıktı. Eski ikon paketi korunarak kopyasına bu tekil değişiklik kaydedildi ve kopya uygulandı. Ardından hem gerçek Samsung ana ekranında hem Finder sonucunda doğru ikon görsel olarak doğrulandı. Diğer ikonların paket ayarları düzenlenmedi. Ekran görüntüleri kişisel telefon içeriği bulunduğundan yalnız yok sayılan `.toolchain/device-audit/` içinde tutulur.

Uygulama kaldırılmadı, plan veritabanına erişilmedi, telefonun ana ekran verileri/önbelleği silinmedi, uygulama kimliği veya imzası değiştirilmedi. Bu işlem bir APK hatasını gizleyen kaynak yeniden adlandırması değildir; telefonda uygulanan eski özel ikon eşlemesi kaldırılmıştır.

`inspect_phone_launcher.py`, açıkça seçilen ADB cihazından model/Android sürümü, varsayılan ana ekran, Samsung ikon katmanı ve Rotam başlatıcı kaydını okur; kurulu base APK'yı analiz için alır. Rapor ve APK yok sayılan `.toolchain/device-audit/` içinde kalır. Script kendi başına hiçbir tema veya uygulama verisi değiştirmez.

## Araştırma

- [Android activity-alias](https://developer.android.com/guide/topics/manifest/activity-alias-element): alias'ın kendi ikonu ve başlatıcı filtresi vardır.
- [Android adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive): foreground/background, roundIcon ve monochrome yolları farklı launcher davranışlarını etkiler.
- [Samsung ikon ve tema desteği](https://www.samsung.com/us/support/answer/ANS10001905/): tema ikonları ayrı uygulanabilir. Kullanıcı varsayılan kullandığını belirttiğinden tema nedeni doğrulanmış değildir.
- [Samsung Members benzer bildirim](https://r1.community.samsung.com/t5/galaxy-s/the-application-icon-remained-unchanged-after-the-ui-7-update/td-p/32826466) ve Reddit bildirimleri tarandı. Kullanıcı deneyimleri olası nedenleri gösterir; Rotam için teknik kanıt sayılmaz.
