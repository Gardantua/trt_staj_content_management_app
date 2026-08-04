# Stage 9 load profiles

The same public content-read journey is exercised with four profiles so results
stay comparable: `baseline`, `ramp`, `spike`, and a ten-minute short `soak`.

Start the application with rate limiting disabled for raw capacity measurement,
and with the `local` profile because the journey uses the temporary test USER
headers. Then run one profile at a time:

```text
$env:RATE_LIMIT_ENABLED="false"
docker compose --profile load run --rm -e LOAD_PROFILE=baseline k6
docker compose --profile load run --rm -e LOAD_PROFILE=ramp k6
docker compose --profile load run --rm -e LOAD_PROFILE=spike k6
docker compose --profile load run --rm -e LOAD_PROFILE=soak k6
```

Every run fails when the error rate reaches 1%, p95 reaches 500 ms, p99 reaches
one second, or the requested arrival rate cannot be generated. The JSON summary
is written to `build/load-results/summary.json`. These limits are the first MVP
capacity guardrails, not a production SLO guarantee.
