FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# Dépendances résolues dans une couche à part : elles ne sont retéléchargées que si pom.xml change
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

EXPOSE 8084

# En développement, docker-compose.yml monte ./backoffice sur /app : spring-boot:run recompile
# le code de l'hôte et DevTools recharge l'application à chaud.
CMD ["mvn", "spring-boot:run"]
