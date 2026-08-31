FROM debian:stretch-slim
RUN echo "deb [trusted=yes] http://snapshot.debian.org/archive/debian/20180810T025124Z/ stretch main" > /etc/apt/sources.list \
 && echo "deb [trusted=yes] http://snapshot.debian.org/archive/debian-security/20180810T025124Z/ stretch/updates main" >> /etc/apt/sources.list \
 && (apt-get -o Acquire::Check-Valid-Until=false update || true) \
 && apt-get -o Acquire::Check-Valid-Until=false install -y --no-install-recommends openjdk-8-jdk-headless ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
RUN curl -sL -o fastjson-1.2.24.jar https://repo1.maven.org/maven2/com/alibaba/fastjson/1.2.24/fastjson-1.2.24.jar
COPY VictimServer.java ./
RUN javac -cp fastjson-1.2.24.jar VictimServer.java
EXPOSE 8090
CMD ["java", "-cp", ".:fastjson-1.2.24.jar", "VictimServer"]
