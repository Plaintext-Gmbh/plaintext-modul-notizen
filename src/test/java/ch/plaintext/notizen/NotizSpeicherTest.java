/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.notizen;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.ablagen.AblageEintrag;
import ch.plaintext.ablagen.DateiAblage;
import ch.plaintext.ablagen.DateiAblagenRegister;
import ch.plaintext.settings.ISettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Karte 2096: Notizen speichern über die Speicher-Ablagen von root. */
class NotizSpeicherTest {

    /** Eine Ablage im Speicher, wie sie das Register liefert. */
    static final class Speicher implements DateiAblage {
        final Map<String, byte[]> dateien = new LinkedHashMap<>();
        final String name;

        Speicher(String name) {
            this.name = name;
        }

        @Override public String name() { return name; }
        @Override public void schreibe(String pfad, byte[] daten, String inhaltTyp) { dateien.put(pfad, daten); }
        @Override public byte[] lies(String pfad) throws IOException {
            byte[] d = dateien.get(pfad);
            if (d == null) throw new IOException("Datei nicht gefunden.");
            return d;
        }
        @Override public boolean existiert(String pfad) { return dateien.containsKey(pfad); }
        @Override public List<AblageEintrag> liste(String ordner) { return List.of(); }
        @Override public void loesche(String pfad) { dateien.remove(pfad); }
        @Override public void legeOrdnerAn(String pfad) { }
        @Override public void verschiebe(String von, String nach) { }
        @Override public void loescheOrdner(String pfad, boolean rekursiv) { }
    }

    final Map<String, Speicher> ablagen = new LinkedHashMap<>();
    final Map<String, String> einstellungen = new HashMap<>();
    ISettingsService settings;
    PlaintextSecurity security;
    boolean mitRegister = true;

    @SuppressWarnings("unchecked")
    NotizSpeicher speicher() {
        DateiAblagenRegister register = new DateiAblagenRegister() {
            @Override public List<String> namen() { return List.copyOf(ablagen.keySet()); }
            @Override public Optional<DateiAblage> ablage(String name) { return Optional.ofNullable(ablagen.get(name)); }
        };
        ObjectProvider<DateiAblagenRegister> rp = mock(ObjectProvider.class);
        when(rp.getIfAvailable()).thenAnswer(i -> mitRegister ? register : null);
        ObjectProvider<ISettingsService> sp = mock(ObjectProvider.class);
        when(sp.getIfAvailable()).thenReturn(settings);
        return new NotizSpeicher(rp, sp, security);
    }

    @BeforeEach
    void setUp() {
        settings = mock(ISettingsService.class);
        when(settings.getString(anyString(), anyString())).thenAnswer(i -> einstellungen.get(i.getArgument(1) + "/" + i.getArgument(0)));
        security = mock(PlaintextSecurity.class);
        when(security.getMandat()).thenReturn("default");
        when(security.getUser()).thenReturn("anna@example.ch");
    }

    @Test
    void ohneEinstellungGiltDieEinzigeAblage_undJederBenutzerHatSeineDatei() throws IOException {
        ablagen.put("demo-notizen", new Speicher("demo-notizen"));
        NotizSpeicher s = speicher();
        assertThat(s.lade()).isEmpty();
        s.speichere("# Einkauf\n- Brot");
        assertThat(ablagen.get("demo-notizen").dateien).containsKey("notizen/default/anna@example.ch.md");
        assertThat(s.lade()).contains("# Einkauf\n- Brot");
        assertThat(s.ladeFuer("default", "bob")).isEmpty();
    }

    @Test
    void einstellungWaehltAblageUndOrdner() throws IOException {
        ablagen.put("a", new Speicher("a"));
        ablagen.put("b", new Speicher("b"));
        einstellungen.put("default/" + NotizSpeicher.SCHLUESSEL_ABLAGE, "b");
        einstellungen.put("default/" + NotizSpeicher.SCHLUESSEL_ORDNER, "team/notizen");
        NotizSpeicher s = speicher();
        assertThat(s.ziel("default")).isEqualTo(new NotizSpeicher.Ziel("b", "team/notizen"));
        s.speichere("x");
        assertThat(ablagen.get("b").dateien).containsKey("team/notizen/default/anna@example.ch.md");
        assertThat(ablagen.get("a").dateien).isEmpty();
    }

    @Test
    void mehrereAblagenOhneEinstellung_oderOhneRegister_heisstKeineAblage() {
        ablagen.put("a", new Speicher("a"));
        ablagen.put("b", new Speicher("b"));
        assertThatThrownBy(() -> speicher().lade()).isInstanceOf(NotizSpeicher.KeineAblageException.class);
        mitRegister = false;
        NotizSpeicher ohne = speicher();
        assertThat(ohne.ablagen()).isEmpty();
        assertThatThrownBy(() -> ohne.speichere("x")).isInstanceOf(NotizSpeicher.KeineAblageException.class);
    }

    @Test
    void zuLangUndPfadsegmenteBereinigt() {
        ablagen.put("a", new Speicher("a"));
        NotizSpeicher s = speicher();
        assertThatThrownBy(() -> s.speichere("x".repeat(NotizSpeicher.MAX_ZEICHEN + 1))).hasMessageContaining("zu lang");
        assertThat(NotizSpeicher.pfad(new NotizSpeicher.Ziel("a", "../ab/./c"), "m/x", "..hans wurst"))
                .isEqualTo("_./ab/_/c/m_x/_.hans_wurst.md");
        assertThat(NotizSpeicher.segmente("  /  ")).isEqualTo(NotizSpeicher.STANDARD_ORDNER);
    }

    @Test
    void pruefenSchreibtLiestUndLoeschtEineProbe() throws IOException {
        Speicher a = new Speicher("a");
        ablagen.put("a", a);
        assertThat(speicher().pruefe("a", "")).isEqualTo("a");
        assertThat(a.dateien).isEmpty();
        assertThatThrownBy(() -> speicher().pruefe("fehlt", "x")).hasMessageContaining("nicht erreichbar");
    }

    @Test
    void zielSetzenSchreibtBeideEinstellungen() {
        NotizSpeicher s = speicher();
        s.setzeZiel("default", " a ", " ");
        verify(settings).setSetting(eq(NotizSpeicher.SCHLUESSEL_ABLAGE), eq("default"), eq("a"), eq("STRING"), anyString());
        verify(settings).setSetting(eq(NotizSpeicher.SCHLUESSEL_ORDNER), eq("default"), eq(NotizSpeicher.STANDARD_ORDNER), eq("STRING"), anyString());
    }
}
