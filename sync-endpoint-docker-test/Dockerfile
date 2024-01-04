FROM tomcat:9-jdk11

COPY init.sh /tmp/init.sh
COPY resources/server.xml conf/
COPY resources/logging.properties conf/

COPY target/dependency/mysql-connector.jar lib/
COPY target/dependency/sync-endpoint-war.war /
RUN apt-get update && apt-get install unzip
RUN chmod +x /tmp/init.sh && \
    rm -rf /usr/local/tomcat/webapps/ROOT && \
    unzip /sync-endpoint-war.war -d /usr/local/tomcat/webapps/ROOT && \
    rm /sync-endpoint-war.war

EXPOSE 8888
EXPOSE 8443
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
