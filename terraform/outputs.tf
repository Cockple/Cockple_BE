output "prod_ip" {
  description = "Prod 서버 공인 IP"
  value       = google_compute_address.prod.address
}

output "staging_ip" {
  description = "Staging 서버 공인 IP"
  value       = google_compute_address.staging.address
}