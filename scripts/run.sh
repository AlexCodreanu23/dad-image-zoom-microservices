#!/bin/bash

echo "=== DAD Image Zoom Microservices ==="
echo ""

# Build Java projects
echo "Building Java projects..."

cd common-rmi
mvn clean install -q
cd ..

cd c01-backend-javalin
mvn clean package -q
cd ..

cd c03-ejb-mdb-rmi-client
mvn clean package -q
cd ..

cd c04-rmi-server-1
mvn clean package -q
cd ..

cd c05-rmi-server-2
mvn clean package -q
cd ..

echo "Java projects built successfully!"
echo ""

# Start Docker containers
echo "Starting Docker containers..."
docker compose up --build