FROM openjdk:11.0-jre

WORKDIR /home/app

COPY *.jar /home/app/app.jar

RUN useradd -m app

USER app

EXPOSE 9998

CMD [ "java", "-Xms200m", "-Xmx200m", "-Djava.awt.headless=true", "-jar", "app.jar" ]