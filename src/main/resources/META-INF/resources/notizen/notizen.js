/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
/*
 * Karte 2096: Verhalten der Seite «Meine Notizen». Alles über Listener am document (CSP ohne
 * 'unsafe-inline', und es überlebt Teil-Updates von PrimeFaces):
 *  - Autosave: nach jeder Änderung, sobald 800 ms nicht mehr getippt wird (remoteCommand notizSpeichern).
 *  - Mini-Editor (Karte 2413, Regeln Daniel): Fett = Sternzeile darunter, Kursiv, Punkt «- »,
 *    Unterpunkt « - », Datum JJ-MM-TT, Markierungen [x] und [.] am Zeilenanfang. Derselbe Knopf auf
 *    einer Zeile mit dieser Form nimmt sie wieder weg (ausser Datum); Punkt und Unterpunkt wechseln ineinander.
 *    Die Textregeln sind reine Funktionen (REGELN), getestet in src/test/js/notizen.test.js.
 *  - «In eigenem Fenster öffnen»: dieselbe Seite mit ?fenster=1.
 *  - Fokus: hat das andere Fenster inzwischen gespeichert, neu laden (remoteCommand notizNeuLaden).
 */
(function () {
    'use strict';

    var PAUSE_MS = 800;
    var timer = null;

    var MARKE = /^\[[x.]\] /;

    /** Ganze Zeilen der Auswahl; eine Auswahl, die direkt nach einem Zeilenumbruch endet, nimmt die Folgezeile nicht mit. */
    function zeilen(w, a, e) {
        var start = w.lastIndexOf('\n', a - 1) + 1;
        var bis = e > a && w.charAt(e - 1) === '\n' ? e - 1 : e;
        var ende = w.indexOf('\n', bis);
        return {start: start, ende: ende < 0 ? w.length : ende};
    }

    /** Wendet fn auf jede gewählte Zeile an (fn liefert eine oder mehrere Zeilen) und wählt den neuen Block. */
    function jeZeile(fn) {
        return function (w, a, e) {
            var z = zeilen(w, a, e);
            var alt = w.substring(z.start, z.ende).split('\n');
            // Folgezeile auch über die Auswahl hinaus, damit fett eine vorhandene Sternzeile erkennt.
            var danach = z.ende < w.length ? w.substring(z.ende + 1).split('\n')[0] : undefined;
            var neu = [];
            alt.forEach(function (zeile, i) {
                neu = neu.concat(fn(zeile, i + 1 < alt.length ? alt[i + 1] : danach));
            });
            var block = neu.join('\n');
            return {text: w.substring(0, z.start) + block + w.substring(z.ende), a: z.start, e: z.start + block.length};
        };
    }

    function umlegen(vor, nach) {
        return function (w, a, e) {
            var mitte = w.substring(a, e);
            return {text: w.substring(0, a) + vor + mitte + nach + w.substring(e),
                a: a + vor.length, e: a + vor.length + mitte.length};
        };
    }

    /**
     * Umschalten (Wunsch Daniel 21.11.2026): Haben schon alle gewählten Zeilen die Form (hat), nimmt
     * derselbe Knopf sie weg (weg); sonst setzt er sie (an).
     */
    function umschalten(hat, weg, an) {
        return function (w, a, e) {
            var z = zeilen(w, a, e);
            var alle = w.substring(z.start, z.ende).split('\n').every(hat);
            return jeZeile(alle ? weg : an)(w, a, e);
        };
    }

    function beginnt(vor) {
        return function (z) {
            return z.indexOf(vor) === 0;
        };
    }

    function markiere(marke) {
        return umschalten(beginnt(marke), function (z) {
            return z.substring(marke.length);
        }, function (z) {
            return marke + z.replace(MARKE, '');
        });
    }

    var STERNE = /^\*+$/;

    /**
     * Fett: darunter Sterne so breit wie die Zeile, danach eine Leerzeile. Sind alle gewählten Textzeilen
     * schon unterstrichen, nimmt der Knopf Sternzeile und folgende Leerzeile wieder weg.
     */
    function fett(w, a, e) {
        var z = zeilen(w, a, e);
        var vorher = w.substring(0, z.start).split('\n').length - 1;
        var alle = w.split('\n');
        var bis = vorher + w.substring(z.start, z.ende).split('\n').length - 1;
        function text(i) {
            return alle[i].trim() !== '' && !STERNE.test(alle[i]);
        }
        function unterstrichen(i) {
            return i + 1 < alle.length && STERNE.test(alle[i + 1]);
        }
        var weg = true;
        for (var j = vorher; j <= bis; j++) {
            if (text(j) && !unterstrichen(j)) {
                weg = false;
            }
        }
        var neu = [];
        var i = vorher;
        for (; i <= bis; i++) {
            neu.push(alle[i]);
            if (!text(i)) {
                continue;
            }
            if (weg) {
                i++;                                        // Sternzeile
                if (i + 1 < alle.length && alle[i + 1] === '') {
                    i++;                                    // Leerzeile danach
                }
            } else if (!unterstrichen(i)) {
                // ponytail: Breite in UTF-16-Einheiten; Emoji zählen doppelt. Erst ändern, wenn das stört.
                neu.push(new Array(alle[i].length + 1).join('*'), '');
            }
        }
        var block = neu.join('\n');
        var nachher = alle.slice(i).join('\n');
        var vorn = w.substring(0, z.start);
        return {text: vorn + block + (i < alle.length ? '\n' + nachher : ''), a: z.start, e: z.start + block.length};
    }

    /** Kursiv: Sterne um die Auswahl; stehen schon Sterne darum (oder an ihren Enden), kommen sie weg. */
    function kursiv(w, a, e) {
        if (a > 0 && w.charAt(a - 1) === '*' && w.charAt(e) === '*') {
            return {text: w.substring(0, a - 1) + w.substring(a, e) + w.substring(e + 1), a: a - 1, e: e - 1};
        }
        if (e - a >= 2 && w.charAt(a) === '*' && w.charAt(e - 1) === '*') {
            return {text: w.substring(0, a) + w.substring(a + 1, e - 1) + w.substring(e), a: a, e: e - 2};
        }
        return umlegen('*', '*')(w, a, e);
    }

    function heute(d) {
        function zwei(n) {
            return (n < 10 ? '0' : '') + n;
        }
        return zwei(d.getFullYear() % 100) + '-' + zwei(d.getMonth() + 1) + '-' + zwei(d.getDate());
    }

    var REGELN = {
        fett: fett,
        kursiv: kursiv,
        // Punkt «- » und Unterpunkt « - » wechseln ineinander; derselbe Knopf nochmals nimmt die Form weg.
        punkt: umschalten(beginnt('- '), function (z) {
            return z.substring(2);
        }, function (z) {
            if (z.indexOf(' - ') === 0) {
                return z.substring(1);
            }
            return z.indexOf('- ') === 0 ? z : '- ' + z;
        }),
        unterpunkt: umschalten(beginnt(' - '), function (z) {
            return z.substring(3);
        }, function (z) {
            return z.indexOf('- ') === 0 ? ' ' + z : ' - ' + z;
        }),
        erledigt: markiere('[x] '),
        offen: markiere('[.] '),
        datum: function (w, a, e, jetzt) {
            var d = heute(jetzt || new Date());
            return {text: w.substring(0, a) + d + w.substring(e), a: a + d.length, e: a + d.length};
        }
    };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = REGELN;
    }
    if (typeof document === 'undefined') {
        return;
    }

    function textfeld() {
        return document.querySelector('.notizen-text');
    }

    function status() {
        return document.querySelector('.notizen-status');
    }

    function zeigeSpeichert() {
        var wurzel = document.querySelector('.notizen');
        var s = status();
        if (wurzel && s) {
            s.textContent = wurzel.getAttribute('data-text-speichert');
            s.classList.remove('notizen-fehler');
        }
    }

    function sichere() {
        timer = null;
        if (typeof window.notizSpeichern === 'function') {
            window.notizSpeichern();
        }
    }

    function plane() {
        zeigeSpeichert();
        clearTimeout(timer);
        timer = setTimeout(sichere, PAUSE_MS);
    }

    function formatiere(t, regel) {
        var r = regel(t.value, t.selectionStart, t.selectionEnd);
        t.value = r.text;
        t.setSelectionRange(r.a, r.e);
        t.focus();
        plane();
    }

    document.addEventListener('input', function (ev) {
        if (ev.target && ev.target.classList && ev.target.classList.contains('notizen-text')) {
            plane();
        }
    });

    document.addEventListener('click', function (ev) {
        var k = ev.target && ev.target.closest ? ev.target.closest('[data-format],[data-fenster]') : null;
        if (!k || !k.closest('.notizen')) {
            return;
        }
        ev.preventDefault();
        if (k.hasAttribute('data-fenster')) {
            window.open(window.location.pathname + '?fenster=1', 'plaintext-notizen', 'width=760,height=820');
            return;
        }
        var t = textfeld();
        var f = REGELN[k.getAttribute('data-format')];
        if (t && f) {
            formatiere(t, f);
        }
    });

    document.addEventListener('keydown', function (ev) {
        if (!(ev.ctrlKey || ev.metaKey) || !ev.target || !ev.target.classList || !ev.target.classList.contains('notizen-text')) {
            return;
        }
        var kurz = {b: 'fett', i: 'kursiv'}[ev.key.toLowerCase()];
        if (kurz) {
            ev.preventDefault();
            formatiere(ev.target, REGELN[kurz]);
        }
    });

    window.addEventListener('focus', function () {
        var s = status();
        if (!timer && s && textfeld() && typeof window.notizNeuLaden === 'function') {
            window.notizNeuLaden([{name: 'stand', value: s.getAttribute('data-stand') || '0'}]);
        }
    });

    // Fenster wird geschlossen oder verlassen, während noch eine Änderung wartet: sofort sichern.
    window.addEventListener('pagehide', function () {
        if (timer) {
            clearTimeout(timer);
            sichere();
        }
    });
})();
