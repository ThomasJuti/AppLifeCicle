terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
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

module "ecs" {
  source        = "./modules/ecs"
  project_name  = var.project_name
  environment   = var.environment
  aws_region    = var.aws_region
  vpc_id        = module.vpc.vpc_id
  subnet_ids    = module.vpc.public_subnet_ids
  app_image     = var.app_image
  db_url        = "jdbc:postgresql://${module.rds.db_endpoint}:5432/customersdb"
  db_username   = var.db_username
  db_secret_arn = module.rds.db_secret_arn
}
