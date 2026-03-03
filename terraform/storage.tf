resource "google_project_service" "storage" {
  service            = "storage.googleapis.com"
  disable_on_destroy = false
}

# 앱 인스턴스용 서비스 계정 (GCS 접근)
resource "google_service_account" "cockple_app" {
  account_id   = "cockple-app"
  display_name = "Cockple App Service Account"
}

resource "google_storage_bucket" "cockple_assets" {
  name     = "cockple-assets-${var.gcp_project_id}"
  location = "ASIA-NORTHEAST3"

  uniform_bucket_level_access = true

  cors {
    origin          = ["https://cockple.store", "https://staging.cockple.store"]
    method          = ["GET", "PUT", "POST", "DELETE"]
    response_header = ["Content-Type"]
    max_age_seconds = 3600
  }

  depends_on = [google_project_service.storage]
}

resource "google_storage_bucket_iam_member" "app_storage_admin" {
  bucket = google_storage_bucket.cockple_assets.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.cockple_app.email}"
}

resource "google_storage_bucket_iam_member" "public_read" {
  bucket = google_storage_bucket.cockple_assets.name
  role   = "roles/storage.objectViewer"
  member = "allUsers"
}
