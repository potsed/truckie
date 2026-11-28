# Multi-arch (amd64/arm64) GraphHopper runtime for OCI Ampere Free Tier.
# Pin the release; bump intentionally when operators re-import the graph.
ARG GH_VERSION=11.1
FROM eclipse-temurin:21-jre-jammy

ARG GH_VERSION
ENV GRAPHHOPPER_VERSION=${GH_VERSION}
ENV JAVA_OPTS="-Xmx8g -Xms2g"

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl ca-certificates \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --system --uid 10001 --home-dir /graphhopper --create-home graphhopper

WORKDIR /graphhopper

RUN curl -fsSL -o graphhopper-web.jar \
    "https://repo1.maven.org/maven2/com/graphhopper/graphhopper-web/${GH_VERSION}/graphhopper-web-${GH_VERSION}.jar" \
  && chown -R graphhopper:graphhopper /graphhopper

USER graphhopper

# Config + PBF + graph-cache mounted at runtime
VOLUME ["/data", "/graphhopper/config", "/graphhopper/custom-models"]

EXPOSE 8989

# Entry: import-or-serve. First boot builds graph under /data/graph-cache.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Ddw.graphhopper.datareader.file=/data/australia-seq.osm.pbf -jar /graphhopper/graphhopper-web.jar server /graphhopper/config/config.yml"]
