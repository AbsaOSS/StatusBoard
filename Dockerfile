# build via (docker root = project root):
# docker build -t absagroup/status-board:latest .
#
# build [with customizations] via (docker root = project root):
# docker build -t absagroup/status-board:latest \
# --build-arg BASE_IMAGE=amazoncorretto:21.0.2 \
# --build-arg BUILD_PROXY=http://proxy.example.com:3128 \
# --build-arg PORT=54321 \
# --build-arg TRUSTED_SSL_CERTS=./myTrustedCertsStorage \
# --build-arg JAR_FILE=./someLocation/IWantThisParticularFile.jar \
# .
#
# run via:
# docker run -p 12345:12345 absagroup/status-board:latest >>log.log 2>>err.log &
#
# run [with custom config] via:
# docker run -v /absolute/path/config.conf:/opt/config/config.conf -p 12345:12345 absagroup/status-board:latest >>log.log 2>>err.log &
#
# test via:
# http://localhost:12345/health

ARG BASE_IMAGE=amazoncorretto:21.0.4

FROM $BASE_IMAGE
LABEL org.opencontainers.image.authors="ABSA"

# Proxy if needed, e.g. http://my.proxy.example.com:3128
ARG BUILD_PROXY
# Port exposed by server - should be the same as the one in server's config
ARG PORT=12345
# Directory with TRUSTED certs in PEM format
ARG TRUSTED_SSL_CERTS=./trusted_certs
# status-board jar file
ARG JAR_FILE=./target/scala-2.13/status-board-*.jar

ENV http_proxy=$BUILD_PROXY
ENV https_proxy=$BUILD_PROXY
ENV HTTP_PROXY=$BUILD_PROXY
ENV HTTPS_PROXY=$BUILD_PROXY

COPY ${JAR_FILE} /opt/app/status-board-assembly.jar
COPY ./docker_entrypoint.sh /opt/app/entrypoint.sh
RUN chmod +x /opt/app/entrypoint.sh

RUN mkdir -p /opt/certs
COPY $TRUSTED_SSL_CERTS /opt/certs/

RUN for file in `ls /opt/certs/*.pem`; \
do \
    keytool -import -file $file -alias $file -cacerts -storepass changeit -noprompt; \
done

RUN chmod 755 /opt/app && \
    chmod 644 /opt/app/status-board-assembly.jar && \
    chmod 755 /opt/app/entrypoint.sh

USER 10001:10001

EXPOSE $PORT
WORKDIR /opt/app
ENTRYPOINT ["./entrypoint.sh"]
