FROM critoma/linux-u20-dev-security-ism:latest

WORKDIR /app

COPY . /app

RUN apt-get update && apt-get install -y maven
RUN mvn clean install