StudyCaster README
  Eirik Bakke (ebakke@mit.edu)

Installation Notes
* If you downloaded a precompiled archive, just unpack it onto some public directory of a web
  server that supports PHP. Then browse to that directory on the web to see an example
  deployment page with a Java Web Start link and button.
* To use or update a GeoIP database, download and gunzip a new version of GeoLiteCity.dat from
    http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz
  By default, the "GeoLiteCity.dat" file should be placed in the directory one level above that
  of "index.php". This way, the database won't end up being uploaded or copied around as much
  during deployment or development.

Build Notes (for NetBeans)
* To set server location
  (e.g. http://www.example.com/studycaster_devel)
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
     (if project was previously cleaned, or to reset logs, select all files for uploading)
* To clean:
  * Remove the StudyCaster directory on the server.
  * Clean the StudyCaster project
  * Clean the SCNative project
  * Remove the StudyCaster application from the Java Web Start cache
    (Control Panel->Java->Temporary Internet Files->View...)

Acknowledgements
* Icon was from the Tango Icon Library
    http://tango.freedesktop.org/Tango_Icon_Library
* An early version of this software used the java-remote-control screencasting library
    http://code.google.com/p/java-remote-control
* The GeoIP PHP API and database are from MaxMind
  Main page:
    http://www.maxmind.com/app/php
    http://www.maxmind.com/app/geolitecity
  Download locations:
    http://geolite.maxmind.com/download/geoip/api/php
    http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz
