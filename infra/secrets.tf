resource "random_password" "db" {
  length  = 32
  special = false
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/${local.app_name}/db-password"
  type  = "SecureString"
  value = random_password.db.result
}
