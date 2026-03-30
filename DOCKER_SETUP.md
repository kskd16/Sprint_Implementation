# SmartSure Docker Setup Guide

## Prerequisites
- Docker Desktop installed and running
- At least 8GB RAM allocated to Docker
- Ports 8080-8085, 8761, 5433, 6379, 5672, 15672, 9411 must be free

---

## Project Structure After Docker Setup

```
sprintImplementation/
├── docker-compose.yml
├── docker/
│   └── postgres/
│       └── init-databases.sh     ← creates all 5 DBs automatically
├── authService/
│   ├── Dockerfile
│   └── .dockerignore
├── policyService/
│   ├── Dockerfile
│   └── .dockerignore
├── claimService/
│   ├── Dockerfile
│   └── .dockerignore
├── adminService/
│   ├── Dockerfile
│   └── .dockerignore
├── paymentService/
│   ├── Dockerfile
│   └── .dockerignore
├── service-registry/
│   ├── Dockerfile
│   └── .dockerignore
└── apiGateway/
    ├── Dockerfile
    └── .dockerignore
```

---

## Step-by-Step Commands

### 1. Open terminal in the project root
```
cd C:\practice\sprintImplementation
```

### 2. Build and start everything (first time)
```
docker-compose up --build
```
This will:
- Pull postgres, redis, rabbitmq, zipkin images
- Build all 7 Spring Boot services from source
- Create all 5 PostgreSQL databases automatically
- Start everything in the correct order

### 3. Start in background (after first build)
```
docker-compose up -d
```

### 4. Check all containers are running
```
docker-compose ps
```
Expected output — all containers should show "Up":
```
smartsure-postgres    Up (healthy)
smartsure-redis       Up (healthy)
smartsure-rabbitmq    Up (healthy)
smartsure-zipkin      Up
smartsure-eureka      Up (healthy)
smartsure-auth        Up
smartsure-policy      Up
smartsure-claim       Up
smartsure-admin       Up
smartsure-payment     Up
smartsure-gateway     Up
```

### 5. View logs for a specific service
```
docker-compose logs -f auth-service
docker-compose logs -f policy-service
docker-compose logs -f api-gateway
```

### 6. Stop all containers
```
docker-compose down
```

### 7. Stop and remove all data (full reset)
```
docker-compose down -v
```

---

## Service URLs (after startup)

| Service | URL |
|---------|-----|
| API Gateway (entry point) | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |
| Zipkin Tracing | http://localhost:9411 |
| Auth Service Swagger | http://localhost:8081/swagger-ui.html |
| Policy Service Swagger | http://localhost:8082/swagger-ui.html |
| Claim Service Swagger | http://localhost:8083/swagger-ui.html |
| Admin Service Swagger | http://localhost:8084/swagger-ui.html |
| Payment Service Swagger | http://localhost:8085/swagger-ui.html |

---

## Before Running — Update These Values

### 1. Email (claimService) — in docker-compose.yml
```yaml
SPRING_MAIL_USERNAME: your-gmail@gmail.com
SPRING_MAIL_PASSWORD: your-16-char-app-password
```

### 2. Razorpay (paymentService) — in docker-compose.yml
```yaml
RAZORPAY_KEY_ID: rzp_test_your_actual_key
RAZORPAY_KEY_SECRET: your_actual_secret
```

---

## How Environment Variables Override Properties

Spring Boot automatically maps environment variables to properties:
- `SPRING_DATASOURCE_URL` → `spring.datasource.url`
- `SPRING_DATA_REDIS_HOST` → `spring.data.redis.host`
- `SPRING_RABBITMQ_HOST` → `spring.rabbitmq.host`
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` → `eureka.client.serviceUrl.defaultZone`

So your `application.properties` files stay unchanged for local development.
Docker overrides them via environment variables in docker-compose.yml.

---

## Troubleshooting

### Service fails to start — "Connection refused to postgres"
The service started before postgres was ready. Run:
```
docker-compose restart auth-service
```
Or wait 30 seconds and try again. The `restart: on-failure` policy handles this automatically.

### Port already in use
Stop your locally running services first, then run docker-compose.

### Out of memory
Increase Docker Desktop memory to 8GB:
Docker Desktop → Settings → Resources → Memory → 8GB

### Rebuild a single service after code change
```
docker-compose up --build auth-service
```

### Connect to PostgreSQL inside Docker
```
docker exec -it smartsure-postgres psql -U postgres -d auth_db
```

### View all databases
```
docker exec -it smartsure-postgres psql -U postgres -c "\l"
```
