# syfobrukertilgang
Access management for users(citizens and employers) of NAV Sykefraværsoppfølging.

## Endpoint relative paths
* Check logged user's access to employee: `GET /api/v2/tilgang/ansatt`

## Authentication
API accepts valid tokens issued by Idporten.

## Technologies used
* Kotlin
* Ktor
* Gradle
* Mockk
* Kotest
* AzureAD

#### Build
Run `./gradlew clean shadowJar`

#### Test
Run `./gradlew test -i`

#### Code analysis
Run `./gradlew detekt`
