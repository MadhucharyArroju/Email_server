# Document Microservices System

Complete microservices architecture with Eureka Service Discovery, Document Service, and Email Service.

## 🏗️ Architecture

```
┌─────────────────────┐
│  EUREKA SERVER      │
│  (Port 8761)        │
└────────┬────────────┘
         │
    ┌────┴─────┐
    │           │
┌───▼──┐  ┌───▼──┐
│DOC   │  │EMAIL │
│SVC   │  │SVC   │
│8001  │  │8002  │
└──────┘  └──────┘
```

## 📋 Prerequisites

- Java 8+
- Maven 3.6+
- PostgreSQL 12+
- Postman (for API testing)

## 🚀 Quick Start

### 1. Database Setup

```bash
psql -U postgres

CREATE DATABASE document_service;
CREATE USER doc_user WITH PASSWORD 'password123';
ALTER ROLE doc_user SET client_encoding TO 'utf8';
ALTER ROLE doc_user SET default_transaction_isolation TO 'read committed';
GRANT ALL PRIVILEGES ON DATABASE document_service TO doc_user;
\q
```

### 2. Build Project

```bash
mvn clean install
```

### 3. Run Services (In Order - 3 Terminals)

**Terminal 1: Eureka Server (Port 8761)**
```bash
cd eureka-server
mvn spring-boot:run
```

**Terminal 2: Document Service (Port 8001)**
```bash
cd document-service
mvn spring-boot:run
```

**Terminal 3: Email Service (Port 8002)**
```bash
cd email-service
mvn spring-boot:run
```

### 4. Verify All Services Running

- Eureka Dashboard: http://localhost:8761
- Should see DOCUMENT-SERVICE and EMAIL-SERVICE registered

## 📚 API Documentation

### Document Service (Port 8001)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/documents` | Get all documents |
| GET | `/api/documents/{id}` | Get document by ID |
| POST | `/api/documents` | Upload document |
| PUT | `/api/documents/{id}` | Update document |
| PUT | `/api/documents/{id}/status` | Update status |
| DELETE | `/api/documents/{id}` | Delete document |

### Email Service (Port 8002)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/emails/health` | Health check |
| POST | `/api/emails/send` | Send email |

## 🔧 Configuration

### Email Service (Gmail)

Update `email-service/src/main/resources/application.yml`:

```yaml
spring:
  mail:
    username: your-email@gmail.com
    password: your-app-password  # Gmail App Password
```

**Get Gmail App Password:**
1. Go to https://myaccount.google.com/apppasswords
2. Select "Mail" and "Windows Computer"
3. Generate and copy the 16-character password

## 📮 Postman Collection

Import `Document_Microservices.postman_collection.json` into Postman to test all endpoints.

## 🐛 Troubleshooting

### Services not registering with Eureka
- Check Eureka URL in application.yml
- Ensure services have @EnableDiscoveryClient
- Check network connectivity

### Database connection error
- Verify PostgreSQL is running
- Check database credentials
- Ensure database exists

### Port already in use
- Change port in application.yml
- Or kill process using the port

## 📝 Project Structure

```
document-microservices/
├── eureka-server/
│   ├── pom.xml
│   └── src/main/java/com/example/eureka/
├── document-service/
│   ├── pom.xml
│   └── src/main/java/com/example/document/
├── email-service/
│   ├── pom.xml
│   └── src/main/java/com/example/email/
└── pom.xml (Parent)
```

## 🎯 Features

✅ Service Discovery (Eureka)
✅ Document Management (CRUD)
✅ Email Notifications
✅ PostgreSQL Integration
✅ Spring Cloud Integration
✅ RESTful APIs
✅ Health Monitoring

## 📖 Next Steps

1. Import into IntelliJ
2. Run database setup script
3. Build with Maven
4. Run all services
5. Import Postman collection
6. Test API endpoints

## 💡 Tips

- Start Eureka first, then other services
- Check Eureka dashboard to verify registration
- Use Postman collection for API testing
- Configure Gmail credentials for email service

---

**Built with:** Spring Boot 2.7, Spring Cloud, PostgreSQL
