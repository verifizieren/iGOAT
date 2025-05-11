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

Das Projekt erzeugt eine einzelne JAR-Datei, die sowohl Server als auch Client enthält. Der Server kann auch vom Client gestartet werden und muss nicht zwingend separat ausgeführt werden.

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

## Technische Details

### Projektstruktur
```
src
├── main
│   ├── java
│   │   └── igoat
│   │       ├── client
│   │       │   ├── Camera.java
│   │       │   ├── Decoration.java
│   │       │   ├── Game.java
│   │       │   ├── GUI
│   │       │   │   ├── Banner.java
│   │       │   │   ├── LobbyGUI.java
│   │       │   │   ├── Main.java
│   │       │   │   └── MainMenuGUI.java
│   │       │   ├── Map.java
│   │       │   ├── MapTest.java
│   │       │   ├── MockUDPSocket.java
│   │       │   ├── Player.java
│   │       │   ├── RealUDPSocket.java
│   │       │   ├── SceneDecoration.java
│   │       │   ├── ServerHandler.java
│   │       │   ├── Sprite.java
│   │       │   ├── SpriteSheetAnimation.java
│   │       │   ├── Terminal.java
│   │       │   ├── UDPSocket.java
│   │       │   └── Wall.java
│   │       ├── Main.java
│   │       ├── Role.java
│   │       ├── server
│   │       │   ├── ClientHandler.java
│   │       │   ├── GameState.java
│   │       │   ├── HighscoreManager.java
│   │       │   ├── Lobby.java
│   │       │   └── Server.java
│   │       └── Timer.java
```

### Verwendete Technologien
- Java 21
- JavaFX für GUI (in Entwicklung)
- Gradle als Build-System
- JUnit für Tests

## Copyright
- Sämtliche Grafik wurde vom Team selbst erstellt.
- Alle Sounds wurden unter der Creative Commons License erworben

## Team
- Jonas
- Max
- Marvin
- Nicolas

---
