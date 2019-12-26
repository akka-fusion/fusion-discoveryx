FROM cassandra:latest

#RUN localedef -i zh_CN -c -f UTF-8 -A /usr/share/locale/locale.alias zh_CN.UTF-8

ENV LANG zh_CN.utf8

COPY ./cassandra-schema.cql /docker-entrypoint-initdb.d/
