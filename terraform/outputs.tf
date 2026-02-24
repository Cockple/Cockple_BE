output "prod_ip" {
  description = "Prod 서버 공인 IP"
  value       = google_compute_address.prod.address
}

output "staging_ip" {
  description = "Staging 서버 공인 IP"
  value       = google_compute_address.staging.address
}

output "db_private_ip" {
  description = "Cloud SQL 사설 IP (SSH 터널로 접속)"
  value       = google_sql_database_instance.cockple.private_ip_address
}

output "gcs_bucket_name" {
  description = "GCS 버킷 이름"
  value       = google_storage_bucket.cockple_assets.name
}

output "app_service_account_email" {
  description = "앱 서비스 계정 이메일 (GCS 인증에 사용)"
  value       = google_service_account.cockple_app.email
}