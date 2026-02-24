resource "google_project_service" "sqladmin" {
  service            = "sqladmin.googleapis.com"
  disable_on_destroy = false
}

resource "google_sql_database_instance" "cockple" {
  name             = "cockple-mysql"
  database_version = "MYSQL_8_0"
  region           = "asia-northeast3"

  deletion_protection = true

  settings {
    tier = "db-g1-small"

    ip_configuration {
      ipv4_enabled    = false  # 공인 IP 비활성화, VPC 내부 통신만
      private_network = google_compute_network.cockple_vpc.id
    }

    backup_configuration {
      enabled    = true
      start_time = "03:00"  # 새벽 3시 자동 백업
    }
  }

  depends_on = [
    google_project_service.sqladmin,
    google_service_networking_connection.private_vpc_connection,
  ]
}

resource "google_sql_database" "prod" {
  name     = "cockple"
  instance = google_sql_database_instance.cockple.name
}

resource "google_sql_database" "staging" {
  name     = "cockple_staging"
  instance = google_sql_database_instance.cockple.name
}

resource "google_sql_user" "prod" {
  name     = "cockple"
  instance = google_sql_database_instance.cockple.name
  password = var.db_password
}

resource "google_sql_user" "staging" {
  name     = "cockple_staging"
  instance = google_sql_database_instance.cockple.name
  password = var.db_staging_password
}