FROM openjdk:17-jdk-slim

MAINTAINER luyanchang1104

ENV PARAMS=""

ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ADD target/ai-rag-knowledge-app.jar /ai-rag-knowledge-app.jar

ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /ai-rag-knowledge-app.jar $PARAMS"]
