# AirConsole — Infra Makefile
# Operations for the infrastructure stack

.PHONY: up down logs ps reset help

up:
	docker-compose up -d

down:
	docker-compose down

down-v:
	docker-compose down -v

prod-up:
	docker-compose -f docker-compose.prod.yml up -d

prod-down:
	docker-compose -f docker-compose.prod.yml down

prod-down-v:
	docker-compose -f docker-compose.prod.yml down -v

prod-build:
	docker-compose -f docker-compose.prod.yml up -d --build

logs:
	docker-compose logs -f

ps:
	docker-compose ps

reset: down-v up

psql:
	docker exec -it airconsole-postgres psql -U airconsole -d airconsole

redis-cli:
	docker exec -it airconsole-redis redis-cli

help:
	@echo "AirConsole Infra — Available Commands:"
	@echo ""
	@echo "  make up      Start infrastructure stack"
	@echo "  make down    Stop infrastructure stack"
	@echo "  make down-v  Stop + remove volumes (⚠️  data loss)"
	@echo "  make logs    Tail all container logs"
	@echo "  make ps      Show running containers"
	@echo "  make reset   Full reset (down + volumes + up)"
	@echo "  make psql    Open PostgreSQL CLI"
	@echo "  make redis-cli Open Redis CLI"
	@echo ""
