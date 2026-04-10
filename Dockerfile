#minimal docker file attempting to run the spring boot app
#create image from this dockerfile by running
#"docker build -t dockerized-app:1.0 ." from root dir
#Run the image as docker run -p 8080:8080 dockerized-app:1.0
# OR
# docker compose down
  #docker compose up --build
#uses 2 stages because the final image only needs JRE + jar, not build tool Maven/src files

# Base image: JRE21 like in poms + maven
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Root aggregator pom
COPY pom.xml .

# Module poms
COPY backend/pom.xml backend/pom.xml
COPY core-libs/pom.xml core-libs/pom.xml
COPY domain/pom.xml domain/pom.xml

# Module source
COPY backend/src backend/src
COPY core-libs/src core-libs/src
COPY domain/src domain/src

RUN mvn -pl backend -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# mvn package from above creates jar file, we copy into the image, will be called app.jar
COPY --from=build /workspace/backend/target/backend-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

#will run java jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]