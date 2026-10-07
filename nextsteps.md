# Next steps

## Add a reusable game contract test harness

Create shared deterministic test helpers for checking `Game` / `GameSession` behavior, round accounting, feature results, cap handling, and seed repeatability. Use them for each registered game so contract regressions are caught without duplicating the same integration checks in every game test suite.
