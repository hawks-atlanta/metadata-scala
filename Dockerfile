# ---- ---- ---- ----
# Build stage
FROM openjdk:22-jdk-slim as builder

WORKDIR /app

# Install scala dependencies
RUN apt update && \
    apt install -y curl && \
    curl -fL "https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz" | gzip -d > cs && \
    chmod +x cs && \
    ./cs setup -y && \
    rm -f cs

# Copy binaries to the /user/local/bin directory
RUN mv ~/.local/share/coursier/bin/** /usr/local/bin

# Copy project files
COPY . .

# Build project
RUN sbt clean && \
    sbt assembly

# Rename jar file
RUN mv target/scala-2.13/*.jar target/scala-2.13/bundle.jar

# ---- ---- ---- ----
# Run stage
FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.20_8 as runner

WORKDIR /app

# Copy binaries from builder
COPY --from=builder /app/target/scala-2.13/bundle.jar .

# Run
# EXPOSE 8080
CMD ["java", "-jar", "/app/bundle.jar"]