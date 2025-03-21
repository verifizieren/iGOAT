# iGOAT

Ein Multiplayer Spiel Projekt für das Programmierprojekt an der Universität Basel.

## Projekt Links

- **OpenProject:** [Gruppe-10](https://openproject.mendes.dev/projects/gruppe-10/)
- **Guest Zugang:**
  - Benutzer: `guest`
  - Passwort: `password1234`

## Schnellstart

### Voraussetzungen

- Java 21 oder höher
- Gradle (optional, Wrapper ist im Projekt enthalten)

### Build

```bash
# Mit Gradle Wrapper
./gradlew clean build

# Oder mit lokalem Gradle
gradle clean build
```

### Ausführung

Das Projekt erzeugt eine einzelne JAR-Datei, die sowohl Server als auch Client enthält.

**Server starten:**
```bash
java -jar build/libs/iGOAT-0.1-ALPHA.jar server <port>
# Beispiel:
java -jar build/libs/iGOAT-0.1-ALPHA.jar server 8888
```

**Client starten:**
```bash
java -jar build/libs/iGOAT-0.1-ALPHA.jar client <host>:<port>
# Beispiel:
java -jar build/libs/iGOAT-0.1-ALPHA.jar client localhost:8888
```

**Main GUI Starten:**
```bash
java -jar build/libs/iGOAT-0.1-ALPHA.jar
```

### Server-Features
- Mehrere gleichzeitige Client-Verbindungen
- Automatisches Ping-Pong für Verbindungsüberwachung
- Lobby-System für Spielorganisation
- Chat-System mit öffentlichen und privaten Nachrichten

## Technische Details

### Projektstruktur
```
src/main/java/igoat/
├── Main.java           # Haupteinstiegspunkt
├── server/            
│   ├── Server.java     # Server-Implementation
│   └── ClientHandler.java  # Client-Verbindungshandling
└── client/
    ├── Client.java     # Client-Implementation
    └── ServerHandler.java  # Server-Verbindungshandling
```

### Verwendete Technologien
- Java 21
- JavaFX für GUI (in Entwicklung)
- Gradle als Build-System
- JUnit für Tests

## Team
- Jonas
- Max
- Marvin
- Nicolas

---
