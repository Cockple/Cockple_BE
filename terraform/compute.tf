resource "google_compute_address" "prod" {
  name   = "cockple-prod-ip"
  region = "asia-northeast3"
}

resource "google_compute_address" "staging" {
  name   = "cockple-staging-ip"
  region = "us-central1"
}

resource "google_compute_instance" "prod" {
  name                      = "cockple-prod"
  machine_type              = "e2-medium"  # 4GB RAM
  zone                      = "asia-northeast3-b"
  tags                      = ["cockple-prod"]
  allow_stopping_for_update = true

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2204-lts"
      size  = 20
    }
  }

  network_interface {
    subnetwork = google_compute_subnetwork.prod.id
    access_config {
      nat_ip = google_compute_address.prod.address
    }
  }

  metadata = {
    ssh-keys = "ubuntu:${var.ssh_public_key}"
  }

  service_account {
    email  = google_service_account.cockple_app.email
    scopes = ["cloud-platform"]  # GCS 등 GCP 서비스 접근
  }

  metadata_startup_script = <<-EOF
    #!/bin/bash
    apt-get update -y
    apt-get install -y docker.io
    curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    systemctl enable docker
    systemctl start docker
    usermod -aG docker ubuntu
  EOF
}

resource "google_compute_instance" "staging" {
  name                      = "cockple-staging"
  machine_type              = "e2-micro"  # 1GB RAM, 무료 티어
  zone                      = "us-central1-a"
  tags                      = ["cockple-staging"]
  allow_stopping_for_update = true

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2204-lts"
      size  = 30  # 무료 티어 최대
    }
  }

  network_interface {
    subnetwork = google_compute_subnetwork.staging.id
    access_config {
      nat_ip = google_compute_address.staging.address
    }
  }

  metadata = {
    ssh-keys = "ubuntu:${var.ssh_public_key}"
  }

  service_account {
    email  = google_service_account.cockple_app.email
    scopes = ["cloud-platform"]
  }

  metadata_startup_script = <<-EOF
    #!/bin/bash
    apt-get update -y
    apt-get install -y docker.io
    curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    systemctl enable docker
    systemctl start docker
    usermod -aG docker ubuntu
  EOF
}
