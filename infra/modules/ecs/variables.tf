variable "project_name" { type = string }
variable "environment" { type = string }
variable "aws_region" { type = string }
variable "vpc_id" { type = string }
variable "subnet_ids" { type = list(string) }
variable "app_image" { type = string }
variable "db_url" { type = string }

variable "db_username" {
  type      = string
  sensitive = true
}

variable "db_secret_arn" {
  type        = string
  description = "ARN del secreto de Secrets Manager con las credenciales de la BD"
}
