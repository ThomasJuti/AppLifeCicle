output "jwt_secret_arn" {
  value = aws_secretsmanager_secret.jwt.arn
}

output "admin_secret_arn" {
  value = aws_secretsmanager_secret.admin.arn
}

output "admin_username" {
  value = var.admin_username
}
