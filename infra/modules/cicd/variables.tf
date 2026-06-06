variable "project_name" { type = string }

variable "github_owner" {
  type        = string
  description = "Usuario u organizacion de GitHub"
}

variable "github_repo" {
  type        = string
  description = "Nombre del repositorio"
}

variable "ecr_repository_arn" {
  type = string
}

variable "frontend_bucket_arn" {
  type = string
}

variable "cloudfront_distribution_arn" {
  type = string
}

variable "ecs_task_execution_role_arn" {
  type        = string
  description = "Rol de ejecucion de la task; el rol de CI necesita poder pasarlo"
}

variable "create_oidc_provider" {
  type        = bool
  default     = true
  description = "false si el OIDC provider de GitHub ya existe en la cuenta"
}
