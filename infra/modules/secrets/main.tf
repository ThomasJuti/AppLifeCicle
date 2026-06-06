terraform {
  required_providers {
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

resource "random_password" "jwt" {
  length  = 64
  special = false
}

resource "random_password" "admin" {
  length           = 24
  special          = true
  override_special = "-_."
}

resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${var.project_name}-${var.environment}-jwt-secret"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = random_password.jwt.result
}

resource "aws_secretsmanager_secret" "admin" {
  name                    = "${var.project_name}-${var.environment}-admin-password"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "admin" {
  secret_id     = aws_secretsmanager_secret.admin.id
  secret_string = random_password.admin.result
}
