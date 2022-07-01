FROM parrotstream/centos-openjdk:8

MAINTAINER YOUXIANGWANG

VOLUME /tmp

ADD target/scala-2.12/orchestrator-assembly.jar app.jar

RUN sh -c 'touch /app.jar'

ENV JAVA_OPTS="-Dserver.port=80 -Dfile.encoding=UTF-8"

# ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]


ENTRYPOINT [ "java", "-jar", "-XX:InitialRAMPercentage=75.0", "-XX:MaxRAMPercentage=75.0", "-XX:MinRAMPercentage=75.0", "-XX:MetaspaceSize=256m", "-XX:MaxMetaspaceSize=256m", "-Dserver.port=80", "-Dfile.encoding=UTF-8", "-Duser.timezone=GMT+8", "-Djava.security.egd=file:/dev/./urandom", "/app.jar" ]
