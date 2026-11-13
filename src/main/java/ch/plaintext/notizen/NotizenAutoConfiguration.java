/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.notizen;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Einbau ohne eigenen Code (Karte 2096): das Jar auf den Klassenpfad, fertig. Spring Boot lädt diese Klasse
 * über {@code META-INF/spring/...AutoConfiguration.imports}; sie meldet Speicher und Seiten-Beans an. Menü
 * und Kachel findet root über die Annotationen.
 */
@AutoConfiguration
@ComponentScan(basePackageClasses = NotizenAutoConfiguration.class)
public class NotizenAutoConfiguration {
}
