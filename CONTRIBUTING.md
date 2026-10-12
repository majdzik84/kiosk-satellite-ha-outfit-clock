# Współtworzenie

Zmiany pluginu zgłaszaj jako issue lub pull request. Do opisu problemu dodaj wersję pluginu, KioskSatellite, Androida, model tabletu, kroki odtworzenia i oczekiwany wynik.

Nie publikuj tokenów HA, haseł ani eksportu konfiguracji aplikacji. Przykłady encji i nazwy urządzeń zanonimizuj.

## Weryfikacja zmian

Wymagane narzędzia: Python 3, Node.js 20+ i JDK 17+.

```sh
python tools/test-clock.py
node tools/test-renderer.cjs
python tools/build-configurator.py
node tools/test-configurator.cjs
```

Zmieniaj źródła w `src`, `assets/clock`, `resources/custom-icons.json` i `tools/icon-configurator.html`. Pliki osadzonych rendererów i konfigurator wynikowy są generowane przez narzędzia.

Budowanie ZIP wymaga także Android SDK platform 35, build-tools 35.0.0 oraz ustawionych `JAVA_HOME` i `ANDROID_HOME`:

```sh
python tools/build.py --android-platform 35
python tools/verify-package.py
```

Testy lokalne nie zastępują testu renderowania na tablecie.
