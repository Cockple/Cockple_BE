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