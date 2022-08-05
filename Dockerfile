FROM navikt/java:17-appdynamics

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENV SPRING_PROFILES_ACTIVE=nais

COPY ./target/bidrag-person-hendelse.jar "app.jar"
