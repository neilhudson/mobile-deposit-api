FROM kmadel/java:8
VOLUME /tmp
#ADD ${project.build.finalName}.jar app.jar
ADD mobile-deposit-api.jar app.jar
RUN bash -c 'touch /app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]