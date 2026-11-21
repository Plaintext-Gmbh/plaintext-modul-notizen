// Karte 2413: Textregeln der Notizen-Knöpfe (Regeln Daniel). Lauf: node --test src/test/js/notizen.test.js
'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const R = require('../../main/resources/META-INF/resources/notizen/notizen.js');

// Text mit Auswahl: «|» markiert Cursor oder Auswahlgrenzen (eins = Cursor, zwei = Auswahl).
function mit(regel, vorlage, jetzt) {
    const a = vorlage.indexOf('|');
    const rest = vorlage.slice(0, a) + vorlage.slice(a + 1);
    const b = rest.indexOf('|');
    const e = b < 0 ? a : b;
    const text = b < 0 ? rest : rest.slice(0, b) + rest.slice(b + 1);
    return R[regel](text, a, e, jetzt).text;
}

test('fett: Sternzeile so breit wie die Zeile, dann Leerzeile', () => {
    assert.equal(mit('fett', 'Ti|tel\nweiter'), 'Titel\n*****\n\nweiter');
    assert.equal(mit('fett', '|Titel|'), 'Titel\n*****\n');
});
test('fett: leere Zeile und schon unterstrichene Zeile bleiben', () => {
    assert.equal(mit('fett', 'a\n|\nb'), 'a\n\nb');
    assert.equal(mit('fett', '|Titel\n*****'), 'Titel\n*****');
});
test('fett: Mehrzeilenauswahl unterstreicht jede Zeile', () => {
    assert.equal(mit('fett', '|ab\ncde|'), 'ab\n**\n\ncde\n***\n');
});

test('punkt: «- » an den Zeilenanfang, nicht doppelt', () => {
    assert.equal(mit('punkt', 'Ei|nkauf'), '- Einkauf');
    assert.equal(mit('punkt', '- Ei|nkauf'), '- Einkauf');
    assert.equal(mit('punkt', '|'), '- ');
    assert.equal(mit('punkt', '|a\nb|\nc'), '- a\n- b\nc');
});

test('unterpunkt: Leerzeichen und «- », aus Punkt wird Unterpunkt', () => {
    assert.equal(mit('unterpunkt', 'Mi|lch'), ' - Milch');
    assert.equal(mit('unterpunkt', '- Mi|lch'), ' - Milch');
    assert.equal(mit('unterpunkt', ' - Mi|lch'), ' - Milch');
    assert.equal(mit('unterpunkt', '|'), ' - ');
    assert.equal(mit('unterpunkt', '|a\n- b|'), ' - a\n - b');
});

test('datum: JJ-MM-TT an der Cursorstelle, ersetzt die Auswahl', () => {
    const tag = new Date(2026, 9, 25);
    assert.equal(mit('datum', 'Am | gesehen', tag), 'Am 26-10-25 gesehen');
    assert.equal(mit('datum', '|alt|', new Date(2027, 0, 5)), '27-01-05');
});

test('markierungen: an den Zeilenanfang, ersetzen eine vorhandene', () => {
    assert.equal(mit('erledigt', 'Zahn|arzt'), '[x] Zahnarzt');
    assert.equal(mit('offen', 'Zahn|arzt'), '[.] Zahnarzt');
    assert.equal(mit('offen', '[x] Zahn|arzt'), '[.] Zahnarzt');
    assert.equal(mit('erledigt', '[x] Zahn|arzt'), '[x] Zahnarzt');
    assert.equal(mit('erledigt', '|'), '[x] ');
    assert.equal(mit('erledigt', '|a\n[.] b|'), '[x] a\n[x] b');
});

test('Auswahl bis direkt nach dem Zeilenumbruch nimmt die Folgezeile nicht mit', () => {
    assert.equal(mit('punkt', '|a\n|b'), '- a\nb');
});

test('kursiv bleibt wie bisher', () => {
    assert.equal(mit('kursiv', 'ein |Wort| hier'), 'ein *Wort* hier');
});
