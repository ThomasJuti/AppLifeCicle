variable "project_name" { type = string }
variable "environment" { type = string }

variable "admin_username" {
  type        = string
  default     = "admin"
  description = "Usuario administrador de la aplicacion en prod"
}
