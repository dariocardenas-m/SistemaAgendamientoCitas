FROM node:20-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
ARG VITE_API_BASE_URL=/api
ARG VITE_AUTH_PROVIDER=local
ARG VITE_KEYCLOAK_URL
ARG VITE_KEYCLOAK_REALM=piedrazul
ARG VITE_KEYCLOAK_CLIENT_ID=piedrazul-app
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL} \
    VITE_AUTH_PROVIDER=${VITE_AUTH_PROVIDER} \
    VITE_KEYCLOAK_URL=${VITE_KEYCLOAK_URL} \
    VITE_KEYCLOAK_REALM=${VITE_KEYCLOAK_REALM} \
    VITE_KEYCLOAK_CLIENT_ID=${VITE_KEYCLOAK_CLIENT_ID}
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
RUN mvn --batch-mode -f backend/pom.xml dependency:go-offline
COPY backend/src backend/src
COPY --from=frontend-build /workspace/frontend/dist backend/src/main/resources/static
RUN mvn --batch-mode -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend-build /workspace/backend/target/sistema-agendamiento-citas-1.0.0.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=35.0 -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -Duser.timezone=America/Bogota"
EXPOSE 8080
ENTRYPOINT ["sh", "-ec", "if [ -n \"${RENDER_DATABASE_URL:-}\" ]; then export DB_URL=\"jdbc:postgresql://${RENDER_DATABASE_URL##*@}\"; fi; exec java -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
