output "alb_dns_name" {
  value = data.aws_lb.shared.dns_name
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "app_url" {
  value = "https://${local.domain}"
}

output "github_actions_role_arn" {
  value = aws_iam_role.github_actions_deploy.arn
}
