variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name used as prefix for all resources"
  type        = string
  default     = "customers-api"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "PostgreSQL master password"
  type        = string
  sensitive   = true
}

variable "app_image" {
  description = "Docker image URI for the application. Para el primer apply usa un placeholder; el pipeline publica la imagen real."
  type        = string
}

variable "github_owner" {
  description = "Usuario u organizacion de GitHub"
  type        = string
  default     = "ThomasJuti"
}

variable "github_repo" {
  description = "Nombre del repositorio de GitHub"
  type        = string
  default     = "AppLifeCicle"
}

variable "create_oidc_provider" {
  description = "Crear el OIDC provider de GitHub. Ponlo en false si ya existe en la cuenta."
  type        = bool
  default     = true
}
