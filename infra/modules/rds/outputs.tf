output "db_endpoint" {
  value     = aws_db_instance.main.address # solo el host; el puerto se añade en main.tf
  sensitive = true
}

output "db_secret_arn" {
  value = aws_secretsmanager_secret.db.arn
}
