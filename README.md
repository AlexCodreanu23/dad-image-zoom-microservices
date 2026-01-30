# DAD Image Zoom Microservices

Distributed Application for BMP image zooming using Docker microservices architecture.

## 🏗️ Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  FRONTEND   │────▶│ C01 Javalin │────▶│C02 ActiveMQ │
│  (HTML/JS)  │     │  REST API   │     │ JMS Broker  │
│   :8080     │     │   :7001     │     │   :61616    │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │ JMS Topic
                                               ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   C06 API   │◀────│    MySQL    │◀────│ C03 TomEE   │
│  (Node.js)  │     │   (BLOB)    │     │  EJB MDB    │
│   :7006     │     │   :3306     │     │   :8080     │
└─────────────┘     └─────────────┘     └──────┬──────┘
      │                                        │ RMI
      ▼                              ┌─────────┴─────────┐
┌─────────────┐                      ▼                   ▼
│   MongoDB   │               ┌───────────┐       ┌───────────┐
│   (SNMP)    │               │C04 RMI #1 │       │C05 RMI #2 │
│   :27017    │               │  :1104    │       │  :1105    │
└─────────────┘               └───────────┘       └───────────┘
```

See `diagrams/` folder for detailed architecture diagram.

## 📦 Containers

| Container | Technology | Port | Description |
|-----------|------------|------|-------------|
| C01 | Java Javalin | 7001 | REST API + JMS Publisher |
| C02 | ActiveMQ 5.15.9 | 61616, 8161 | JMS Message Broker |
| C03 | TomEE 8 Plus | 8080 | EJB MDB + RMI Client |
| C04 | Java RMI | 1104 | RMI Zoom Server |
| C05 | Java RMI | 1105 | RMI Zoom Server |
| C06 | Node.js 18 | 7006 | REST API + DB Access |
| MySQL | MySQL 8.0 | 3306 | Image Storage (BLOB) |
| MongoDB | MongoDB 6.0 | 27017 | SNMP Data Storage |

## 🔄 Flow

1. **Frontend** uploads BMP image with zoom parameters (in/out, %)
2. **C01** receives image via REST API, publishes to JMS Topic
3. **C02** (ActiveMQ) routes message to subscribers
4. **C03** (MDB) receives message, calls **C04** or **C05** via RMI
5. **C04/C05** processes zoom (resize image using Java AWT)
6. **C03** saves result to **MySQL** via C06 REST API
7. **C01** returns download URL to frontend
8. **Frontend** downloads processed image from **C06**

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Maven 3.x
- Java 11+

### Run

```bash
# Build and start all containers
./run.sh

# Or manually:
./build.sh
docker compose up --build
```

### Test Upload

```bash
curl -F "file=@test.bmp" -F "zoomType=in" -F "percent=50" http://localhost:7001/upload
```

Response:
```json
{
  "status": "OK",
  "zoomType": "in",
  "percent": 50,
  "requestId": "abc-123-...",
  "downloadUrl": "http://localhost:7006/api/pictures/name/abc-123-..."
}
```

### Frontend

Open `frontend/index.html` in browser or serve it:

```bash
cd frontend
python -m http.server 8080
# Open http://localhost:8080
```

## 🔌 API Endpoints

### C01 - Upload Service (:7001)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/upload` | Upload BMP (form: file, zoomType, percent) |
| GET | `/health` | Health check |

### C06 - Data Service (:7006)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/pictures` | List all images |
| GET | `/api/pictures/:id` | Get image by ID |
| GET | `/api/pictures/name/:requestId` | Get image by request ID |
| POST | `/api/pictures` | Upload image (internal) |
| GET | `/api/snmp` | Get SNMP data (all containers) |
| GET | `/api/snmp/:container` | Get SNMP for specific container |
| POST | `/api/snmp/refresh` | Refresh SNMP readings |

### C02 - ActiveMQ Console (:8161)

- URL: http://localhost:8161/admin/
- Credentials: admin / admin

## 📁 Project Structure

```
├── c01-backend-javalin/     # Javalin REST API + JMS Publisher
├── c02-jms-broker/          # (uses ActiveMQ Docker image)
├── c03-ejb-mdb-rmi-client/  # TomEE MDB + RMI Client
├── c04-rmi-server-1/        # RMI Zoom Server
├── c05-rmi-server-2/        # RMI Zoom Server
├── c06-node-api/            # Node.js REST API
├── common-rmi/              # Shared RMI Interface
├── frontend/                # HTML/JS Frontend
├── diagrams/                # Architecture diagrams
├── scripts/                 # Build & run scripts
├── docker-compose.yml       # Container orchestration
└── README.md
```

## 🛠️ Technologies

- **Java 11** - Backend services
- **Javalin 5** - REST API framework
- **Apache ActiveMQ 5.15.9** - JMS Message Broker
- **Apache TomEE 8 Plus** - Jakarta EE Application Server
- **Java RMI** - Remote Method Invocation
- **Java AWT** - Image processing (zoom)
- **Node.js 18** - REST API
- **Express.js** - Node.js web framework
- **MySQL 8.0** - Relational database (BLOB storage)
- **MongoDB 6.0** - NoSQL database (SNMP data)
- **Docker** - Containerization

## 👤 Author

DAD Project - Distributed Application Development  
Codreanu Alex-Cosmin
