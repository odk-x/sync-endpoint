# Full Maven Development Environment Configuration

1. Install Maven 3.  This document assumes Maven 3.3.3 or higher.
    This will generally set up a maven repository under
    the user's home directory:  ${HOME}/.m2/repository
1. Update your path so that mvn is recognized.
1. Install Ant. This document assumes Ant 1.9.6 or higher.
1. Update your path so that ant is recognized.
1. Install Java 8 JDK.
1. Configure JAVA_HOME to point to Java 8 JDK.
1. Configure your PATH to have Java 8.
    `java -version` should indicate that java 8 is being accessed. 
1. Install Eclipse Mars.
1. Install Google Eclipse Plugin with App Engine SDK and Google Web Toolkit SDK.
1. Install Tomcat 8.0.
    This is required.
	
1. Install Postgres
   This must be Postgres 9.4 or higher. The project now uses Java 8 and JDBC 4 drivers.

   For Postgres, run these commands:

   ```
   create database "odk_unit";
   create schema "odk_unit";
   create user "odk_unit" with unencrypted password 'odk_unit';
   grant all privileges on database "odk_unit" to "odk_unit";
   alter database "odk_unit" owner to "odk_unit";
   ```

   From the Postgres SQL shell (psql) commandline client,
   using the root account and password, if the above commands
   are in the file postgres.sql, you can type:

   ```
   \cd C:/your_path_no_spaces_forward_slashes_only
   \i postgres.sql
   \q
   ```

1. Install MySQL
   This **MUST** be version 5.6 or higher. The project now uses Java 8 and JDBC 4 drivers.
   And it uses database features only available in newer MySQL databases.
   
   For MySQL, run this script:

   ```
   UPDATE mysql.user SET Password=PASSWORD('odk_unit') WHERE User='root';
   FLUSH PRIVILEGES;
   CREATE USER 'odk_unit'@'localhost' IDENTIFIED BY 'odk_unit';
   CREATE DATABASE odk_unit;
   GRANT ALL PRIVILEGES ON odk_unit.* TO 'odk_unit'@'localhost' WITH GRANT OPTION;
   ```

   For MySQL, download and copy the MySQL Connector J 5.1.40 (or higher) jar into the Tomcat /lib directory (mysql-connector-java-5.1.40.jar to apache-tomcat-8.0.38/lib).
      
   You must stop tomcat, if it is running, in order for the library to be detected.
   
1. Install Microsoft SQL Server (on some accessible host)

