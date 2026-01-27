FROM critoma/linux-u20-dev-security-ism

WORKDIR /app

COPY . /app

RUN apt-get update && apt-get install -y maven

RUN mvn clean package

EXPOSE 7001

CMD ["java", "-jar", "target/c01-backend-1.0.jar"]
