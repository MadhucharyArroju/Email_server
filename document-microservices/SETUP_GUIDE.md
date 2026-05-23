# Document Microservices - Complete Setup Guide

## Step 1: Extract ZIP File

Extract `document-microservices.zip` to your desired location.

## Step 2: Open in IntelliJ

1. Open IntelliJ IDEA
2. File → Open → Select the `document-microservices` folder
3. Click "Open as Project"
4. Wait for Maven to download dependencies (may take 2-3 minutes)

## Step 3: Configure PostgreSQL Database

### Option A: Using pgAdmin (GUI)
1. Open pgAdmin
2. Create database: `document_service`
3. Create user: `doc_user` with password `password123`
4. Grant privileges

### Option B: Using Terminal
```bash
psql -U postgres

CREATE DATABASE document_service;
CREATE USER doc_user WITH PASSWORD 'password123';
ALTER ROLE doc_user SET client_encoding TO 'utf8';
ALTER ROLE doc_user SET default_transaction_isolation TO 'read committed';
GRANT ALL PRIVILEGES ON DATABASE document_service TO doc_user;
\q
```

## Step 4: Configure Email Service (Gmail)

1. Open `email-service/src/main/resources/application.yml`
2. Update credentials:
   ```yaml
   spring:
     mail:
       username: your-email@gmail.com
       password: your-app-password
   ```

3. Get Gmail App Password:
   - Go to https://myaccount.google.com/apppasswords
   - Enable 2-Factor Authentication first
   - Select "Mail" and "Windows Computer"
   - Click Generate
   - Copy the 16-character password
   - Paste into `application.yml`

## Step 5: Build Project

In IntelliJ Terminal:
```bash
mvn clean install
```

Or use IntelliJ's Maven panel:
- View → Tool Windows → Maven
- Click the refresh icon
- Right-click project → Maven → Clean
- Right-click project → Maven → Install

## Step 6: Run Services (IntelliJ Run Configurations)

### Create Run Configuration for Eureka Server:
1. Run → Edit Configurations
2. Click "+" → Maven
3. Name: "Eureka Server"
4. Working directory: Select `eureka-server` folder
5. Command line: `spring-boot:run`
6. Click OK

### Create Run Configuration for Document Service:
1. Run → Edit Configurations
2. Click "+" → Maven
3. Name: "Document Service"
4. Working directory: Select `document-service` folder
5. Command line: `spring-boot:run`
6. Click OK

### Create Run Configuration for Email Service:
1. Run → Edit Configurations
2. Click "+" → Maven
3. Name: "Email Service"
4. Working directory: Select `email-service` folder
5. Command line: `spring-boot:run`
6. Click OK

## Step 7: Start Services (In Order)

1. **Start Eureka Server:**
   - Run → Run "Eureka Server"
   - Wait for: "Eureka Server started"

2. **Start Document Service:**
   - Run → Run "Document Service"
   - Wait for: "Tomcat started on port(s): 8001"

3. **Start Email Service:**
   - Run → Run "Email Service"
   - Wait for: "Tomcat started on port(s): 8002"

## Step 8: Verify All Services

**Eureka Dashboard:**
- Open: http://localhost:8761
- Should see DOCUMENT-SERVICE and EMAIL-SERVICE in registry

**Document Service Health:**
- Open: http://localhost:8001/api/documents
- Should see empty array `[]`

**Email Service Health:**
- Open: http://localhost:8002/api/emails/health
- Should see: `{"status":"UP","service":"EMAIL-SERVICE"...}`

## Step 9: Import Postman Collection

1. Open Postman
2. Click "Import"
3. Select `Document_Microservices.postman_collection.json`
4. Click Import
5. Now you have ready-to-use API requests!

## Step 10: Test APIs

### Upload Document:
```
POST http://localhost:8001/api/documents
Body: {
  "documentName": "TestDoc.pdf",
  "documentType": "PDF",
  "filePath": "/docs/test.pdf",
  "uploadedBy": "john@example.com",
  "status": "UPLOADED"
}
```

### Send Email:
```
POST http://localhost:8002/api/emails/send
Body: {
  "to": "recipient@example.com",
  "subject": "Test",
  "body": "Hello from microservices",
  "from": "your-email@gmail.com"
}
```

## 🆘 Troubleshooting

### Port Already in Use
- Kill process: `lsof -i :8761` (Mac/Linux) or `netstat -ano` (Windows)
- Or change port in application.yml

### Maven Dependencies Not Downloaded
- Delete `.m2` folder
- Delete `target` folders
- Run `mvn clean install` again

### Services Not Registering
- Check Eureka URL in application.yml
- Verify @EnableDiscoveryClient annotation
- Check logs for errors

### Database Connection Error
- Verify PostgreSQL is running
- Check credentials in application.yml
- Verify database exists

### Email Not Sending
- Verify Gmail app password
- Check 2FA is enabled
- Verify credentials in email-service application.yml

## 📋 Checklist

- [ ] PostgreSQL installed and running
- [ ] Database "document_service" created
- [ ] User "doc_user" created
- [ ] Project imported in IntelliJ
- [ ] Maven dependencies downloaded
- [ ] Email credentials configured
- [ ] Eureka Server started (8761)
- [ ] Document Service started (8001)
- [ ] Email Service started (8002)
- [ ] All services visible on Eureka dashboard
- [ ] Postman collection imported

## ✅ Success!

Once all services are running and Eureka shows all services, you're ready to go!

Start testing APIs with the included Postman collection.

---

**Need Help?**
- Check application logs for errors
- Verify firewall settings
- Check network connectivity
- Review configuration files
