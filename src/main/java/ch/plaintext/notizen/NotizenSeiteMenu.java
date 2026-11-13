/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.notizen;

import ch.plaintext.boot.menu.MenuAnnotation;

/** «Meine Notizen» für alle angemeldeten Benutzer (Karte 2096). */
@MenuAnnotation(
        title = "Meine Notizen",
        link = "notizen.html",
        parent = "Notizen",
        order = 1,
        icon = "pi pi-file-edit",
        roles = {"USER", "ADMIN", "ROOT"}
)
public class NotizenSeiteMenu {
}
