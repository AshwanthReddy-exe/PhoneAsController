# AirConsole — Root Makefile
# One-command operations for the entire project

.PHONY: dev test build clean infra-up infra-down infra-logs help

## Start the local development stack (PostgreSQL, Redis, Prometheus, Grafana, Loki, Jaeger)
dev:
	@echo "🚀 Starting local dev stack..."
	cd infra && docker-compose up -d
	@echo "✅ Dev stack running:"
	@echo "   PostgreSQL  → jdbc:postgresql://localhost:5432/airconsole"
	@echo "   Redis       → redis://localhost:6379"
	@echo "   Prometheus  → http://localhost:9090"
	@echo "   Grafana     → http://localhost:3000 (admin/admin)"
	@echo "   Loki        → http://localhost:3100"
	@echo "   Jaeger      → http://localhost:16686"

## Stop the local development stack
infra-down:
	@echo "🛑 Stopping local dev stack..."
	cd infra && docker-compose down

## Show logs for the local development stack
infra-logs:
	cd infra && docker-compose logs -f

## Run all tests across all modules
test:
	@echo "🧪 Running all tests..."
	./gradlew test

## Build all modules (compile + test + package)
build:
	@echo "🔨 Building all modules..."
	./gradlew build

## Clean build artifacts
clean:
	@echo "🧹 Cleaning build artifacts..."
	./gradlew clean
	cd infra && docker-compose down -v

## Publish common-lib to local Maven repo
publish-local:
	@echo "📦 Publishing common-lib to local Maven..."
	./gradlew :common-lib:publishToMavenLocal

## Build Docker images for all services
docker-build:
	@echo "🐳 Building Docker images..."
	./gradlew bootBuildImage

## Check code style
checkstyle:
	@echo "🔍 Running checkstyle..."
	./gradlew checkstyleMain checkstyleTest

## Generate JaCoCo coverage reports
coverage:
	@echo "📊 Generating coverage reports..."
	./gradlew jacocoTestReport
	@echo "Open: common-lib/build/reports/jacoco/test/html/index.html"

## Format all files (requires editorconfig)
format:
	@echo "🎨 Formatting code..."
	./gradlew spotlessApply || echo "spotless not configured — install EditorConfig plugin in IDE"

## Show this help
help:
	@echo "AirConsole — Available Commands:"
	@echo ""
	@echo "  make dev          Start local dev stack (DB, Redis, Observability)"
	@echo "  make infra-down   Stop local dev stack"
	@echo "  make infra-logs   Tail dev stack logs"
	@echo "  make test         Run all tests"
	@echo "  make build        Build all modules"
	@echo "  make clean        Clean build artifacts + dev stack"
	@echo "  make publish-local Publish common-lib to local Maven"
	@echo "  make docker-build Build Docker images for all services"
	@echo "  make checkstyle   Run code style checks"
	@echo "  make coverage     Generate JaCoCo coverage reports"
	@echo ""
