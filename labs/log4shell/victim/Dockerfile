FROM debian:stretch-slim
RUN echo "deb [trusted=yes] http://snapshot.debian.org/archive/debian/20180810T025124Z/ stretch main" > /etc/apt/sources.list \
 && echo "deb [trusted=yes] http://snapshot.debian.org/archive/debian-security/20180810T025124Z/ stretch/updates main" >> /etc/apt/sources.list \
 && (apt-get -o Acquire::Check-Valid-Until=false update || true) \
 && apt-get -o Acquire::Check-Valid-Until=false install -y --no-install-recommends openjdk-8-jdk-headless ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
RUN curl -sL -o log4j-core-2.14.1.jar https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.14.1/log4j-core-2.14.1.jar \
 && curl -sL -o log4j-api-2.14.1.jar https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.14.1/log4j-api-2.14.1.jar
COPY VictimServer.java ./
RUN javac -cp log4j-core-2.14.1.jar:log4j-api-2.14.1.jar VictimServer.java
EXPOSE 8080
CMD ["java", "-cp", ".:log4j-core-2.14.1.jar:log4j-api-2.14.1.jar", "VictimServer"]
