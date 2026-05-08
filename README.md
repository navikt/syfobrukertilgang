# API for tilgang til ansatte i sykefraværsoppfølgingen

[![Build & Deploy](https://github.com/navikt/syfobrukertilgang/actions/workflows/build-and-deploy.yaml/badge.svg)](https://github.com/navikt/syfobrukertilgang/actions/workflows/build-and-deploy.yaml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Ktor](https://img.shields.io/badge/Ktor-3.2.0-087CFA?logo=ktor&logoColor=white)](https://ktor.io/)
[![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Kotest](https://img.shields.io/badge/Kotest-6.1.11-FF6B6B)](https://kotest.io/)

> **Merk:** Tjenesten er kun for internt bruk av eksisterende konsumenter. Nye tjenester skal ikke integrere seg mot den.

## Formål

Backend-API tjeneste som avgjør om en innlogget bruker har tilgang til en ansatt i sykefraværsoppfølgingen.

- mottar forespørsler på vegne av en innlogget bruker
- leser personident fra token og ansattens personident fra request-header
- henter aktive relasjoner fra `narmesteleder`
- svarer med `true` eller `false`

## API

**GET** `/api/v2/tilgang/ansatt`

## Arkitektur

```mermaid
graph LR
    A[syfomotebehov / syfooppfolgingsplanservice / oppfolgingsplan-backend] -->|GET /api/v2/tilgang/ansatt<br/>TokenX + Nav-Personident| B[syfobrukertilgang]
    B -->|Initialiserer issuer/JWK-provider ved oppstart| C[TokenX well-known / JWK]
    B -->|Henter Azure AD-token| D[Azure AD]
    B -->|GET /leder/narmesteleder/aktive<br/>Narmeste-Leder-Fnr| E[narmesteleder]
    E -->|aktive relasjoner| B
    B -->|true / false| A
```

## Utvikling (kjøre lokalt)

### Forutsetninger

- Java 21
- tilgang til nødvendige lokale verdier for TokenX, Azure AD og `narmesteleder`

### Lokal kjøring

1. Opprett `src/main/resources/localEnv.json` med utgangspunkt i `src/main/resources/localEnvForTests.json`.
2. Erstatt testverdiene med reelle verdier for:
   - `narmestelederScope`
   - `narmestelederUrl`
   - `aadClientId`
   - `aadClientSecret`
   - `aadTokenEndpoint`
   - `syfobrukertilgangTokenXClientId`
   - `tokenXWellKnownUrl`
3. Bygg kjørbar jar:

   ```bash
   ./gradlew shadowJar
   ```

4. Start appen:

   ```bash
   java -jar build/libs/syfobrukertilgang-1.0-SNAPSHOT-all.jar
   ```

Applikasjonen bruker `application.conf` med `ktor.environment=dev` lokalt. Port settes fra `localEnv.json`.

### Nyttige kommandoer

```bash
./gradlew shadowJar
./gradlew test
./gradlew detekt
```

`./gradlew build` kjører build, tester og statisk analyse samlet.

## For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen [#esyfo](https://nav-it.slack.com/archives/C012X796B4L).
