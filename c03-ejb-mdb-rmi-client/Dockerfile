FROM tomee:8-jre11-plus

# Copy TomEE configuration for external broker connection
COPY resources.xml /usr/local/tomee/conf/tomee.xml

# Copy the WAR file
COPY target/c03-ejb-mdb.war /usr/local/tomee/webapps/

EXPOSE 8080

CMD ["catalina.sh", "run"]