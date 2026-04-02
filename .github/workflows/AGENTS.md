# WORKFLOWS GUIDE

Apply root `AGENTS.md` first. This file only applies to `.github/workflows/*` edits.

## OVERVIEW
This directory drives CI on pull requests and CD on pushes to `develop` and `main`.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| PR build/test | `ci.yml` | writes `application.yml`, creates Firebase key, runs `./gradlew build` |
| Image build + deploy | `cd.yml` | builds on push, tags by branch, then copies compose/nginx/scripts to server |

## CONVENTIONS
- `develop` and `main` are the only workflow branches here.
- `ci.yml` chooses `APPLICATION` vs `APPLICATION_STAGING` by `github.base_ref`.
- `cd.yml` chooses Docker tag `latest` for `main`, `staging` otherwise.
- Deploy step assumes `/home/ubuntu/cockple` and hands off to `scripts/deploy.sh`.

## ANTI-PATTERNS
- Do not hardcode secrets into workflow YAML.
- Do not change copied deploy assets in `cd.yml` without matching script/compose expectations.
- Do not make `main` and `develop` diverge silently; branch-specific behavior is intentionally small and explicit.

## COMMANDS
```bash
./gradlew.bat build
```
