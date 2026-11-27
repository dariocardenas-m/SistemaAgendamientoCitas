FROM node:20-alpine AS realm
WORKDIR /realm
COPY backend/keycloak/piedrazul-realm.json backend/keycloak/render-realm.mjs ./
RUN node render-realm.mjs

FROM quay.io/keycloak/keycloak:26.8.2
ENV KC_DB=postgres
RUN /opt/keycloak/bin/kc.sh build
COPY --from=realm --chown=keycloak:keycloak /realm/cloud/piedrazul-realm.json /opt/keycloak/data/import/piedrazul-realm.json
ENV KC_HTTP_ENABLED=true \
    KC_HTTP_PORT=10000 \
    KC_PROXY_HEADERS=xforwarded \
    KC_DB_POOL_MAX_SIZE=10 \
    JAVA_OPTS_KC_HEAP="-Xms64m -Xmx160m" \
    JAVA_OPTS_APPEND="-XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=48m"
EXPOSE 10000
ENTRYPOINT ["/bin/bash", "-ec", "exec /opt/keycloak/bin/kc.sh start --optimized --import-realm --hostname=\"${KC_HOSTNAME:-${RENDER_EXTERNAL_URL:?Falta el hostname HTTPS de Keycloak}}\""]
