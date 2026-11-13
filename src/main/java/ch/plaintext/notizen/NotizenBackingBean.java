/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.notizen;

import jakarta.faces.context.FacesContext;
import lombok.Getter;
import org.primefaces.PrimeFaces;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Seite «Meine Notizen» (Karte 2096): ein Textfeld, das bei jeder Änderung speichert.
 *
 * <p>Das Speichern stösst {@code notizen.js} an (verzögert, sobald eine kurze Tipp-Pause entsteht) über
 * den remoteCommand {@code notizSpeichern}. Das Fenster daneben (Popup) zeigt dieselbe Notiz; kommt ein
 * Fenster in den Vordergrund, lädt es neu, damit es nicht mit altem Stand weiterschreibt.</p>
 */
@Component("notizenBean")
@Scope("session")
public class NotizenBackingBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter ZEIT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final transient NotizSpeicher speicher;

    @Getter
    @Setter
    private String text = "";

    /** Zeit der letzten erfolgreichen Sicherung, {@code null} = in dieser Sitzung noch nicht gesichert. */
    @Getter
    private String gespeichertUm;

    /** Fehlermeldung des letzten Ladens oder Sicherns, {@code null} = alles gut. */
    @Getter
    private String fehler;

    /** Keine Ablage eingerichtet: die Seite zeigt den Hinweis statt des Editors. */
    @Getter
    private boolean ohneAblage;

    /**
     * Zählt die Sicherungen dieser Sitzung. Hauptfenster und Popup teilen sich die Bean; jedes Fenster kennt
     * den Stand, den es zuletzt gesehen hat, und lädt beim Fokus nur neu, wenn das andere inzwischen
     * gespeichert hat (sonst bliebe der Cursor nicht stehen).
     */
    @Getter
    private long stand;

    public NotizenBackingBean(NotizSpeicher speicher) {
        this.speicher = speicher;
    }

    /** preRenderView: beim ersten Aufruf und bei jedem Neuladen der Seite den gespeicherten Stand holen. */
    public void laden() {
        try {
            text = speicher.lade().orElse("");
            ohneAblage = false;
            fehler = null;
        } catch (NotizSpeicher.KeineAblageException e) {
            ohneAblage = true;
            fehler = null;
        } catch (IOException | RuntimeException e) {
            fehler = e.getMessage();
        }
    }

    /** remoteCommand {@code notizSpeichern}: der Text kam eben mit dem Formular. */
    public void speichern() {
        try {
            speicher.speichere(text);
            gespeichertUm = LocalTime.now().format(ZEIT);
            stand++;
            fehler = null;
        } catch (IOException | RuntimeException e) {
            fehler = e.getMessage();
        }
    }

    /** remoteCommand {@code notizNeuLaden} beim Fokus: neu laden, nur wenn das andere Fenster gespeichert hat. */
    public void neuLadenWennNoetig() {
        String gesehen = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("stand");
        if (!String.valueOf(stand).equals(gesehen)) {
            laden();
            PrimeFaces.current().ajax().update("nf:notizText", "nf:notizStatus");
        }
    }

    /** Fenster im Popup-Modus ({@code ?fenster=1}): ohne Menü, nur der Editor. */
    public boolean isFenster(String param) {
        return "1".equals(param);
    }
}
