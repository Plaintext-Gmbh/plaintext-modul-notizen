/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.notizen;

import ch.plaintext.boot.dashboard.DashboardTile;

/** Kachel «Notizen» auf dem Dashboard (Karte 2096). */
@DashboardTile(
        id = "notizen",
        title = "Notizen",
        icon = "pi pi-pencil",
        link = "notizen.html",
        menuTitle = "Notizen",
        order = 2,
        roles = {"USER", "ADMIN", "ROOT"}
)
public class NotizenDashboardTile {
}
