output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = module.ecr.repository_url
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = module.rds.db_endpoint
  sensitive   = true
}

output "app_public_ip" {
  description = "Public IP of the ECS task"
  value       = module.ecs.service_url
}
