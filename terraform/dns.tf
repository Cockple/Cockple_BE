resource "cloudflare_record" "prod" {
  zone_id = var.cloudflare_zone_id
  name    = "@"
  content = google_compute_address.prod.address
  type    = "A"
  proxied = true
}

resource "cloudflare_record" "prod_ssh" {
  zone_id = var.cloudflare_zone_id
  name    = "ssh"
  content = google_compute_address.prod.address
  type    = "A"
  proxied = false
}

resource "cloudflare_record" "staging" {
  zone_id = var.cloudflare_zone_id
  name    = "staging"
  content = google_compute_address.staging.address
  type    = "A"
  proxied = true
}

resource "cloudflare_record" "staging_ssh" {
  zone_id = var.cloudflare_zone_id
  name    = "ssh-staging"
  content = google_compute_address.staging.address
  type    = "A"
  proxied = false
}
