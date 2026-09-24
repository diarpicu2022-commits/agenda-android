#!/usr/bin/env bash
# Prueba :core:domain sin Android SDK ni Google Maven (útil en la nube cuando dl.google.com está bloqueado).
# Crea un proyecto Kotlin/JVM temporal que compila las fuentes del repo y ejecuta sus pruebas.
# Uso: bash tools/verificacion/dominio_sin_sdk.sh
set -euo pipefail
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
KOTLIN="$(sed -n 's/^kotlin = "\(.*\)"/\1/p' "$REPO/gradle/libs.versions.toml")"
COROUTINES="$(sed -n 's/^coroutines = "\(.*\)"/\1/p' "$REPO/gradle/libs.versions.toml")"
JUNIT="$(sed -n 's/^junit = "\(.*\)"/\1/p' "$REPO/gradle/libs.versions.toml")"
TMP="${TMPDIR:-/tmp}/dominio-sin-sdk-$(basename "$REPO")"
mkdir -p "$TMP/gradle"
cp -r "$REPO/gradle/wrapper" "$TMP/gradle/"
cp "$REPO/gradlew" "$TMP/"
echo "org.gradle.jvmargs=-Xmx2g" > "$TMP/gradle.properties"
cat > "$TMP/settings.gradle.kts" <<KTS
pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { mavenCentral() } }
rootProject.name = "dominio"
KTS
cat > "$TMP/build.gradle.kts" <<KTS
plugins { kotlin("jvm") version "$KOTLIN" }
kotlin { jvmToolchain(21) }
sourceSets {
    main { kotlin.srcDir("$REPO/core/domain/src/main/kotlin") }
    test { kotlin.srcDir("$REPO/core/domain/src/test/kotlin") }
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINES")
    testImplementation("junit:junit:$JUNIT")
}
tasks.test { testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL } }
KTS
# Maven Central a veces responde 429: se reintenta.
for i in 1 2 3 4 5; do
  if (cd "$TMP" && ./gradlew test --console=plain -q); then break; fi
  [ "$i" = 5 ] && exit 1
  sleep $((i * 20))
done
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$TMP"/build/test-results/test/*.xml |
  awk -F'"' '{t+=$2; f+=$6+$8} END {print t " pruebas, " f " fallos"; exit (f > 0)}'
