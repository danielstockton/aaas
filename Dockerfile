FROM java:openjdk-8

RUN apt-get update

ADD target/aaas-0.1.0-SNAPSHOT-standalone.jar /srv/aaas.jar

EXPOSE 3000

VOLUME ["/config"]

CMD ["java", "-cp", "/srv/aaas.jar:/config/", "aaas.core"]
