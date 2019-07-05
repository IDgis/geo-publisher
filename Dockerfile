FROM azul/zulu-openjdk:11 AS builder

COPY . /
RUN ./gradlew --no-daemon installDist

FROM azul/zulu-openjdk:11

COPY --from=builder /build/install/service-info-service /service-info-service

CMD /service-info-service/bin/service-info-service
