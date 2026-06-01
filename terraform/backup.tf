resource "google_storage_bucket" "cockple_db_backups" {
  name     = "cockple-db-backups-${var.gcp_project_id}"
  location = upper(var.gcp_region)

  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"

  lifecycle_rule {
    action {
      type = "Delete"
    }

    condition {
      age = 30
    }
  }

  depends_on = [google_project_service.storage]
}

resource "google_storage_bucket_iam_member" "app_db_backup_object_creator" {
  bucket = google_storage_bucket.cockple_db_backups.name
  role   = "roles/storage.objectCreator"
  member = "serviceAccount:${google_service_account.cockple_app.email}"
}
