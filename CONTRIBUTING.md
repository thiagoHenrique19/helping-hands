# Contributing

## Branch model
| Branch    | Purpose                                   |
|-----------|-------------------------------------------|
| `main`    | Production. Only receives release PRs from `hmg`. |
| `hmg`     | Homologation (integration branch). Must always be green. |
| `feat/*`  | One branch per feature, created from `hmg`. |

## Workflow
1. `git checkout hmg && git pull`
2. `git checkout -b feat/<short-name>`
3. Work in small, logical commits.
4. Run `./mvnw verify` (Docker is required for Testcontainers).
5. Push and open a Pull Request into `hmg`; CI must pass.
6. Merge with a merge commit and delete the feature branch.
7. Releases: open a PR `hmg` -> `main` titled `Release vX.Y.Z`, merge it and tag `main`.

## Commit convention
Messages are in English, imperative mood, and use only two prefixes:
- `feat: add volunteer opportunity entity`
- `fix: reject donations to cancelled campaigns`

## Code guidelines
- Test first (JUnit 5, Mockito, AssertJ, Testcontainers).
- `core` must not depend on Spring; `application` only on core; frameworks live in `infrastructure`.
- Every entity change requires a new Flyway migration; never edit an existing one.
