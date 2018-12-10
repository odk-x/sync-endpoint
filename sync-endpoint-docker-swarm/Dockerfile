FROM tomcat:8.5

COPY init.sh /tmp/init.sh
COPY server.xml conf/

COPY target/dependency/sync-endpoint-war.war /
RUN chmod +x /tmp/init.sh && \
    rm -rf /usr/local/tomcat/webapps/ROOT && \
    unzip /sync-endpoint-war.war -d /usr/local/tomcat/webapps/ROOT && \
    rm /sync-endpoint-war.war

EXPOSE 8443
ENTRYPOINT ["/tmp/init.sh"]
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
