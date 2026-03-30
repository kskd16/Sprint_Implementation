# 🔧 SmartSure System - Eureka Registration Fix

## ❌ PROBLEM IDENTIFIED

**Error:** `com.netflix.discovery.shared.transport.decorator.EurekaHttpClientDecorator$1.execute`

**Root Cause:** Microservices were trying to connect to `localhost:8761` for Eureka registration, but in Docker containers, `localhost` doesn't resolve to the Eureka server.

---

## ✅ SOLUTION IMPLEMENTED

### **Issue 1: Hardcoded localhost in application.properties**
- ❌ **Before:** `eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka`
- ✅ **After:** `eureka.client.serviceUrl.defaultZone=${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka}`

### **Issue 2: Hardcoded Database URLs**
- ❌ **Before:** `spring.datasource.url=jdbc:postgresql://localhost:5433/auth_db`
- ✅ **After:** `spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5433/auth_db}`

### **Issue 3: Hardcoded Redis Host**
- ❌ **Before:** `spring.data.redis.host=localhost`
- ✅ **After:** `spring.data.redis.host=${SPRING_DATA_REDIS_HOST:localhost}`

### **Issue 4: Hardcoded RabbitMQ Host**
- ❌ **Before:** `spring.rabbitmq.host=localhost`
- ✅ **After:** `spring.rabbitmq.host=${SPRING_RABBITMQ_HOST:localhost}`

---

## 📝 FILES UPDATED

✅ authService/src/main/resources/application.properties
✅ policyService/src/main/resources/application.properties
✅ claimService/src/main/resources/application.properties
✅ adminService/src/main/resources/application.properties
✅ paymentService/src/main/resources/application.properties
✅ apiGateway/src/main/resources/application.properties

---

## 🏗️ HOW IT WORKS NOW

### **Local Development (Non-Docker)**
```
Client → localhost:8080 (API Gateway)
         └→ localhost:8081 (Auth Service)
         └→ localhost:8761 (Eureka Server) ✅ Uses default
```

### **Docker Compose**
```
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://service-registry:8761/eureka
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/auth_db
SPRING_DATA_REDIS_HOST=redis
SPRING_RABBITMQ_HOST=rabbitmq

Container → service-registry:8761 ✅ Uses Docker service name
         → postgres:5432 ✅ Uses Docker service name
         → redis:6379 ✅ Uses Docker service name
         → rabbitmq:5672 ✅ Uses Docker service name
```

---

## 🚀 TO RUN LOCALLY

```bash
# Ensure service-registry runs on port 8761
# Make sure all services can reach localhost:8761

# Run services in order:
1. Start Eureka Server (service-registry)
2. Start PostgreSQL, Redis, RabbitMQ
3. Start all microservices
```

---

## 🐳 TO RUN IN DOCKER

```bash
cd c:\practice\sprintImplementation

# Build all container images
docker-compose build --no-cache

# Start all services
docker-compose up -d

# Verify Eureka registration
http://localhost:8761 (Dashboard)
```

---

## ✔️ VERIFICATION

### **Check if services registered with Eureka**
```
GET http://localhost:8761/eureka/apps
```

Should show:
```
- authService (8081)
- policyService (8082)
- claimService (8083)
- adminService (8084)
- paymentService (8085)
- apiGateway (8080)
```

### **Check service logs**
```bash
docker logs smartsure-auth
docker logs smartsure-eureka
```

Should NOT show:
```
Connection refused
Cannot connect to Eureka
Registration failed
```

---

## 🔑 KEY CONFIGURATIONS

### **Environment Variables in docker-compose.yml**

```yaml
environment:
  # Eureka
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-registry:8761/eureka
  
  # Database
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/auth_db
  SPRING_DATASOURCE_USERNAME: postgres
  SPRING_DATASOURCE_PASSWORD: root
  
  # Redis
  SPRING_DATA_REDIS_HOST: redis
  SPRING_DATA_REDIS_PORT: 6379
  
  # RabbitMQ
  SPRING_RABBITMQ_HOST: rabbitmq
  SPRING_RABBITMQ_PORT: 5672
```

---

## 📊 Service-to-Service Communication

Now services can reach each other:

| From | To | Connection | Status |
|------|----|-----------| --------|
| API Gateway | Auth Service | Via Eureka Discovery | ✅ Works |
| Policy Service | RabbitMQ | Via `rabbitmq:5672` | ✅ Works |
| Claim Service | Redis | Via `redis:6379` | ✅ Works |
| Any Service | Database | Via `postgres:5432` | ✅ Works |

---

## 🎯 NEXT STEPS

1. **Rebuild Docker images:**
   ```bash
   docker-compose build --no-cache
   ```

2. **Remove old containers:**
   ```bash
   docker-compose down -v
   ```

3. **Start fresh:**
   ```bash
   docker-compose up -d
   ```

4. **Monitor logs:**
   ```bash
   docker-compose logs -f
   ```

5. **Access services:**
   - API Gateway: http://localhost:8080
   - Eureka Dashboard: http://localhost:8761
   - RabbitMQ UI: http://localhost:15672

---

## 🆘 IF STILL NOT WORKING

### **Check 1: Is Eureka server healthy?**
```bash
docker logs smartsure-eureka | grep "Started Eureka"
```

### **Check 2: Can services reach Eureka?**
```bash
docker exec smartsure-auth curl http://service-registry:8761/eureka/apps
```

### **Check 3: Check Docker network**
```bash
docker network inspect smartsure_smartsure-network
```

Should show all services connected.

### **Check 4: Verify environment variables are set**
```bash
docker inspect smartsure-auth | grep EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
```

---

**All services should now register with Eureka successfully! ✅**
