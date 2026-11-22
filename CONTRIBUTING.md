# Contributing to Truckie1

Thanks for helping build safer vehicle navigation.

## Development standards

1. **TDD** — write a failing unit test (and Gherkin where applicable) before implementation.
2. **Feature flags** — every user-facing capability is gated via `:core-flags`.
3. **Gherkin** — one `.feature` file per feature under `features/`.
4. **Domain language** — use **Vehicle** in UI and public APIs (not Truck).
5. **Clearance** — `max(vehicleHeight, tallestCargo) + buffer`; buffer only in Settings (0.15–1.0 m).
6. **Cargo** — height/width/length/mass belong on stops (pick up / drop off).
7. **Android Auto** — only enable on API 23+ behind `ff_android_auto`.

## Local setup

1. JDK 17+ (21 recommended).
2. Android SDK with `platforms;android-35` and `build-tools;35.0.0`.
3. Clone, then:

```bash
./gradlew :app:assembleDebug test
```

## Pull requests

- Branch from `main` with a clear name.
- Keep changes minimal and focused.
- Include/update tests and docs under `/docs` when behavior changes.
- Do not commit secrets, keystores, or `local.properties`.

## License

By contributing, you agree your contributions are licensed under Apache-2.0.
