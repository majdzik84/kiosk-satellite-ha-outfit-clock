# Instalacja i konfiguracja krok po kroku

## 1. Przygotowanie aplikacji

Zainstaluj KioskSatellite obsługujący pluginy SDK 1 oraz wygaszacze inline. Skonfiguruj połączenie aplikacji z Home Assistant. Minimalny Android pluginu to 7 / API 24; sama wersja Androida nie gwarantuje zgodności wszystkich funkcji hosta.

W ustawieniach Androida włącz automatyczną datę i godzinę. Zegar korzysta z czasu urządzenia, a nie z encji czasu w HA. Format daty jest polski, strefa czasu to Europe/Warsaw, z uwzględnieniem zmiany czasu letniego i zimowego.

## 2. Instalacja ZIP

Pobierz `ha-outfit-clock-1.7.1.zip` z wydania, nie archiwum „Source code”. Otwórz **Plugin Manager → Developer Tools → Install from ZIP**, wskaż paczkę i włącz plugin oraz główny przełącznik pluginów.

Następnie wybierz **Screensaver → Screensaver mode → Zegar i pogoda (Zegar HA Outfit)**. Ten tryb może być również używany w harmonogramie wygaszaczy.

Aktualizacja odbywa się przez instalację nowszego ZIP z tym samym identyfikatorem pluginu. Nie odinstalowuj poprzedniej wersji, jeśli chcesz zachować jej ustawienia. Przed zmianą skopiuj wartość pola wyglądu ikon, jeśli chcesz mieć własną kopię.

## 3. Dane pogodowe

| Ustawienie | Co wybrać |
| --- | --- |
| Encja temperatury | Sensor temperatury albo `weather.*`; dla encji weather odczytywany jest atrybut `temperature`. |
| Encja warunków pogodowych | Encja weather, której stan określa ikonę. |
| Encja słońca | Zwykle `sun.sun`, aby wybierać wariant dzienny/nocny. |
| Miejsca po przecinku | 0 albo 1; separator wyświetlanej temperatury jest kropką. |

Domyślne encje są przykładami; wybierz własne. Jednostka temperatury pochodzi z encji. Plugin pokazuje bieżącą temperaturę i ikonę warunków, nie wielodniową prognozę.

## 4. Wygląd zegara

Rozmiary zegara, daty, temperatury i ikony pogody regulowane są osobno w zakresie 50–200%. Dostępne są kolory tła, zegara, daty, temperatury i cyfry 1. Wyróżnianie dotyczy jedynek w części godzinowej HH; minuty pozostają w kolorze zegara. Można je wyłączyć.

Czcionka Outfit jest dostarczana w paczce. Układ automatycznie dopasowuje się do dostępnego miejsca; długa data lub duże podpisy mogą ograniczyć rozmiar całego zegara. Ochronę przez przesuwanie pikseli włącza się w KioskSatellite.

## 5. Pięć pozycji informacyjnych

W ustawieniach pluginu wybierz od zera do pięciu encji. Puste pozycje nie zajmują miejsca. Do bram używaj encji cover lub odpowiedniego czujnika otwarcia, do zamków lock, do okien czujników z właściwym device_class.

Ikony nie są przyciskami i nie sterują urządzeniami. Odryglowany zamek nie oznacza potwierdzonego otwarcia drzwi.

### Konfigurator offline

1. Pobierz `konfigurator-ikon.html` i otwórz go lokalnie w przeglądarce.
2. Wczytaj aktualną wartość pola **Nazwy, ikony i wygląd pięciu pozycji**, aby zachować istniejące ustawienia.
3. Zmień nazwy, ikony, wielkości ikon i podpisów oraz kolory aktywne i nieaktywne.
4. Wybierz układ i sposób pokazywania paska.
5. Skopiuj wynik do pola ustawień pluginu w KioskSatellite.

Konfigurator nie łączy się z HA ani aplikacją i sam nie zapisuje zmian w tablecie. Numery pozycji odpowiadają numerom wyborów encji w pluginie, również gdy któraś pozycja jest pusta.

### Układ i widoczność

| Tryb | Zachowanie |
| --- | --- |
| Po lewej | Pionowy pasek dopasowuje szerokość do zawartości; długie nazwy mogą się zawijać. |
| Na dole | Ikony są w poziomym rzędzie pod pogodą. |
| Ikony wyłączone | Pasek znika bez usuwania ustawień, a zegar korzysta z pełnej szerokości. |
| Cały pasek, gdy dowolna encja aktywna | Widać wszystkie skonfigurowane pozycje, również nieaktywne, jeśli co najmniej jedna jest aktywna, alarmowa lub w ruchu. Gdy wszystkie są nieaktywne, pasek znika. |

### Wybór ikon i stany

Pierwszeństwo ma ręczna ikona, następnie atrybut `icon` z HA, rozpoznanie domeny/device_class i ikona zastępcza. Lokalny zestaw MDI nie wymaga pobierania ikon z internetu. Ikona ustawiona wyłącznie w karcie dashboardu nie musi być dostępna jako atrybut encji; w takim przypadku wybierz ją ręcznie.

Własne kontury: `custom:gate-left`, `custom:garage`, `custom:window`, `custom:lock`. Brama wjazdowa ma poziome przęsła, grubszy prawy słupek i skrzydło otwierane w lewo.

Stany open/on wybierają wariant otwarty, closed/off zamknięty, a locked/unlocked wariant zaryglowany/odryglowany. Ruch i alarm używają koloru aktywnego. Domyślny kolor nieaktywny to grafitowy `#444444`. Nieznany stan pokazuje znak zapytania.

## 6. Aktualizacja danych i brak połączenia

Zmiany danych HA są scalane do około 15 sekund; ekran nie służy jako natychmiastowy panel alarmowy. Zegar przelicza czas co sekundę i po wznowieniu. Przy pierwszym uruchomieniu kreski mogą pozostać do nadejścia danych HA.

Ostatnie odczyty pogodowe zachowywane są w pamięci sesji i oznaczane jako nieaktualne po utracie dostępności. Nie są zachowywane po restarcie. Brak danych encji informacyjnej sygnalizowany jest ikoną nieznanego stanu.

## 7. Biały błysk

Plugin ustawia czarne tło i dostarcza czcionki oraz grafikę lokalnie, ale ustawienia natywnego WebView są kontrolowane przez KioskSatellite. Wydanie 1.7.1 nie zawiera poprawki kodu aplikacji.

W diagnostycznym APK opartym na KioskSatellite 2026.10.15 (313) dodano przezroczyste tło WebView i czarne tło iframe. Użytkownik potwierdził usunięcie błysku w swoim teście. Nie oznacza to potwierdzenia na wszystkich urządzeniach ani w wydaniu produkcyjnym. Testowy APK nie jest dystrybuowany jako część pluginu.

## 8. Zgłaszanie problemów

Podaj wersję pluginu, KioskSatellite, Androida, WebView, model tabletu, kroki odtworzenia i wynik oczekiwany. Do problemów renderowania pomocne są nagranie oraz logi z chwili zdarzenia. Nie załączaj tokenów, haseł ani pełnego eksportu konfiguracji aplikacji.
