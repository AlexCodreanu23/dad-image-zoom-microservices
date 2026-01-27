FROM critoma/linux-u20-dev-security-ism:latest
WORKDIR /app
COPY . /app
RUN apt-get update && apt-get install -y maven
RUN mvn -q clean package

ENV RMI_PORT=1099
ENV RMI_NAME=ZoomService
CMD ["bash", "-c", "java -jar target/*.jar"]