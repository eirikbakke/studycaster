StudyCaster README
  Eirik Bakke (ebakke@mit.edu)

Other Notes
* It's useful to test network functionality using NetLimiter
  (http://www.netlimiter.com)

Acknowledgements
* Icon was from the Tango Icon Library
    http://tango.freedesktop.org/Tango_Icon_Library
* The RLE+GZIP encoding was inspired by the one used by the java-remote-control
  project:
    http://code.google.com/p/java-remote-control

Known Issues
* Under certain firewall configurations, notably while running
  COMODO Internet Security 5.4, the client may receive an intermittent error
  "Splash: recv failed" when opening the application via Java Web Start. There
  seems to be little that can be done about this problem from a deployment point
  of view, and I would not recommend asking users to turn off their firewalls.
  See http://lopica.sourceforge.net/faq.html ("The Splash Screen Firewall
  Dead-Lock.") for a related issue.
