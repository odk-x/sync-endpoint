# sync-endpoint

This project is __*actively maintained*__

The developer [wiki](https://github.com/opendatakit/opendatakit/wiki) (including release notes) and
[issues tracker](https://github.com/opendatakit/opendatakit/issues) are located under
the [**opendatakit**](https://github.com/opendatakit/opendatakit) project.

## Setting up Your Environment

#### Prerequisites

 - Java 8 JRE + JDK (Java 9 not supported)
 - Apache Maven (>= 3.3.3)
 - Apache Ant (>= 1.9.6)
 - Tomcat (>= 8.5)
 - PostgreSQL (>= 8.6) / MySQL (>= 5.7) / MS SQL Server
 - Docker (>= 17.07.1)

#### Recommended 
 
 - a Java IDE (e.g. Eclipse or IntelliJ IDEA)

#### Detailed Configuration Guides

 - [**Minimal Eclipse Installation Setup**](docs/eclipse.md)
   - [**Tomcat Installation Setup**](docs/tomcat.md)
 - [**Full Maven Development Environment Configuration**](docs/maven-full.md)

#### Source Structure

 - `src/`  -- main source code, configuration files, and libraries
 - `war-base/` -- static web content, libraries for Eclipse environment, and Servlet configuration

## Building 

 - Sync Endpoint
   - Sync Endpoint is only meant to be built as a Docker container image. See [sync-endpoint-containers](https://github.com/opendatakit/sync-endpoint-containers)
 - Aggregate REST Interface (odk-tables-api)
   - `mvn -pl "aggregate-src, odk-tables-api" clean package`

## Running

See [sync-endpoint-default-setup](https://github.com/opendatakit/sync-endpoint-default-setup) for typical usage.

## Upgrade Guide

Upgrading versions of software should be done by first updating
the versions in the maven `pom.xml` file located in this directory.

Some of these versions are defined as properties at the top of the 
file so that entire suites of jars (e.g. Spring) can
be upgraded at the same time.

Once that is done, and builds without errors, you should
check that there are no older jars in the `WEB-INF/lib` directories
of the war files (all jars go everywhere at this time, so looking 
at just one should be fine).  If older jars are being pulled in, you
will need to update the dependencies to exclude pulling in those jars;
there are examples of this in the current `pom.xml` and online.

You'll need a pom dependency analyzer to uncover why Maven has
pulled in the down-version jar.

After that, you should copy the jars from the `WEB-INF/lib` directory 
of the build back into the `war-base/WEB-INF/lib`
directory and refresh the eclipse projects to reflect the new set
of jars.  When doing this, you will likely need to hand-edit the 
spring-web jar to remove the servlet-3.0 compatible files:

 - overview.html
 - META-INF/web-fragment.xml

And, of course, test to verify this all works.

## License

This software is licensed under the [**Apache 2.0 license**](http://www.apache.org/licenses/LICENSE-2.0)

