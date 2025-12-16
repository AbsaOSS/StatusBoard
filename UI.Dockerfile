# build via (docker root = project root):
# docker build -t absagroup/status-board-ui:latest -f UI.Dockerfile .
#
# run via:
# docker run -p 4200:4200 absagroup/status-board-ui:latest >>log.log 2>>err.log &
#
# URL:
# http://localhost:4200

ARG BASE_IMAGE=nginx

FROM $BASE_IMAGE
LABEL org.opencontainers.image.authors="ABSA"

# Project should be build locally by running `npm run build`
COPY ./ui/dist/ui/browser/ /usr/share/nginx/html
COPY ./ui/conf/nginx.conf /etc/nginx/conf.d/default.conf

RUN chmod -R 777 /var/cache/nginx /var/run

USER nginx

EXPOSE 4200

CMD ["nginx", "-g", "daemon off;"]
