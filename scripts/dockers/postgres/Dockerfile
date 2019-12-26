FROM postgres:latest

RUN localedef -i zh_CN -c -f UTF-8 -A /usr/share/locale/locale.alias zh_CN.UTF-8

ENV LANG zh_CN.utf8

COPY postgres-schema.sql /docker-entrypoint-initdb.d/
