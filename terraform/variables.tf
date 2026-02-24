variable "gcp_project_id" {
  description = "GCP 프로젝트 ID"
  type        = string
}

variable "gcp_region" {
  description = "GCP 기본 리전 (prod)"
  type        = string
  default     = "asia-northeast3"
}

variable "cloudflare_api_token" {
  description = "Cloudflare API 토큰"
  type        = string
  sensitive   = true
}

variable "cloudflare_zone_id" {
  description = "cockple.shop Cloudflare Zone ID"
  type        = string
}

variable "ssh_public_key" {
  description = "인스턴스 접속용 SSH 공개키"
  type        = string
}
