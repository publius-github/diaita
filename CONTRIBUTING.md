# Contributing

Diaita is a small one-maintainer project. Issues and pull requests are welcome and reviewed as time allows; there is no roadmap or support commitment.

- **Bugs and ideas:** open an issue using the templates. Please keep personal data (weight, notes) out of screenshots and logs.
- **Security problems:** do not open an issue, follow [SECURITY.md](SECURITY.md).
- **Pull requests:** keep them focused and open an issue first for anything large.

## Before you open a pull request

You need JDK 21 and the Android SDK (see the README for building).

```bash
./gradlew ktlintCheck testDebugUnitTest
```

- Follow the existing code style; ktlint enforces it (max line length 120).
- Add every user-visible string to both `values/strings.xml` and `values-ru/strings.xml`.
- A Room schema change needs a migration, an exported schema in `app/schemas/` and a migration test.
- Describe user-facing changes in `CHANGELOG.md`.

By contributing you agree that your work is released under the [MIT License](LICENSE).
