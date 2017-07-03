build.xml -- ANT script to install various library files into your local Mvn repository.

Run ANT in this directory to register these files with your local maven repository.

The files being installed are not available via mvnrepository.com

----------------------
JARs built by ODK team
----------------------

maven_local_installs -- contains the maven commands to register these jars into your repo.

Summary of what is installed.

# odk-tomcatutil-1.0.1:

This can be installed by pulling the Aggregate (Components) sources
and running 'mvn install' in the TomcatUtils project.  Or,

# gwt-google-maps-v3-1.0.1:

This can be installed by pulling the Aggregate (Components) sources
and building the gwt-google-maps-v3 project. 
See the Aggregate (Components) README.txt file for how this was built. It
uses the sources that were originally located here (but have since been removed):
http://code.google.com/p/gwt-google-maps-v3/

# gwt-visualization-1.1.2:

The sources are in the jar. The full project can be found in 
the Aggregate (Components) project. It is copied from the download
formerly available here:
  http://code.google.com/p/gwt-google-apis/