1. Install Microsoft SQL Server client:
    on Windows: Microsoft SQL Server Management Studio
    on Linux/MacOSX: SQL Server workbench (http://www.sql-workbench.net/)      

   We use SQL Server authentication for connecting to SQL Server. On Windows, you can
   use Windows authentication, which allows the jdbc.properties files to not contain passwords.
   In the maven build, we expect certain hardcoded usernames and passwords.
   
	Ensure that you can connect to SQL Server that you have installed on a Windows machine.

   For SQLServer, run this script:

   ```
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

1. Register libraries in Maven: (this is also required for Eclipse builds)

   Run the ANT script (build.xml) under:

   `src/main/libs/` -- registers various jars into your local maven repo.

   To run, just cd to this directory and type `ant`

   See the src/main/libs/readme.txt for information about these jars.

1. Download and install Chrome. The test scripts now use Chrome for the selenium testing rather than Firefox, which changed so often as to be unusable. You might need to update selenium and the Chrome Driver for UI testing to work.  This is done in the build\build.xml file.

1. Download the selenium java client for full-stack integration / web UI tests.

   Run the ANT script (build.xml) under:

   `build/`   -- downloads the selenium java client and chrome driver (for full-stack integration / web UI tests)

   To run, just cd to this directory and type `ant`
   
1. Edit Maven's settings.xml file (this is in the .m2 directory).

A minimal file is:

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
	<server>
		<id>local.gae</id>
		<username>mitchellsundt@gmail.com</username>
		<password></password>
	</server>
  </servers>
  <profiles>
  	<profile>
  	  <id>gae</id>
  	  <activation><activeByDefault>true</activeByDefault></activation>
  	  <properties>
		<localRepository>${user.home}/.m2/repository</localRepository>
		<jdk8.home>C:\\Program Files\\Java\\jdk1.8.0_112</jdk8.home>
		<jdk7.home>C:\\Program Files\\Java\\jdk1.7.0_80</jdk7.home>
		<temp.home>C:\\Users\\Admin\\AppData\\Local\\Temp</temp.home>
		<bitrock.home>C:\Program Files (x86)\BitRock InstallBuilder Professional 16.7.0</bitrock.home>
	    <keystore.propertyfile>\C:\\Users\\Admin\\keystore\\jarSignerDetails.txt</keystore.propertyfile>
		<headless.operation>no</headless.operation>
		<mysql.client.executable>C:\\Program Files\\MySQL\\MySQL Server 5.6\\bin\\mysql.exe</mysql.client.executable>
		<mysql.root.password>MYSQLROOTPASSWORDHERE</mysql.root.password>
		<postgres.client.executable>C:\\Program Files\\PostgreSQL\\9.4\\bin\\psql.exe</postgres.client.executable>
		<postgres.root.password>POSTGRESQLROOTPASSWORDHERE</postgres.root.password>
		<sqlserver.client.executable>C:\\Program Files\\Microsoft SQL Server\\Client SDK\\ODBC\\130\\Tools\\Binn\\sqlcmd.exe</sqlserver.client.executable>
		<!-- http://www.sql-workbench.net/ -->
		<sqlserver.linux.sqlworkbench.dir>C:\\Users\\Admin\\Downloads\\Workbench-Build122</sqlserver.linux.sqlworkbench.dir>
		<sqlserver.server>localhost</sqlserver.server>
		<sqlserver.database>odk_unit</sqlserver.database>
		<!-- This is the Server Admin Login. It is not the Active Directory Admin -->
		<sqlserver.root.username>odk_owner</sqlserver.root.username>
		<sqlserver.root.password>SQLSERVERROOTPASSWORDHERE</sqlserver.root.password>
		<test.server.hostname>localhost</test.server.hostname> <!-- sprite.cs.washington.edu -->
		<test.server.port>7070</test.server.port>
		<test.server.secure.port>7443</test.server.secure.port>
		<test.server.gae.monitor.port>7075</test.server.gae.monitor.port>
		<!-- A user configured in Active Directory.
		   The URL to the ldap interface for this active directory is
		     "ldaps://{test.server.ldapDomainDClevel2}.{test.server.ldapDomainDClevel1}"
			 e.g., "ldaps://YOURLDAPDOMAIN2.YOURLDAPDOMAIN1"
		   A user that can authenticate to that ldap is 
		      {test.server.username} with password {test.server.password}
		   Below, the TESTACCOUNTUSERNAMEATDOMAINNAMEHERE should be, e.g.: 
		      someuser@YOURLDAPDOMAIN2.YOURLDAPDOMAIN1
		   And this user belongs to the "{test.server.ldapGroupPrefix} site_admin" group. -->
		<test.server.ldapDomainDClevel2>YOURLDAPDOMAIN2</test.server.ldapDomainDClevel2>
		<test.server.ldapDomainDClevel1>YOURLDAPDOMAIN1</test.server.ldapDomainDClevel1>
		<test.server.ldapGroupPrefix>odkdemo</test.server.ldapGroupPrefix>
		<test.server.username>TESTACCOUNTUSERNAMEATDOMAINNAMEHERE</test.server.username>
		<test.server.password>TESTACCOUNTPASSWORDHERE</test.server.password>
		<unix.display>:20.0</unix.display>
		<webdriver.chrome.drivername>chromedriver_win32</webdriver.chrome.drivername>
		<webdriver.chrome.driverext>.exe</webdriver.chrome.driverext>
		<firefox.executable></firefox.executable>
	  </properties>
  	</profile>
  </profiles>
</settings>
```

Be sure to update the paths and passwords to match those of your environment.

If you are running on a Mac, change the webdriver.chrome.driver* entries to:

```
		<webdriver.chrome.drivername>chromedriver_mac64</webdriver.chrome.drivername>
		<webdriver.chrome.driverext></webdriver.chrome.driverext>
```

The ant script at build/build.xml will download both the windows and mac drivers. If you are 
running on Linux (and not Mac OSX), you will need to modify the build.xml to download the 
Linux drivers and modify these entries with the appropriate values for those drivers.

## Maven Command Line Builds

Maven command-line builds are done as follows:

```
mvn clean
```

This cleans the workspace, removing all temporary files.
If this errors out, verify that there are no orphaned java
executables running. If the GAE tests crash, they can leave
a java database background process running.

```
mvn install
```

This will build and install the projects, running the unit tests
against the 4 datastores (Google BigTable, MySQL, Postgresql, SQLServer),
and building the wars for the 4 platforms.
