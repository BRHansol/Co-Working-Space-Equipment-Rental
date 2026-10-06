# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# copy pom ก่อน เพื่อให้ layer ของ dependency ถูก cache ไว้ ไม่ต้องโหลดใหม่ทุกครั้งที่แก้โค้ด
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- run stage ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# ไม่รันแอปด้วย root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
