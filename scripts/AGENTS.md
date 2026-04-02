# SCRIPTS GUIDE

Apply root `AGENTS.md` first. This file only applies to `scripts/*`.

## OVERVIEW
Shell scripts handle server deployment and local SSH tunneling into the remote stack.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Branch-aware deploy | `deploy.sh` | writes `.env`, pulls image tag, restarts one app service |
| Remote DB/Redis access | `tunnel.sh` | forwards MySQL to `3307`, Redis to `6380` |

## CONVENTIONS
- `deploy.sh` treats `main` as `cockple-app:latest`; any other branch path becomes `cockple-app-staging:staging`.
- Deploy always starts `mysql`, `redis`, and `nginx` before replacing the app container.
- Health checks are part of the script, not just Docker Compose.

## ANTI-PATTERNS
- Do not change forwarded local ports without matching `application-local.yml`.
- Do not add new required env vars in scripts without updating workflow `envs` and compose.
- Do not bypass the health-check loop when changing deploy behavior.

## COMMANDS
```bash
bash scripts/deploy.sh <docker_repo> <branch>
bash scripts/tunnel.sh <GCP_IP>
```
