How to set server location
(e.g. http://www.sieuferd.com/studycaster_devel)
* ServerSide project->Properties->Run Configuration->Project URL
  (e.g. http://www.sieuferd.com/studycaster_devel/)
* ServerSide project->Properties->Run Configuration->Remote Connection
  (e.g. FTP, host name = www.sieuferd.com, initial directory = "/")
* ServerSide project->Properties->Run Configuration->Upload Directory
  (e.g. /studycaster_devel)
* Set the "studycaster.server-script-url" property in nbproject/project.properties
  (e.g. to "http://www.sieuferd.com/studycaster/server.php")


Build Notes (for NetBeans)
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
  (http://tango.freedesktop.org/Tango_Icon_Library)
* An early version of this software used the java-remote-control screencasting library
  (http://code.google.com/p/java-remote-control)

