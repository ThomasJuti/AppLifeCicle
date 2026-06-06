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

variable "target_group_arn" {
  type        = string
  description = "ARN del target group del ALB donde se registra el servicio"
}

variable "alb_security_group_id" {
  type        = string
  description = "SG del ALB; el SG de las tasks solo acepta trafico desde aqui"
}

variable "jwt_secret_arn" {
  type        = string
  description = "ARN del secreto con el JWT_SECRET"
}

variable "admin_secret_arn" {
  type        = string
  description = "ARN del secreto con la contrasena del admin"
}

variable "admin_username" {
  type    = string
  default = "admin"
}

variable "app_port" {
  type    = number
  default = 9090
}
