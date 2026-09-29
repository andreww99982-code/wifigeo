# WifiSpoofer — LSPosed-модуль (Android 10–15)

Сборка: открыть папку в Android Studio (Ladybug+, JDK 17) → Build → Build APK(s).
gradle-wrapper.jar/gradlew не включены — Android Studio подтянет Gradle 8.9 сам
(или выполните `gradle wrapper` в корне).

Установка: APK → LSPosed → включить модуль → выбрать scope → перезагрузка → в приложении «Синхронизировать эфир».

## Что исправлено по сравнению с исходным гайдом
- settings.gradle.kts: dependencyResolution → dependencyResolutionManagement
- MODE_WORLD_READABLE (крашит на targetSdk ≥ 24) → MODE_PRIVATE + meta-data `xposedsharedprefs` и xposedminversion=93 (нужно LSPosed для XSharedPreferences)
- Убраны package= из манифеста, makeWorldReadable(), зависимость org.json, лишний JSON-файл
- Добавлен app/proguard-rules.pro
- Сетевые вызовы в WifiDataFetcher — обычные функции (вызываются из Dispatchers.IO)
- WifiSsid: fromBytes вместо несуществующего createFromByteArray
- item_app.xml: высота wrap_content (двухстрочный текст)

## Сборка через GitHub Actions
1. Создайте репозиторий на GitHub, залейте содержимое этой папки (включая скрытую .github).
2. Вкладка Actions → Build APK (запускается автоматически или кнопкой Run workflow).
3. По завершении откройте запуск → раздел Artifacts → WifiSpoofer-debug-apk.
