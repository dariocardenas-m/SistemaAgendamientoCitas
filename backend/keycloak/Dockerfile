FROM node:20-alpine AS realm
WORKDIR /realm
COPY backend/keycloak/piedrazul-realm.json backend/keycloak/render-realm.mjs ./
RUN node render-realm.mjs

FROM quay.io/keycloak/keycloak:26.8.2
ENV KC_DB=postgres \
    KC_HTTP_ENABLED=true \
    KC_HTTP_PORT=10000 \
    KC_PROXY_HEADERS=xforwarded \
    JAVA_OPTS_KC_HEAP="-Xms128m -Xmx256m"
RUN /opt/keycloak/bin/kc.sh build
COPY --from=realm --chown=keycloak:keycloak /realm/cloud/piedrazul-realm.json /opt/keycloak/data/import/piedrazul-realm.json
EXPOSE 10000
ENTRYPOINT ["/bin/bash", "-ec", "exec /opt/keycloak/bin/kc.sh start --optimized --import-realm --hostname=\"${KC_HOSTNAME:-${RENDER_EXTERNAL_URL:?Falta el hostname HTTPS de Keycloak}}\""]
