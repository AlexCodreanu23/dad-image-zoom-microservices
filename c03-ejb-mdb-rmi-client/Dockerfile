FROM critoma/linux-u20-dev-security-ism

WORKDIR /app
COPY . /app

RUN apt-get update && apt-get install -y maven
RUN mvn clean package

CMD ["bash", "-c", "echo C03 EJB MDB up && sleep infinity"]