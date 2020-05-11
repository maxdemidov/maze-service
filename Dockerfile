FROM anapsix/alpine-java
ADD target/scala-2.12/*-assembly-*.jar /app.jar
ENTRYPOINT java -jar /app.jar
EXPOSE 9090 9099
