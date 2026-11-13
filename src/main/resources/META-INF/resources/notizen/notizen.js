/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
/*
 * Karte 2096: Verhalten der Seite «Meine Notizen». Alles über Listener am document (CSP ohne
 * 'unsafe-inline', und es überlebt Teil-Updates von PrimeFaces):
 *  - Autosave: nach jeder Änderung, sobald 800 ms nicht mehr getippt wird (remoteCommand notizSpeichern).
 *  - Mini-Editor: fett, kursiv, Liste, Überschrift als Markdown ins Textfeld.
 *  - «In eigenem Fenster öffnen»: dieselbe Seite mit ?fenster=1.
 *  - Fokus: hat das andere Fenster inzwischen gespeichert, neu laden (remoteCommand notizNeuLaden).
 */
(function () {
    'use strict';

    var PAUSE_MS = 800;
    var timer = null;

    var FORMATE = {
        fett: {vor: '**', nach: '**', zeile: false},
        kursiv: {vor: '*', nach: '*', zeile: false},
        liste: {vor: '- ', nach: '', zeile: true},
        titel: {vor: '# ', nach: '', zeile: true}
    };

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

    /** Markdown um die Auswahl legen (fett, kursiv) oder vor jede gewählte Zeile setzen (Liste, Überschrift). */
    function formatiere(t, f) {
        var a = t.selectionStart, e = t.selectionEnd, w = t.value;
        if (f.zeile) {
            var start = w.lastIndexOf('\n', a - 1) + 1;
            var block = w.substring(start, e);
            var neu = block.split('\n').map(function (z) {
                return z.indexOf(f.vor) === 0 ? z : f.vor + z;
            }).join('\n');
            t.value = w.substring(0, start) + neu + w.substring(e);
            t.setSelectionRange(start, start + neu.length);
        } else {
            var mitte = w.substring(a, e);
            t.value = w.substring(0, a) + f.vor + mitte + f.nach + w.substring(e);
            t.setSelectionRange(a + f.vor.length, a + f.vor.length + mitte.length);
        }
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
        var f = FORMATE[k.getAttribute('data-format')];
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
            formatiere(ev.target, FORMATE[kurz]);
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
