resource "google_compute_network" "cockple_vpc" {
  name                    = "cockple-vpc"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "prod" {
  name          = "cockple-subnet-prod"
  ip_cidr_range = "10.0.1.0/24"
  region        = "asia-northeast3"
  network       = google_compute_network.cockple_vpc.id
}

# Cloudflare IP 대역에서만 80 포트 허용 (origin IP 보호)
resource "google_compute_firewall" "allow_http_cloudflare" {
  name    = "cockple-allow-http-cloudflare"
  network = google_compute_network.cockple_vpc.name

  allow {
    protocol = "tcp"
    ports    = ["80"]
  }

  source_ranges = [
    "173.245.48.0/20",
    "103.21.244.0/22",
    "103.22.200.0/22",
    "103.31.4.0/22",
    "141.101.64.0/18",
    "108.162.192.0/18",
    "190.93.240.0/20",
    "188.114.96.0/20",
    "197.234.240.0/22",
    "198.41.128.0/17",
    "162.158.0.0/15",
    "104.16.0.0/13",
    "104.24.0.0/14",
    "172.64.0.0/13",
    "131.0.72.0/22",
  ]

  target_tags = ["cockple-prod"]
}

resource "google_compute_firewall" "allow_ssh" {
  name    = "cockple-allow-ssh"
  network = google_compute_network.cockple_vpc.name

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["cockple-prod"]
}
