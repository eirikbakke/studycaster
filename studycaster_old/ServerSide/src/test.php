<?php

function fetch_config($config_id) {
  $ini = parse_ini_file('files/studyconfig.ini', true);
  return $ini;
}

var_dump(fetch_config('6728'))

?>
