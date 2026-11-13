#!/usr/bin/env bash
# ci/release.sh — Release des Moduls nach GitHub Packages (plaintext-mvn), Karte 2096.
#
# Ausloeser ist der Git-Tag (Woodpecker-Ereignis `tag`, .woodpecker/release.yml). Der Tag IST die
# Version (X.Y.Z, ohne Praefix). Die POM-Version wird nur im Arbeitsbaum gesetzt, nie committet.
#
# Umgebung: CI_COMMIT_TAG (Woodpecker), MVN_DEPLOY_TOKEN (Secret mvn_deploy_token, Server-Id plaintext)
set -euo pipefail

cd "$(dirname "$0")/.."
sag() { echo "release: $*"; }
fehler() { echo "release: FEHLER: $*" >&2; exit 1; }

V="${CI_COMMIT_TAG:-}"
[[ "$V" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fehler "Tag '$V' ist keine Version X.Y.Z"

feld() { awk -v f="$1" '/<parent>/{p=1} /<\/parent>/{p=0; next} !p && $0 ~ "<"f">" {gsub(".*<"f">|</"f">.*",""); print; exit}' pom.xml; }
GRUPPE="$(feld groupId)"; ARTEFAKT="$(feld artifactId)"
[ "$GRUPPE" = "ch.plaintext" ] || fehler "groupId '$GRUPPE' — das Modul liegt unter ch.plaintext"
GPFAD="$(echo "$GRUPPE" | tr . /)"
sag "veroeffentliche $GRUPPE:$ARTEFAKT:$V nach GitHub Packages (Plaintext-Gmbh/plaintext-mvn)"

[ -n "${MVN_DEPLOY_TOKEN:-}" ] || fehler "MVN_DEPLOY_TOKEN fehlt (Woodpecker-Secret mvn_deploy_token)"

# Zugangsdaten nur in /tmp/ci, nie im geteilten /root/.m2.
mkdir -p /tmp/ci && chmod 700 /tmp/ci
( umask 077; printf '%s\n' '<settings><servers><server><id>plaintext</id><username>Plaintext-Gmbh</username>' \
    "<password>$MVN_DEPLOY_TOKEN</password></server></servers></settings>" > /tmp/ci/settings.xml )
trap 'rm -f /tmp/ci/settings.xml; git checkout -q -- pom.xml 2>/dev/null || true' EXIT

mvn -B -q versions:set -DnewVersion="$V" -DgenerateBackupPoms=false
mvn -B -s /tmp/ci/settings.xml clean deploy -Dmaven.build.cache.enabled=false
sag "$GRUPPE:$ARTEFAKT:$V ist auf plaintext-mvn"
