output "prod_ip" {
  description = "서버 공인 IP (prod + staging 공용)"
  value       = google_compute_address.prod.address
}

output "gcs_bucket_name" {
  description = "GCS 버킷 이름"
  value       = google_storage_bucket.cockple_assets.name
}

output "app_service_account_email" {
  description = "앱 서비스 계정 이메일 (GCS 인증에 사용)"
  value       = google_service_account.cockple_app.email
}