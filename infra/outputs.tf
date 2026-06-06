output "app_url" {
  description = "URL publica de la aplicacion (frontend + API por el mismo dominio)"
  value       = "https://${module.frontend.cloudfront_domain}"
}

output "cloudfront_distribution_id" {
  description = "ID de la distribucion CloudFront (para invalidar cache en el CD)"
  value       = module.frontend.distribution_id
}

output "frontend_bucket_name" {
  description = "Bucket S3 del frontend (destino del aws s3 sync)"
  value       = module.frontend.bucket_name
}

output "ecr_repository_url" {
  description = "URL del repositorio ECR (destino del docker push)"
  value       = module.ecr.repository_url
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

output "ecs_service_name" {
  value = module.ecs.service_name
}

output "ecs_task_family" {
  value = module.ecs.task_definition_family
}

output "github_deploy_role_arn" {
  description = "ARN del rol OIDC; guardar como variable AWS_DEPLOY_ROLE_ARN en el repo"
  value       = module.cicd.deploy_role_arn
}

output "rds_endpoint" {
  value     = module.rds.db_endpoint
  sensitive = true
}
