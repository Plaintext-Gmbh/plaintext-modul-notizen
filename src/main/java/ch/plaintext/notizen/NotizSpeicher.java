/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.notizen;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.ablagen.DateiAblage;
import ch.plaintext.ablagen.DateiAblagenRegister;
import ch.plaintext.settings.ISettingsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Liest und schreibt die Textnotiz des angemeldeten Benutzers (Karte 2096).
 *
 * <p>Gespeichert wird über die Speicher-Ablagen von plaintext-root ({@link DateiAblagenRegister}):
 * welche Ablage und welcher Ordner, steht in den Einstellungen {@value #SCHLUESSEL_ABLAGE} und
 * {@value #SCHLUESSEL_ORDNER} je Mandant. Ohne Einstellung gilt die einzige eingerichtete Ablage.
 * Jede Notiz ist eine Markdown-Datei {@code <ordner>/<mandant>/<benutzer>.md}.</p>
 */
@Service
public class NotizSpeicher {

    static final String SCHLUESSEL_ABLAGE = "notizen.ablage";
    static final String SCHLUESSEL_ORDNER = "notizen.ordner";
    static final String STANDARD_ORDNER = "notizen";
    /** Obergrenze einer Notiz in Zeichen; schützt die Ablage vor versehentlich eingefügten Riesentexten. */
    static final int MAX_ZEICHEN = 500_000;

    private final ObjectProvider<DateiAblagenRegister> register;
    private final ObjectProvider<ISettingsService> settings;
    private final PlaintextSecurity security;

    public NotizSpeicher(ObjectProvider<DateiAblagenRegister> register, ObjectProvider<ISettingsService> settings,
                         PlaintextSecurity security) {
        this.register = register;
        this.settings = settings;
        this.security = security;
    }

    /** Wohin die Notizen eines Mandanten gehen. {@code ablage} ist {@code null}, wenn keine bestimmbar ist. */
    public record Ziel(String ablage, String ordner) {
    }

    /** @return die eingerichteten Speicher-Ablagen, leer ohne Register */
    public List<String> ablagen() {
        DateiAblagenRegister r = register.getIfAvailable();
        return r == null ? List.of() : r.namen();
    }

    public Ziel ziel(String mandat) {
        ISettingsService s = settings.getIfAvailable();
        String ablage = s == null ? null : leerAlsNull(s.getString(SCHLUESSEL_ABLAGE, mandat));
        String ordner = s == null ? null : leerAlsNull(s.getString(SCHLUESSEL_ORDNER, mandat));
        if (ablage == null) {
            List<String> alle = ablagen();
            ablage = alle.size() == 1 ? alle.get(0) : null;
        }
        return new Ziel(ablage, ordner == null ? STANDARD_ORDNER : ordner);
    }

    public void setzeZiel(String mandat, String ablage, String ordner) {
        ISettingsService s = settings.getIfAvailable();
        if (s == null) {
            throw new IllegalStateException("Einstellungen sind in dieser Anwendung nicht verfügbar.");
        }
        s.setSetting(SCHLUESSEL_ABLAGE, mandat, ablage == null ? "" : ablage.strip(), "STRING", "Notizen: Speicher-Ablage");
        s.setSetting(SCHLUESSEL_ORDNER, mandat, ordner == null || ordner.isBlank() ? STANDARD_ORDNER : ordner.strip(),
                "STRING", "Notizen: Ordner in der Ablage");
    }

    /** Die Notiz des angemeldeten Benutzers; leer, wenn er noch keine hat. */
    public Optional<String> lade() throws IOException {
        return ladeFuer(security.getMandat(), security.getUser());
    }

    public void speichere(String text) throws IOException {
        speichereFuer(security.getMandat(), security.getUser(), text);
    }

    public Optional<String> ladeFuer(String mandat, String benutzer) throws IOException {
        DateiAblage a = ablage(mandat);
        String pfad = pfad(ziel(mandat), mandat, benutzer);
        if (!a.existiert(pfad)) {
            return Optional.empty();
        }
        return Optional.of(new String(a.lies(pfad), StandardCharsets.UTF_8));
    }

    /** Auch für Anwendungen, die eine Notiz vorbelegen (z.B. die Beispielnotiz der Demo). */
    public void speichereFuer(String mandat, String benutzer, String text) throws IOException {
        String t = text == null ? "" : text;
        if (t.length() > MAX_ZEICHEN) {
            throw new IOException("Die Notiz ist zu lang (höchstens " + MAX_ZEICHEN + " Zeichen).");
        }
        ablage(mandat).schreibe(pfad(ziel(mandat), mandat, benutzer), t.getBytes(StandardCharsets.UTF_8), "text/markdown");
    }

    /** «Prüfen» auf der Einstellungsseite: Probe schreiben, lesen, löschen. */
    public String pruefe(String ablageName, String ordner) throws IOException {
        DateiAblage a = oeffne(ablageName);
        String probe = segmente(ordner == null || ordner.isBlank() ? STANDARD_ORDNER : ordner) + "/.notizen-probe";
        a.schreibe(probe, "ok".getBytes(StandardCharsets.UTF_8), "text/plain");
        try {
            if (!"ok".equals(new String(a.lies(probe), StandardCharsets.UTF_8))) {
                throw new IOException("Probe nicht lesbar.");
            }
        } finally {
            a.loesche(probe);
        }
        return a.name();
    }

    private DateiAblage ablage(String mandat) throws IOException {
        Ziel z = ziel(mandat);
        if (z.ablage() == null) {
            throw new KeineAblageException();
        }
        return oeffne(z.ablage());
    }

    private DateiAblage oeffne(String name) throws IOException {
        DateiAblagenRegister r = register.getIfAvailable();
        if (r == null) {
            throw new KeineAblageException();
        }
        return r.ablage(name).orElseThrow(() -> new IOException("Die Ablage «" + name + "» ist nicht erreichbar."));
    }

    static String pfad(Ziel z, String mandat, String benutzer) {
        return segmente(z.ordner()) + "/" + segment(mandat == null ? "ohne-mandant" : mandat) + "/"
                + segment(benutzer == null ? "anonym" : benutzer) + ".md";
    }

    /** Ordner aus den Einstellungen: Segmente einzeln bereinigt, nie {@code ..}. */
    static String segmente(String ordner) {
        StringBuilder b = new StringBuilder();
        for (String teil : ordner.split("/")) {
            if (!teil.isBlank()) {
                b.append(b.isEmpty() ? "" : "/").append(segment(teil));
            }
        }
        return b.isEmpty() ? STANDARD_ORDNER : b.toString();
    }

    /** Ein Dateiname-Segment: nur Buchstaben, Ziffern und {@code . _ @ -}, nie mit Punkt am Anfang. */
    static String segment(String s) {
        String t = s.strip().replaceAll("[^\\p{L}\\p{N}._@-]", "_");
        while (t.startsWith(".")) {
            t = "_" + t.substring(1);
        }
        return t.isEmpty() ? "_" : t;
    }

    private static String leerAlsNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    /** Keine Ablage bestimmbar: weder Einstellung noch genau eine eingerichtete Ablage. */
    public static final class KeineAblageException extends IOException {
        private static final long serialVersionUID = 1L;

        public KeineAblageException() {
            super("Keine Speicher-Ablage für Notizen eingerichtet.");
        }
    }
}
