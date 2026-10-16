# Contributing to DailyPlanetH2

Thanks for your interest. This document describes how to propose changes.

## Ways to contribute

- Report a bug or a usability problem with an [issue](https://github.com/donatopepe/DailyPlanetH2/issues)
  (include steps to reproduce, expected behaviour, actual behaviour, environment).
- Propose a feature or an improvement.
- Send a pull request with a fix, a test or documentation.

## Development setup

1. Clone the repository and make sure a JDK 17+ is available (the Gradle toolchain can
   download it automatically).
2. Run the test suite before and after your changes:

   ```bash
   ./gradlew clean test        # Linux / macOS
   gradlew.bat clean test      # Windows
   ```

3. Start the application in development mode:

   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

## Pull request guidelines

- Keep the change focused: one logical change per pull request.
- **Add or update tests** for every behaviour change; a fix without a regression test is
  considered incomplete.
- Keep the tree green: `./gradlew clean test` must pass.
- Do not commit secrets, credentials, tokens, real e-mail addresses or personal data.
- Do not add dependencies without stating the reason in the pull request description.
- Keep the project cross-platform: no absolute paths, UTF-8 only, no reliance on a POSIX
  shell inside the build, line endings normalised (see `.gitattributes`).
- Follow the existing code style (4 spaces, no wildcard imports, Java 17 language level).
- Update the documentation (`README.md`, `README.it.md`, `docs/`, `CHANGELOG.md`) when the
  change is user visible.

## Commit messages

```
S<phase>.<task>: short summary in imperative mood

Optional body explaining what changed and why, plus the commands used to verify it.
```

## Reporting security issues

Please do not open a public issue for a security problem: contact the maintainer directly
(see the repository owner profile) and allow time for a fix before disclosure.

## License

By contributing you agree that your contribution is released under the MIT license of this
project (see [LICENSE](LICENSE)).
