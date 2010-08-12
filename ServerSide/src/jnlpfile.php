<?php
function output_jnlpfile($app_args) {
  $xml_args = '';
  foreach ($app_args as $arg)
    $xml_args .= '  <argument>' . $arg . "</argument>\n";
  header("Content-Type: application/x-java-jnlp-file");
  header('Content-Disposition: attachment; filename="studycaster.jnlp"');

  echo <<<EOD
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<jnlp codebase="http://www.sieuferd.com/studycaster" href="server.php?cmd=lnc" spec="1.0+">
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
  <jar eager="true" href="app/StudyCaster.jar" main="true"/>
  <jar eager="true" href="app/lib/apache-mime4j-0.6.jar"/>
  <jar eager="true" href="app/lib/commons-codec-1.3.jar"/>
  <jar eager="true" href="app/lib/commons-logging-1.1.1.jar"/>
  <jar eager="true" href="app/lib/httpclient-4.0.1.jar"/>
  <jar eager="true" href="app/lib/httpcore-4.0.1.jar"/>
  <jar eager="true" href="app/lib/httpmime-4.0.1.jar"/>
  <nativelib eager="true" href="app/lib/sc-native.jar"/>
</resources>
<application-desc main-class="no.ebakke.studycaster.applications.ExcelLauncher">
$xml_args</application-desc>
</jnlp>
EOD;
}
?>