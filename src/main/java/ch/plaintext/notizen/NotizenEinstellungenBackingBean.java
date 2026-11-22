/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.notizen;

import ch.plaintext.PlaintextSecurity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Seite «Notizen-Einstellungen» (Karte 2096): welche Speicher-Ablage und welcher Ordner, mit Prüfen-Knopf.
 * Die Ablagen selbst (Nextcloud, Git, Lokale Datei) richtet ROOT unter «Root → Sidecars» ein.
 */
@Component("notizenEinstellungenBean")
@Scope("session")
public class NotizenEinstellungenBackingBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final transient NotizSpeicher speicher;
    private final transient PlaintextSecurity security;

    @Getter
    @Setter
    private String ablage;

    @Getter
    @Setter
    private String ordner;

    /** {@code geprueft}, {@code gespeichert} oder {@code fehler}; die Seite wählt danach den Text. */
    @Getter
    private String ergebnis;

    /** Ablage-Name bzw. Fehlermeldung zum Ergebnis. */
    @Getter
    private String meldung;

    public NotizenEinstellungenBackingBean(NotizSpeicher speicher, PlaintextSecurity security) {
        this.speicher = speicher;
        this.security = security;
    }

    public void laden() {
        NotizSpeicher.Ziel z = speicher.ziel(security.getMandat());
        ablage = z.ablage();
        ordner = z.ordner();
        ergebnis = null;
        meldung = null;
    }

    /** Karte 2470: die Einstellungen sind ein Dialog auf der Notizen-Seite (fuer alle), darum hier ADMIN/ROOT pruefen. */
    public boolean isDarf() {
        return security.ifGranted("ADMIN") || security.ifGranted("ROOT");
    }

    private boolean verweigert() {
        if (isDarf()) {
            return false;
        }
        ergebnis = "fehler";
        meldung = "Kein Zugriff";
        return true;
    }

    public List<String> getAblagen() {
        return speicher.ablagen();
    }

    public void pruefen() {
        if (verweigert()) {
            return;
        }
        try {
            meldung = speicher.pruefe(ablage, ordner);
            ergebnis = "geprueft";
        } catch (IOException | RuntimeException e) {
            ergebnis = "fehler";
            meldung = e.getMessage();
        }
    }

    public void speichern() {
        if (verweigert()) {
            return;
        }
        try {
            speicher.setzeZiel(security.getMandat(), ablage, ordner);
            laden();
            ergebnis = "gespeichert";
            meldung = ablage;
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Erwartete Fehler anzeigen; alles andere (z.B. Schreibschutz der App) dem
            // JSF-ExceptionHandler überlassen, der zeigt den übersetzten Hinweis der App.
            ergebnis = "fehler";
            meldung = e.getMessage();
        }
    }
}
