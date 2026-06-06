output "dns_name" {
  value       = aws_lb.main.dns_name
  description = "DNS publico del ALB (origin del backend en CloudFront)"
}

output "target_group_arn" {
  value = aws_lb_target_group.app.arn
}

output "security_group_id" {
  value = aws_security_group.alb.id
}

output "listener_arn" {
  value = aws_lb_listener.http.arn
}
