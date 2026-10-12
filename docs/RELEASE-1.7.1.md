# Zegar HA Outfit 1.7.1

Wygaszacz KioskSatellite z zegarem Outfit, polską datą, temperaturą i ikoną pogody z Home Assistant oraz maksymalnie pięcioma ikonami informacyjnymi.

## Co nowego

Tryb „Pokaż cały pasek, gdy co najmniej jedna encja jest aktywna” pokazuje wszystkie skonfigurowane ikony, także nieaktywne. Gdy żadna encja nie jest aktywna, alarmowa lub w ruchu, pasek znika i zegar zajmuje pełną szerokość. Ustawienia z wersji 1.7.0 pozostają zgodne.

## Instalacja i aktualizacja

1. Pobierz `ha-outfit-clock-1.7.1.zip` z załączników wydania.
2. W KioskSatellite otwórz **Plugin Manager → Developer Tools → Install from ZIP**.
3. Zainstaluj ZIP bez odinstalowywania poprzedniej wersji. Identyfikator pluginu pozostaje ten sam.
4. Włącz plugin, wybierz encje HA i tryb wygaszacza **Zegar i pogoda (Zegar HA Outfit)**.

Plik `konfigurator-ikon.html` pobierz osobno i otwórz lokalnie w przeglądarce. Wczytaj obecną konfigurację wyglądu, zmień ustawienia i wklej wynik do pola **Nazwy, ikony i wygląd pięciu pozycji**. Encje wybiera się w aplikacji.

## Wymagania i ograniczenia

- Android 7 / API 24 lub nowszy i KioskSatellite z obsługą pluginów SDK 1 oraz wygaszaczy inline.
- Połączenie z Home Assistant skonfigurowane w KioskSatellite.
- Zegar używa czasu urządzenia i strefy Europe/Warsaw; włącz automatyczną synchronizację czasu w Androidzie.
- Aktualizacje danych HA są scalane do około 15 sekund; ikony wyłącznie pokazują stan.
- Ewentualny biały błysk zależy również od renderowania w aplikacji hosta. Osobna poprawka KioskSatellite została sprawdzona przez użytkownika w diagnostycznym APK opartym na wydaniu 2026.10.15 (313). Sam ZIP pluginu nie zawiera tej poprawki.

Kod jest objęty Apache-2.0. Czcionki i grafika zachowują własne licencje; szczegóły w dokumentacji repozytorium.
