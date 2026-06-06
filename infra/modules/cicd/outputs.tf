output "deploy_role_arn" {
  value       = aws_iam_role.deploy.arn
  description = "ARN del rol que asume GitHub Actions (guardar como variable del repo)"
}
