<?php
function serverURL() {
  # See http://www.webcheatsheet.com/PHP/get_current_page_url.php
  # See http://www.php.net/manual/en/reserved.variables.server.php
  $port = ($_SERVER['SERVER_PORT'] == '80') ? '' : (':' . $_SERVER['SERVER_PORT']);
  $sn = $_SERVER['SCRIPT_NAME'];
  return 'http://' . $_SERVER['SERVER_NAME'] . $port . substr($sn, 0, strpos($sn, "/index.php"));
}

function output_launch() {
  $url  = serverURL();
  $urls = addslashes($url);
  header('Content-Type: text/html; charset=utf-8');
  echo <<<EOD
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
  <head>
    <meta http-equiv="Content-type" content="text/html;charset=utf-8">
    <title>Test page for launching the application via JNLP</title>
  </head>
  <body>
    <p>This page: <a href="$url">$url</a></p>
    <h3>Link version:</h3>
    <p><a href="index.php?cmd=lnc">Launch the application</a></p>
    <h3>Button version:</h3>
    <p>
    <script type="text/javascript" src="http://java.com/js/deployJava.js"></script>
    <script type="text/javascript">
      deployJava.createWebStartLaunchButton("index.php?cmd=lnc", "1.5");
    </script>
    </p>
  </body>
</html>
EOD;
}

function output_jnlpfile($app_args) {
  # TODO: Check with http://pscode.org/janela/
  $url  = serverURL() . "/";
  $urls = addslashes($url);
  $xml_args = '';
  foreach ($app_args as $arg)
    $xml_args .= '  <argument>' . $arg . "</argument>\n";
  header('Content-Type: application/x-java-jnlp-file');
  header('Content-Disposition: attachment; filename="studycaster.jnlp"');
  echo <<<EOD
<?xml version="1.0" encoding="utf-8" standalone="no"?>
<jnlp codebase="$url" href="index.php?cmd=lnc" spec="1.0+">
<information>
  <title>MTurk User Study Screencaster</title>
  <vendor>Eirik Bakke (ebakke@mit.edu)</vendor>
  <homepage href="ebakke@mit.edu"/>
  <description>MTurk User Study Screencaster</description>
  <description kind="short">MTurk User Study Screencaster</description>
  <icon href="app/icons/icon22.png" kind="default" width="22" height="22"/>
  <icon href="app/icons/icon24.png" kind="default" width="24" height="24"/>
  <icon href="app/icons/icon32.png" kind="default" width="32" height="32"/>
  <icon href="app/icons/icon48.png" kind="default" width="48" height="48"/>
  <!-- JNLP seems to scale everything from a 64x64 icon if one is included. -->
  <!--<icon href="app/icons/icon64.png" kind="default" width="64" height="64"/>-->
  <icon href="app/icons/icon128.png" kind="default" width="128" height="128"/>
  <icon href="app/icons/icon256.png" kind="default" width="256" height="256"/>
</information>
<security>
  <all-permissions/>
</security>
<resources>
  <j2se version="1.5+"/>
  <property name="jnlp.studycaster.serveruri" value="$urls"/>
  <jar download="eager" href="app/StudyCaster.jar" main="true"/>
  <jar download="eager" href="app/lib/apache-mime4j-0.6.jar"/>
  <jar download="eager" href="app/lib/commons-codec-1.3.jar"/>
  <jar download="eager" href="app/lib/commons-logging-1.1.1.jar"/>
  <jar download="eager" href="app/lib/httpclient-4.0.1.jar"/>
  <jar download="eager" href="app/lib/httpcore-4.0.1.jar"/>
  <jar download="eager" href="app/lib/httpmime-4.0.1.jar"/>
  <nativelib download="eager" href="app/lib/sc-native.jar"/>
</resources>
<application-desc main-class="no.ebakke.studycaster.applications.ExcelLauncher">
$xml_args</application-desc>
</jnlp>
EOD;
}
?>