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

1. **Abhängigkeit** (ab 1.1.0 in Reposilite `https://maven.plaintext.ch/releases`, root ≥ 3.58.0;
   1.0.x liegen auf GitHub Packages):

   ```xml
   <dependency>
       <groupId>ch.plaintext</groupId>
       <artifactId>plaintext-modul-notizen</artifactId>
       <version>1.1.0</version>
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
mvn verify   # liest root-v2 aus Reposilite (maven.plaintext.ch, nur im LAN), ohne Anmeldung
```

Der Parent ist `plaintext-root-parent`; alle root-Teile sind `provided` — die
Zielanwendung bringt sie mit. Die Modul-Version ist unabhängig von der root-Version.

## Release

Git-Tag `X.Y.Z` pushen: `.woodpecker/release.yml` ruft `ci/release.sh` und legt das Jar in
Reposilite ab (Secret `maven_nas_token`, nur Ereignis tag). Versionen sind unveränderlich;
`ci/release.sh --trocken` prüft vorher, ob die Version frei ist.
