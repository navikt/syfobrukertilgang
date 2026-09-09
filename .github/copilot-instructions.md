# syfobrukertilgang

- Existing consumers only: new services must not integrate with this API.
- `./gradlew build` runs build and checks; `./gradlew test` runs tests.
  Use Java 25 from `mise.toml`; the README's Java 21 prerequisite is stale.
- Local configuration is read from `src/main/resources/localEnv.json`;
  `localEnvForTests.json` supplies test values, not working remote credentials.
- `/api/v2/tilgang/ansatt` gets the logged-in leader from TokenX `pid` and the
  employee from `Nav-Personident`. Preserve the audience and high-assurance
  `acr` checks and the active nearest-leader relation check.
- No matching relation, including an empty upstream result, means no access.
  Do not turn successful token validation into a positive access decision.
