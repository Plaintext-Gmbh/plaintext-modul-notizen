/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.notizen;

import ch.plaintext.boot.menu.MenuAnnotation;

/** Menü «Notizen» (Karte 2096); darunter {@link NotizenSeiteMenu} und {@link NotizenEinstellungenMenu}. */
@MenuAnnotation(
        title = "Notizen",
        link = "notizen.html",
        order = 9000,
        icon = "pi pi-pencil",
        roles = {"USER", "ADMIN", "ROOT"},
        moduleId = "notizen"
)
public class NotizenMenu {
}
