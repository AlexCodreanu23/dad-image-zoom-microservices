FROM critoma/linux-u20-dev-security-ism:latest

WORKDIR /app

# copiem TOT proiectul Maven
COPY . /app

# maven pentru build
RUN apt-get update && apt-get install -y maven

# build
RUN mvn -q clean package

# port RMI (doar informativ, RMI e TCP dinamic)
ENV RMI_PORT=1100

# PORNIREA corectă
CMD ["java", "-cp", "target/classes", "app.Main"]
