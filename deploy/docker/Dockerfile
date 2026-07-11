ARG RUNTIME_IMAGE=registry.cn-hangzhou.aliyuncs.com/cloudcanal/baseimage:centos8_x86_v2

FROM ${RUNTIME_IMAGE}
ARG COST_BUDDY_UID=10001
ARG COST_BUDDY_GID=10001

RUN groupadd --gid ${COST_BUDDY_GID} costbuddy \
    && useradd --uid ${COST_BUDDY_UID} --gid ${COST_BUDDY_GID} --home-dir /home/costbuddy --create-home --shell /bin/bash costbuddy \
    && mkdir -p /home/costbuddy/app/logs \
    && chown -R costbuddy:costbuddy /home/costbuddy

WORKDIR /home/costbuddy/app
COPY --chown=costbuddy:costbuddy cost-buddy.jar /home/costbuddy/app/cost-buddy.jar
COPY docker-entrypoint.sh /usr/local/bin/cost-buddy-entrypoint.sh
RUN chmod +x /usr/local/bin/cost-buddy-entrypoint.sh

EXPOSE 8766

ENTRYPOINT ["/usr/local/bin/cost-buddy-entrypoint.sh"]
