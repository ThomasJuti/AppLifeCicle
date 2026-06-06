resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = var.subnet_ids
}

resource "aws_security_group" "rds" {
  name   = "${var.project_name}-rds-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_instance" "main" {
  identifier        = "${var.project_name}-${var.environment}"
  engine            = "postgres"
  engine_version    = "16.3"
  instance_class    = "db.t3.micro" # FREE TIER
  allocated_storage = 20            # FREE TIER máximo

  db_name  = "customersdb"
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  skip_final_snapshot     = true
  deletion_protection     = false
  backup_retention_period = 0 # Desactivar backups en free tier

  tags = { Name = "${var.project_name}-rds" }
}

# ── Credenciales gestionadas en Secrets Manager (fuera de la task definition) ──
resource "aws_secretsmanager_secret" "db" {
  name                    = "${var.project_name}-${var.environment}-db-credentials"
  recovery_window_in_days = 0 # kata/free tier: borrado inmediato, sin ventana de recuperación
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    username = var.db_username
    password = var.db_password
  })
}
