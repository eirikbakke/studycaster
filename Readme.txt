StudyCaster README
  Eirik Bakke (ebakke@mit.edu)

Installation Notes
* If you downloaded a precompiled archive, just unpack it onto some public
  directory of a web server that supports PHP. Then browse to that directory on
  the web to see an example deployment page with a Java Web Start link and
  button.
* To use or update a GeoIP database, download and gunzip a new version of
  GeoLiteCity.dat from
    http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz
  By default, the "GeoLiteCity.dat" file should be placed in the directory one
  level above that of "index.php". This way, the GeoIP database won't end up
  being uploaded or copied around as much during deployment or development.

Build Notes
* Tested under Windows XP x64 with NetBeans 7.0, JDK 1.5 update 22.
* Install NetBeans with the "PHP" and "C/C++" plugins.
* Install 32-bit JDK 1.5 and add it as a Java platform in NetBeans
  (Tools->Java Platforms). Compiling with this older JDK will ensure backwards
  compatibility.
* Recommended: Download a ZIP file distribution of the Javadoc for JDK 1.5, put
  it in the JDK directory, and point to it from the NetBeans "Java Platform"
  settings. This will enable context-sensitive Javadoc help.
* To be able to compile the native windows libraries
  * Install MinGW and MSYS via mingw-get-inst
    (http://www.mingw.org/wiki/Getting_Started).
  * Register MinGW/MSYS as a tool collection in NetBeans
    (Tools->Options->C/C++->Add...) with the name "MingGW_32". The base
    directory is "C:\MinGW32\bin". Every binary will be from this directory
    except "make" which will be at "C:\MinGW\msys\1.0\bin\make.exe".
    * Note: If Cygwin is installed, make sure _not_ to use any of its binaries;
      they are incompatible with the MSYS ones.
  * Set the SC_JDK_HOME variable in SCNative/Makefile to point to the JDK 1.5
    directory. Also add the "include" and "include/win32" JDK 1.5 directories to
    the include directories for the SCNative C compiler configuration, for both
    the Debug and the Release configuration. For instance:
      C:/Program Files (x86)/Java/jdk1.5.0_22/include;
      C:/Program Files (x86)/Java/jdk1.5.0_22/include/win32
    This hardcoding is ugly and should be avoided in the future.
* To set server location
  (e.g. http://www.example.com/studycaster_devel)
  * ServerSide project->Properties->Run Configuration->Run As
    Remote Web Site (FTP, SFTP)
  * ServerSide project->Properties->Run Configuration->Project URL
    (e.g. http://www.example.com/studycaster_devel/)
  * ServerSide project->Properties->Run Configuration->Remote Connection
    (e.g. FTP, host name = www.example.com, initial directory = "/")
  * ServerSide project->Properties->Run Configuration->Upload Directory
    (e.g. /studycaster_devel)
  * Set the "studycaster.serveruri" property in nbproject/project.properties
    (e.g. to "http://www.example.com/studycaster_devel")
* To build (order matters):
  1) Build the SCNative project
  2) Build the StudyCaster project
  3) Upload the "Source Files" in the ServerSide project
     (if project was previously cleaned, or to reset logs, select all files for
     uploading)
* To clean:
  * Remove the StudyCaster directory on the server.
  * Clean the StudyCaster project
  * Clean the SCNative project
  * Remove the StudyCaster application from the Java Web Start cache
    (Control Panel->Java->Temporary Internet Files->View...)

Acknowledgements
* Icon was from the Tango Icon Library
    http://tango.freedesktop.org/Tango_Icon_Library
* An early version of this software used the java-remote-control screencasting
  library; 
  The RLE+GZIP encoding was inspired by the one used by the java-remote-control
  project:
    http://code.google.com/p/java-remote-control
* The GeoIP PHP API and database are from MaxMind
  Main page:
    http://www.maxmind.com/app/php
    http://www.maxmind.com/app/geolitecity
  Download locations:
    http://geolite.maxmind.com/download/geoip/api/php
    http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz

Known Issues
* Under certain firewall configurations, notably while running
  COMODO Internet Security 5.4, the client may receive an intermittent error
  "Splash: recv failed" when opening the application via Java Web Start. There
  seems to be little that can be done about this problem from a deployment point
  of view, and I would not recommend asking users to turn off their firewalls.
  See http://lopica.sourceforge.net/faq.html ("The Splash Screen Firewall
  Dead-Lock.") for a related issue.

