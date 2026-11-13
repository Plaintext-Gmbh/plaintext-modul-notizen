# plaintext-modul-notizen

Textnotizen mit Autosave für Plaintext-Anwendungen. Eine Seite «Meine Notizen» mit
Textfeld (Markdown), das bei jeder Tipp-Pause automatisch speichert, ein Button
«In eigenem Fenster öffnen» (Popup nur mit dem Editor) und eine Einstellungsseite
(ADMIN/ROOT) für Speicher-Ablage und Ordner.

Gespeichert wird über die **Speicher-Ablagen von plaintext-root** (`DateiAblagenRegister`):
jede Notiz ist eine Markdown-Datei `<ordner>/<mandant>/<benutzer>.md`. Als Provider
stehen zur Wahl, was die Anwendung einrichtet (Nextcloud, Git, Lokale Datei).

Offen, MPL-2.0.

## Einbau in eine Plaintext-App

1. **Abhängigkeit** (Version siehe Releases):

   ```xml
   <dependency>
       <groupId>ch.plaintext</groupId>
       <artifactId>plaintext-modul-notizen</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

2. **Fertig.** Das Modul meldet sich selbst (`NotizenAutoConfiguration`,
   `META-INF/spring/…AutoConfiguration.imports`): Menü-Eintrag «Notizen → Meine Notizen»
   (alle), «Notizen → Einstellungen» (ADMIN/ROOT) und eine Dashboard-Kachel.

3. **Speicher-Ablage einrichten** (ROOT unter «Root → Sidecars», MCP
   `set_speicher_ablage`): das Modul braucht ein `DateiAblagenRegister` mit mindestens
   einer Ablage. Ohne Register bzw. ohne eindeutige Ablage zeigt die Seite einen Hinweis
   statt des Editors. Für Apps ohne das Sidecars-Modul genügt ein eigenes kleines
   Register, das eine Ablage (z. B. `LokaleDateiAblage` aus root) als Bean anbietet.

4. **Optional Einstellungen je Mandant** (sonst: die einzige eingerichtete Ablage,
   Ordner `notizen`): Schlüssel `notizen.ablage` und `notizen.ordner`, setzbar auf der
   Einstellungsseite (mit «Prüfen»-Knopf: Probe schreiben, lesen, löschen).

## Entwicklung

```bash
mvn verify   # braucht Zugang zum Plaintext-Maven-Repo (GitHub Packages, Plaintext-Gmbh/plaintext-mvn)
```

Der Parent ist `plaintext-root-parent`; alle root-Teile sind `provided` — die
Zielanwendung bringt sie mit. Die Modul-Version ist unabhängig von der root-Version.
