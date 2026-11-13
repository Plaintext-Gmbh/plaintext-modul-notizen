/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.notizen;

import ch.plaintext.boot.menu.MenuAnnotation;

/** «Notizen-Einstellungen»: Ablage und Ordner wählen, nur Admin und Root (Karte 2096). */
@MenuAnnotation(
        title = "Notizen-Einstellungen",
        link = "notizen-einstellungen.html",
        parent = "Notizen",
        order = 2,
        icon = "pi pi-cog",
        roles = {"ADMIN", "ROOT"}
)
public class NotizenEinstellungenMenu {
}
