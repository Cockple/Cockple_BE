# TERRAFORM GUIDE

Apply root `AGENTS.md` first. This file only applies to `terraform/*`.

## OVERVIEW
Terraform provisions the Cockple GCP network/compute/storage stack and Cloudflare DNS integration.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Providers | `main.tf` | Google + Cloudflare providers |
| Network/firewall | `network.tf` | VPC, subnet, Cloudflare-only HTTP ingress, open SSH |
| Compute bootstrap | `compute.tf` | VM, static IP, Docker install via startup script |
| Storage/IAM | `storage.tf` | GCS bucket + service account + public read |
| Inputs/outputs | `variables.tf`, `outputs.tf` | secret vars and exported IP/bucket/account |

## CONVENTIONS
- Region defaults to `asia-northeast3`.
- HTTP ingress is intentionally restricted to Cloudflare IP ranges.
- Bucket CORS is pinned to Cockple prod/staging domains.

## ANTI-PATTERNS
- Do not weaken firewall or bucket exposure without touching the matching runtime assumptions.
- Do not move secrets out of sensitive variables into plain values.
- Do not duplicate Docker/bootstrap logic here and in scripts without keeping them aligned.

## COMMANDS
```bash
terraform plan
terraform apply
```
