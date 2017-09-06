# Minimal Tomcat8 MySQL/PostgreSQL Eclipse Setup

This assumes you have completed the [**Minimal Eclipse Installation Setup**](eclipse.md)

1. Install Tomcat8 on your computer.
1. Database Dependencies
   - download MySQL Connector/J and place it in the lib directory of the Tomcat install. This **MUST** be version 5.1.40 or higher. It is known that there are issues with 5.1.6 and earlier. We have only tested with 5.1.40. Stop and restart the Tomcat8 server so it picks up that library. This must be present for MySQL connections to work. It does not harm anything if this is present when using PostgreSQL.
1. install the database server of your choice (MySQL or PostgreSQL or SQLServer). **NOTE**:  Be sure that it is configured using a UTF-8 character set as the default.

    For MySQL: Stop the MySQL database server, then configure the database (via the `my.cnf` or the `my.ini` file) with these lines added to the `[mysqld]` section:

    ```ini
    character_set_server=utf8
    collation_server=utf8_unicode_ci
    max_allowed_packet=1073741824
    ```
 
    For SQLServer, we configure it to use mixed-mode.
1. For SQLServer, install Microsoft SQL Server client:
    on Windows: Microsoft SQL Server Management Studio
    on Linux/MacOSX: SQL Server workbench (http://www.sql-workbench.net/)
1. Once again, go to Window / Preferences
    - Open Server/Runtime Environment 
    - Select Apache Tomcat v8.0. If it complains about not having a configured runtime, then delete this.
    - Select Add..., Select Apache / Apache Tomcat v8.0 and set it up to point to your installation of Tomcat v8.0 on your system. Do not choose to create a new server; you are re-using the existing server.
    - Click OK to save changes.
1. If you haven't already, go to the Workbench view. Then, Import / Import... / General / Existing Projects into Workspace
      - import these existing projects:
        - odk-mysql-settings
        - odk-posgres-settings
        - eclipse-tomcat8
1. Depending upon which database you want to use:

    **MySQL**

    - open odk-mysql-settings/common
    - edit security.properties to set the hostname to the IP address of your computer.
    - open odk-mysql-settings/mysql
    - edit jdbc.properties to specify a username, password and database name in the url. 

    ```properties
    jdbc.driverClassName=com.mysql.jdbc.Driver
    jdbc.resourceName=jdbc/odk_aggregate
    jdbc.url=jdbc:mysql://127.0.0.1/odk_db?autoDeserialize=true
    jdbc.username=odk_unit
    jdbc.password=test
    jdbc.schema=odk_db
    ```

    - Save changes.
    - Now open MySQL Workbench. If you have not yet created that database, issue the following commands, with the names changed for what you specified above. The names to substitute above/below are:
      - `odk_db` -- replace with your database name
      - `odk_unit` -- replace with your username
      - `test` -- replace with your password

    ```sql
    create database `odk_db`;
    create user 'odk_unit'@'localhost' identified by 'test';
    grant all on `odk_db`.* to 'odk_unit'@'localhost' identified by 'test';
    flush privileges;
    ```
    - Finally, return to Eclipse, select the build.xml script within the odk-mysql-settings
    - project, right-click, Run As / Ant Build.
    - This will bundle up these changes and copy the changes into the eclipse-tomcat8 project.

    **PostgreSQL**

    - open odk-postgres-settings/common
    - edit security.properties to set the hostname to the IP address of your computer.
    - open odk-postgres-settings/postgres
    - edit jdbc.properties to specify a username, password and database name in the url.

    ```properties
    jdbc.driverClassName=org.postgresql.Driver
    jdbc.resourceName=jdbc/odk_aggregate
    jdbc.url=jdbc:postgresql://127.0.0.1/odk_db?autoDeserialize=true
    jdbc.username=odk_unit
    jdbc.password=test
    jdbc.schema=odk_db
    ```

    - Save changes.
    - Now open pgAdmin III. If you have not yet created that database, issue the following commands, with the names changed for what you specified above. The names to substitute above/below are:
      - odk_db -- replace with your database name
      - odk_unit -- replace with your username
      - test -- replace with your password

    ```sql
    create database "odk_db";
    SELECT datname FROM pg_database WHERE datistemplate = false;
    create user "odk_unit" with unencrypted password 'test';
    grant all privileges on database "odk_db" to "odk_unit";
    alter database "odk_db" owner to "odk_unit";
    \c "odk_db";
    create schema "odk_db";
    grant all privileges on schema "odk_db" to "odk_unit";
    ```

    - Finally, return to Eclipse, select the build.xml script within the odk-postgres-settings project, right-click, Run As / Ant Build.
    - This will bundle up these changes and copy the changes into the eclipse-tomcat8 project.

    **SQLServer**

    - open odk-sqlserver-settings/common
    - edit security.properties to set the hostname to the IP address of your computer.
    - open odk-sqlserver-settings/sqlserver
    - edit jdbc.properties to specify a database name in the url. The url is configured to use Windows authentication for accessing the database, so no username or password is present in this file. If you do not want to use Windows authentication, compare the odk_settings.xml file for sqlserver with that for postgres to see where to add settings for username and password so that you can use those for authentication.

	To use standard SQLServer username and password (suitable for all platforms):
	
    ```properties
    jdbc.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver
    jdbc.resourceName=jdbc/odk_aggregate
    jdbc.url=jdbc:sqlserver://127.0.0.1\\MSSQLSERVER:1433;database=odk_unit;user=odk_unit_login;password=odk_unit;integratedSecurity=false;encrypt=true;trustServerCertificate=true;loginTimeout=30
    jdbc.schema=odk_schema
    ```

	If your server is remote, you will need to update the jdbc.url to point to something other than localhost.

    - Save changes.
    - Now open Microsoft SQL Server Management Studio or the SQL Server workbench. If you have not yet created that database, issue the following commands, with the names changed for what you specified above. The names to substitute above/below are:
      - odk_unit -- replace with your database name
      - odk_schema -- replace with the schema name

   For SQL Server username/password authentication:
   
   ```sql
   USE master;
   go
   CREATE DATABASE odk_unit;
   go
   USE odk_unit;
   go
   CREATE LOGIN odk_unit_login with password = 'odk_unit', default_database = odk_unit;
   go
   CREATE USER odk_unit_login from LOGIN odk_unit_login WITH default_schema = dbo;
   go
   grant all privileges on DATABASE::odk_unit to odk_unit_login;
   go
   CREATE SCHEMA odk_schema;
   go
   ```

    - Finally, return to Eclipse, select the build.xml script within the odk-sqlserver-settings project, right-click, Run As / Ant Build.
    - This will bundle up these changes and copy the changes into the eclipse-tomcat8 project.
1. Select eclipse-tomcat8, right-click, Refresh. (to pick up file changes).
1. Select eclipse-tomcat8, right-click, Compile

    Verify the program arguments are

    ```sh
       -war WebContent
    ```

    And the VM arguments are

    ```sh
    -Xmx512m
    ```

    Apply and Compile.
1. Select eclipse-tomcat8, right-click, Refresh.
1. Select eclipse-tomcat8, right-click, properties,
1. Select the Servers tab in the Output area
    - Delete any existing Tomcat v8.0 server.
    - Click to create a server.
    - Choose Tomcat v8.0 Server
    - Enter the IP address of your computer. If you leave this as localhost, then ODK Collect and ODK Briefcase will not be able to fully communicate with your development server.
    - Click Next
    - Configure to deploy eclipse-tomcat8 on this server.
    - Click Finish
1. Select eclipse-tomcat8 project, right-click Properties
    - Go to Project Facets, select Dynamic Web App, click Runtimes tab, verify that Tomcat v8.0 is chosen.
    - Apply
    - Go to Server, select the Tomcat v8.0 server that you just created
    - Apply
    - Click OK
1. You should now be able to run ODK Aggregate on this Tomcat8 server by right-click, Debug As / Debug on Server

    The project may report a validation error (web.xml not found in WebContent). You can ignore this. The web.xml is provided in war-base.

## Tomcat8 Edit-Debug Cycle Considerations

Now, you should be able to Debug the server-side code using the
Tomcat8 development server. When you are developing, as you
change code, you will probably need to start and stop the server.
