# DFAD
Driver Facial Anomaly Detector

# Struktura pakietów
* camera - logika odpowiedzialna za wykrywanie twarzy
* detector - moduł uczenia maszynowego odpowiedzialnego za wykrywanie anomalii
* gsm - logika nasłuchująca na ruch GSM
* helper - klasy pomocnicze używane w pozostałych modułach
* model - klasy reprezentujące model danych
* notification - serwis obsługujący notyfikacje dźwiękowe
* MainActivity - główny widok projektu; klasa obsługuje także odczyt z sensorów urządzenia

# Uruchamianie projektu
W celu uruchomienia projektu należy wygenerować plik .apk ze źródłeł projektu (np. przy użyciu Android Studio), a następnie uruchomić ten plik na urządzeniu docelowym. Spowoduje to instalację aplikacji, która po uruchomieniu i zezwoleniu na korzystanie z określonych zasobów urządzenia nie wymaga żadnej dodatkowej konfiguracji.