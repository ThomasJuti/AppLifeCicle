terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Descomenta cuando tengas un bucket S3 para el state
  # backend "s3" {
  #   bucket = "customers-api-terraform-state"
  #   key    = "prod/terraform.tfstate"
  #   region = "us-east-1"
  # }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

module "vpc" {
  source       = "./modules/vpc"
  project_name = var.project_name
  environment  = var.environment
}

module "ecr" {
  source       = "./modules/ecr"
  project_name = var.project_name
  environment  = var.environment
}

module "rds" {
  source       = "./modules/rds"
  project_name = var.project_name
  environment  = var.environment
  vpc_id       = module.vpc.vpc_id
  subnet_ids   = module.vpc.private_subnet_ids
  db_username  = var.db_username
  db_password  = var.db_password
}

module "secrets" {
  source       = "./modules/secrets"
  project_name = var.project_name
  environment  = var.environment
}

module "alb" {
  source       = "./modules/alb"
  project_name = var.project_name
  environment  = var.environment
  vpc_id       = module.vpc.vpc_id
  subnet_ids   = module.vpc.public_subnet_ids
}

module "ecs" {
  source                = "./modules/ecs"
  project_name          = var.project_name
  environment           = var.environment
  aws_region            = var.aws_region
  vpc_id                = module.vpc.vpc_id
  subnet_ids            = module.vpc.public_subnet_ids
  app_image             = var.app_image
  db_url                = "jdbc:postgresql://${module.rds.db_endpoint}:5432/customersdb"
  db_username           = var.db_username
  db_secret_arn         = module.rds.db_secret_arn
  target_group_arn      = module.alb.target_group_arn
  alb_security_group_id = module.alb.security_group_id
  jwt_secret_arn        = module.secrets.jwt_secret_arn
  admin_secret_arn      = module.secrets.admin_secret_arn
  admin_username        = module.secrets.admin_username
}

module "frontend" {
  source       = "./modules/frontend"
  project_name = var.project_name
  environment  = var.environment
  alb_dns_name = module.alb.dns_name
}

module "cicd" {
  source                      = "./modules/cicd"
  project_name                = var.project_name
  github_owner                = var.github_owner
  github_repo                 = var.github_repo
  create_oidc_provider        = var.create_oidc_provider
  ecr_repository_arn          = module.ecr.repository_arn
  frontend_bucket_arn         = "arn:aws:s3:::${module.frontend.bucket_name}"
  cloudfront_distribution_arn = module.frontend.distribution_arn
  ecs_task_execution_role_arn = module.ecs.task_execution_role_arn
}
