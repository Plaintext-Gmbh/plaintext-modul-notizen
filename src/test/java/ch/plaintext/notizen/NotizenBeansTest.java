/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.notizen;

import ch.plaintext.PlaintextSecurity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Karte 2096: Zustände der Seiten «Meine Notizen» und «Notizen-Einstellungen». */
class NotizenBeansTest {

    final NotizSpeicher speicher = mock(NotizSpeicher.class);

    @Test
    void notizLadenUndSpeichern() throws IOException {
        when(speicher.lade()).thenReturn(Optional.of("hallo"));
        NotizenBackingBean b = new NotizenBackingBean(speicher);
        b.laden();
        assertThat(b.getText()).isEqualTo("hallo");
        assertThat(b.isOhneAblage()).isFalse();
        assertThat(b.getGespeichertUm()).isNull();

        b.setText("hallo welt");
        b.speichern();
        assertThat(b.getGespeichertUm()).matches("\\d\\d:\\d\\d:\\d\\d");
        assertThat(b.getStand()).isEqualTo(1);
        assertThat(b.getFehler()).isNull();
        assertThat(b.isFenster("1")).isTrue();
        assertThat(b.isFenster(null)).isFalse();
    }

    @Test
    void ohneAblageUndFehler() throws IOException {
        when(speicher.lade()).thenThrow(new NotizSpeicher.KeineAblageException());
        NotizenBackingBean b = new NotizenBackingBean(speicher);
        b.laden();
        assertThat(b.isOhneAblage()).isTrue();

        doThrow(new IOException("Die Notiz ist zu lang")).when(speicher).speichere(any());
        b.speichern();
        assertThat(b.getFehler()).contains("zu lang");
        assertThat(b.getStand()).isZero();
    }

    @Test
    void einstellungenPruefenUndSpeichern() throws IOException {
        PlaintextSecurity security = mock(PlaintextSecurity.class);
        when(security.getMandat()).thenReturn("default");
        when(speicher.ziel("default")).thenReturn(new NotizSpeicher.Ziel("demo", "notizen"));
        when(speicher.ablagen()).thenReturn(List.of("demo"));
        when(speicher.pruefe("demo", "notizen")).thenReturn("demo");
        when(security.ifGranted("ADMIN")).thenReturn(true);
        NotizenEinstellungenBackingBean b = new NotizenEinstellungenBackingBean(speicher, security);
        b.laden();
        assertThat(b.getAblage()).isEqualTo("demo");
        assertThat(b.getAblagen()).containsExactly("demo");

        b.pruefen();
        assertThat(b.getErgebnis()).isEqualTo("geprueft");
        assertThat(b.getMeldung()).isEqualTo("demo");

        b.speichern();
        assertThat(b.getErgebnis()).isEqualTo("gespeichert");

        when(speicher.pruefe(anyString(), anyString())).thenThrow(new IOException("nicht erreichbar"));
        b.pruefen();
        assertThat(b.getErgebnis()).isEqualTo("fehler");
        assertThat(b.getMeldung()).contains("nicht erreichbar");
    }

    /** Karte 2470: Einstellungs-Dialog liegt auf der Seite fuer alle; ohne ADMIN/ROOT nichts pruefen, nichts speichern. */
    @Test
    void einstellungenNurAdminOderRoot() throws IOException {
        PlaintextSecurity security = mock(PlaintextSecurity.class);
        when(security.getMandat()).thenReturn("default");
        when(speicher.ziel("default")).thenReturn(new NotizSpeicher.Ziel("demo", "notizen"));
        NotizenEinstellungenBackingBean b = new NotizenEinstellungenBackingBean(speicher, security);
        b.laden();
        assertThat(b.isDarf()).isFalse();
        b.speichern();
        assertThat(b.getErgebnis()).isEqualTo("fehler");
        b.pruefen();
        assertThat(b.getErgebnis()).isEqualTo("fehler");
        org.mockito.Mockito.verify(speicher, org.mockito.Mockito.never()).setzeZiel(anyString(), anyString(), anyString());
        org.mockito.Mockito.verify(speicher, org.mockito.Mockito.never()).pruefe(anyString(), anyString());

        when(security.ifGranted("ROOT")).thenReturn(true);
        assertThat(b.isDarf()).isTrue();
    }
}
