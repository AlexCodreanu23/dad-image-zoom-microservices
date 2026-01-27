# Docker JMS RMI Image Processing System

Distributed system for BMP image zooming using:
- Docker containers
- JMS (ActiveMQ / TomEE)
- Jakarta EE (EJB MDB)
- Java RMI
- Node.js + MySQL + MongoDB
- REST APIs

## Architecture
(see diagrams folder)

## Containers
- C01: Java Javalin Backend + JMS Publisher
- C02: TomEE JMS Broker
- C03: TomEE EJB MDB + RMI Client
- C04/C05: TomEE RMI Servers
- C06: Node.js REST API + MySQL + MongoDB

## How to run
```bash
docker compose build
docker compose up
