# Vulnuris Unified Incident Log RCA Platform  
## Microservices Architecture (Spring Boot)

## 1. Overview
The system is a microservices-based platform for:
- Log ingestion
- Parsing & normalization
- Event correlation
- Graph building
- RCA report generation

## 2. Core Services

| Service | Responsibility | Database |
|--------|---------------|----------|
| API Gateway | Entry point, routing | None |
| Auth Service | Authentication & authorization | PostgreSQL |
| Ingestion Service | Upload, parse, normalize logs | None |
| Event Service | Store & query events | PostgreSQL |
| Correlation Service | Build event graph | Neo4j |
| Report Service | Generate RCA reports | PostgreSQL |

## 3. API Gateway
- Spring Cloud Gateway
- Routes requests
- Validates JWT

Routes:
- /api/auth/** → Auth Service  
- /api/events/** → Event Service  
- /api/ingest/** → Ingestion Service  
- /api/correlation/** → Correlation Service  
- /api/report/** → Report Service  

## 4. Auth Service
- Login, Register
- JWT generation
- Role management

Tech:
- Spring Security
- JWT
- Spring Data JPA

Tables:
- users
- roles
- user_roles

## 5. Communication
Async (Kafka):
- Ingestion → Event
- Event → Correlation
- Correlation → Report

Sync (REST):
- Gateway → services

## 6. Security Flow
1. User logs in → Auth Service
2. JWT generated
3. Gateway validates JWT
4. Request forwarded

## 7. Tech Stack
- Java 21
- Spring Boot 3
- Spring Cloud Gateway
- Spring Security
- Kafka
- PostgreSQL
- Neo4j
- Docker
