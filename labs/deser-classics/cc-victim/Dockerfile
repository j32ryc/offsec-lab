FROM debian:stretch-slim
RUN echo "deb [trusted=yes] http://snapshot.debian.org/archive/debian/20180810T025124Z/ stretch main" > /etc/apt/sources.list \
 && echo "deb [trusted=yes] http://snapshot.debian.org/archive/debian-security/20180810T025124Z/ stretch/updates main" >> /etc/apt/sources.list \
 && (apt-get -o Acquire::Check-Valid-Until=false update || true) \
 && apt-get -o Acquire::Check-Valid-Until=false install -y --no-install-recommends openjdk-8-jdk-headless curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
RUN curl -sL -o commons-collections-3.2.1.jar https://repo1.maven.org/maven2/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar
COPY DeserializeServer.java ./
RUN javac DeserializeServer.java
EXPOSE 7070
CMD ["java", "-cp", ".:commons-collections-3.2.1.jar", "DeserializeServer"]
