#!/usr/bin/env bash
# ci/release.sh — Release des Moduls nach Reposilite (Karte 2412, Muster plaintext-modul-files).
#
# Ausloeser ist der Git-Tag (Woodpecker-Ereignis `tag`, .woodpecker/release.yml). Der Tag IST die
# Version (X.Y.Z, ohne Praefix). Die POM-Version wird nur im Arbeitsbaum gesetzt, nie committet
# (main bleibt auf X.Y.Z-SNAPSHOT).
#
#   ci/release.sh             Release (braucht CI_COMMIT_TAG und MAVEN_NAS_TOKEN)
#   ci/release.sh --trocken   nur pruefen und anzeigen, was veroeffentlicht wuerde
#
# Umgebung: CI_COMMIT_TAG (Woodpecker), MAVEN_NAS_TOKEN (Secret maven_nas_token, Server-Id plaintext-nas),
#           REPOSILITE_BASIS (Standard https://maven.plaintext.ch/releases)
set -euo pipefail

TROCKEN=0; [ "${1:-}" = "--trocken" ] && TROCKEN=1
cd "$(dirname "$0")/.."
BASIS="${REPOSILITE_BASIS:-https://maven.plaintext.ch/releases}"
sag() { echo "release: $*"; }
fehler() { echo "release: FEHLER: $*" >&2; exit 1; }

V="${CI_COMMIT_TAG:-}"
[[ "$V" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fehler "Tag '$V' ist keine Version X.Y.Z"

feld() { awk -v f="$1" '/<parent>/{p=1} /<\/parent>/{p=0; next} !p && $0 ~ "<"f">" {gsub(".*<"f">|</"f">.*",""); print; exit}' pom.xml; }
GRUPPE="$(feld groupId)"; A="$(feld artifactId)"
[ "$GRUPPE" = "ch.plaintext" ] || fehler "groupId '$GRUPPE' — das Modul liegt unter ch.plaintext"
URL="$BASIS/$(echo "$GRUPPE" | tr . /)/$A/$V/$A-$V"

code() { curl -s -o /dev/null -w '%{http_code}' --max-time 30 "$1" || true; }
c="$(code "$URL.pom")"
case "$c" in
    200) fehler "$GRUPPE:$A:$V liegt schon in Reposilite — Versionen sind unveraenderlich, neuen Tag nehmen" ;;
    404) sag "$A:$V ist frei" ;;
    *)   fehler "Reposilite antwortet $c — ohne Kollisionspruefung kein Release" ;;
esac

if [ "$TROCKEN" = 1 ]; then
    sag "TROCKEN: wuerde $GRUPPE:$A:$V bauen und nach $BASIS deployen."
    exit 0
fi
[ -n "${MAVEN_NAS_TOKEN:-}" ] || fehler "MAVEN_NAS_TOKEN fehlt (Woodpecker-Secret maven_nas_token)"

# Zugangsdaten nur in /tmp/ci, nie im geteilten /root/.m2.
mkdir -p /tmp/ci && chmod 700 /tmp/ci
( umask 077; printf '%s\n' '<settings><servers><server><id>plaintext-nas</id><username>ci</username>' \
    "<password>$MAVEN_NAS_TOKEN</password></server></servers></settings>" > /tmp/ci/settings.xml )
trap 'rm -f /tmp/ci/settings.xml; git checkout -q -- pom.xml 2>/dev/null || true' EXIT

mvn -B -q versions:set -DnewVersion="$V" -DgenerateBackupPoms=false
mvn -B -s /tmp/ci/settings.xml clean deploy -Dmaven.build.cache.enabled=false

# Gegenprobe: .pom und .jar samt .sha1 abrufbar.
for e in .pom .pom.sha1 .jar .jar.sha1; do
    c="$(code "$URL$e")"; [ "$c" = 200 ] || fehler "$A-$V$e: HTTP $c nach dem Deploy"
done
sag "$GRUPPE:$A:$V in Reposilite (.pom .jar .sha1: 200)"
