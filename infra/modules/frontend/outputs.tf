output "bucket_name" {
  value = aws_s3_bucket.frontend.id
}

output "distribution_id" {
  value = aws_cloudfront_distribution.main.id
}

output "cloudfront_domain" {
  value       = aws_cloudfront_distribution.main.domain_name
  description = "URL publica de la aplicacion (frontend + API)"
}

output "distribution_arn" {
  value = aws_cloudfront_distribution.main.arn
}
