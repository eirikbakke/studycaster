StudyCaster README
  Eirik Bakke (ebakke@mit.edu)

Other Notes
* It's useful to test network functionality using NetLimiter
  (http://www.netlimiter.com)

Build Requirements
* NetBeans EE 7.0.1
* JDK 1.5 for building backwards-compatible client
* Tomcat (other containers possible)
* JavaDB (other databases possible)
  TODO: Think it should be possible to install this with NetBeans?
* Xuggler

Acknowledgements
* Icon was from the Tango Icon Library
    http://tango.freedesktop.org/Tango_Icon_Library
* The RLE+GZIP encoding was inspired by the one used by the java-remote-control
  project:
    http://code.google.com/p/java-remote-control
* The GeoIP PHP API and database are from MaxMind
    http://www.maxmind.com/app/java
    http://www.maxmind.com/app/geolitecity
  Download an updated database here:
    http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz

Known Issues
* Under certain firewall configurations, notably while running
  COMODO Internet Security 5.4, the client may receive an intermittent error
  "Splash: recv failed" when opening the application via Java Web Start. There
  seems to be little that can be done about this problem from a deployment point
  of view, and I would not recommend asking users to turn off their firewalls.
  See http://lopica.sourceforge.net/faq.html ("The Splash Screen Firewall
  Dead-Lock.") for a related issue.

Configuration
* In nbproject/project.properties
  (override in nbproject/private/private.properties):
  * In sc_server:
      studycaster.jdbc.url=jdbc:derby://localhost:1527/sc_devel;create=true
      studycaster.storage.path=../devel_storagedir
  * In sc_server:
      studycaster.server.uri=http://localhost:8084/sc_server/client
* Put the "GeoLiteCity.dat" file in the storage folder
