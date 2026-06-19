FROM ubuntu:latest
LABEL authors="tj"

ENTRYPOINT ["top", "-b"]

FROM docker.elastic.co/elasticsearch/elasticsearch:9.4.2

RUN bin/elasticsearch-plugin list | grep -q '^analysis-nori$' \
    || bin/elasticsearch-plugin install --batch analysis-nori