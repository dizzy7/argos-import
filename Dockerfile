FROM dockerreg.elama.ru/alpine-openjdk:8-jre

COPY /target/scala-2.12/argos-import_2.12-0.1.1.jar /argos-import.jar

CMD exec java $JAVA_OPTS -jar /argos-import.jar
