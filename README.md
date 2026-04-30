# syfobrukertilgang

[![Build & Deploy](https://github.com/navikt/syfobrukertilgang/actions/workflows/build-and-deploy.yaml/badge.svg)](https://github.com/navikt/syfobrukertilgang/actions/workflows/build-and-deploy.yaml)

## Miljøer

- 🚀 [Produksjon](https://syfobrukertilgang.intern.nav.no)
- 🛠️ [Utvikling](https://syfobrukertilgang.intern.dev.nav.no)

## Formål

`syfobrukertilgang` er et backend-API som avgjør om en innlogget bruker har tilgang til en ansatt i sykefraværsoppfølgingen.

Tjenesten:

- mottar forespørsler på vegne av en innlogget bruker
- leser personident fra token og ansattens personident fra request-header
- henter aktive relasjoner fra `narmesteleder`
- svarer med `true` eller `false`

## API

### Beskyttet API

**GET** `/api/v2/tilgang/ansatt`

Header:

- `Authorization: Bearer <token>`
- `Nav-Personident: <11 siffer>`

Respons:

- `200 OK` med `true` eller `false`
  - `true` betyr at tilgang ble bekreftet
  - `false` betyr enten at brukeren ikke har en aktiv relasjon til ansatt, eller at oppslaget mot `narmesteleder` feilet
- `400 Bad Request` hvis `Nav-Personident` mangler eller er ugyldig
- `401 Unauthorized` hvis token mangler, ikke er gyldig, eller ikke inneholder `pid`

Operasjonelt kan disse to `false`-utfallene skilles ved å følge Prometheus-metrikkene `syfobrukertilgang_call_narmesteleder_success_count` og `syfobrukertilgang_call_narmesteleder_fail_count` på `/prometheus`.

### Drift/endepunkter

- **GET** `/is_alive`
- **GET** `/is_ready`
- **GET** `/prometheus`

## Autentisering

### Inbound

Den beskyttede API-ruten kjører bak `authenticate("tokenx")`.

- JWT valideres mot TokenX sitt well-known/JWK-oppsett
- tokenets audience må inneholde appens `TOKEN_X_CLIENT_ID`
- `acr` må være `Level4` eller `idporten-loa-high`
- `pid`-claim brukes som identen til innlogget bruker

I NAIS er inbound nettverkstilgang eksplisitt åpnet for:

- `syfomotebehov` (`dev-fss`, `dev-gcp`, `prod-fss`, `prod-gcp`)
- `syfooppfolgingsplanservice` (`dev-fss`, `prod-fss`)
- `oppfolgingsplan-backend`

### Outbound

Ved oppslag mot `narmesteleder` bruker appen Azure AD client credentials.

- token hentes fra `AZURE_OPENID_CONFIG_TOKEN_ENDPOINT`
- klienten autentiserer seg med `AZURE_APP_CLIENT_ID` og `AZURE_APP_CLIENT_SECRET`
- kall går til `NARMESTELEDER_URL/leder/narmesteleder/aktive`
- scope styres av `NARMESTELEDER_SCOPE`
- headeren `Narmeste-Leder-Fnr` brukes for å hente aktive relasjoner for innlogget bruker

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

Ved oppstart leses TokenX sitt well-known-endepunkt for å hente issuer og `jwks_uri`. Selve JWK-oppslagene håndteres deretter av JWK-provider/cache, ikke som et eksplisitt well-known-oppslag per request i applikasjonskoden.

## Kom i gang

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

## Teknologier og observability

- Kotlin
- Ktor
- Gradle (Kotlin DSL)
- Jackson
- Kotest og MockK
- Prometheus-metrikker på `/prometheus`
- egne tellere for vellykkede og feilede kall mot `narmesteleder`
- NAIS-observability med logging til Elastic og Loki, samt auto-instrumentering for Java
- alarmer for nedetid og høy andel HTTP 4xx/5xx i `nais/alerts.yaml`

## Team og kontakt

- Team: `team-esyfo`
- CODEOWNERS: `@navikt/team-esyfo`
