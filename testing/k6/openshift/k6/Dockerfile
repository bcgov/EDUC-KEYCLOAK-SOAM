FROM loadimpact/k6:latest

WORKDIR /var/opt

COPY ../../src/tests /var/opt/scripts
COPY ./start.sh /var/opt

ENTRYPOINT [ "sh", "/var/opt/start.sh" ]
