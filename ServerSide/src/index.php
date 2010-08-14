<?php
  define('MAX_FILE_SIZE'      , 50000000);
  define('MAX_APPEND_CHUNK'   , 1024 * 256);
  // TODO: Get this from the client.
  define('SERVER_TICKET_BYTES', 3);
  define('DOWNLOAD_DIR'       , 'files');
  define('UPLOAD_DIR'         , 'files/uploads');
  define('STUDY_LOG_FILE'     , 'study.log');
  define('STUDYP_LOG_FILE'    , 'studyp.log');
  define('FAIL_LOG_FILE'      , 'fail.log');
  define('DEBUG_LOG_FILE'     , 'debug.log');
  // TODO: Get rid of this configuration detail.
  define('GEOIP_DATABASE_FILE', '../GeoLiteCity.dat');

  function get_geoip_info() {
    if (file_exists(constant('GEOIP_DATABASE_FILE'))) {
      require_once('geoip/geoipcity.inc');
      require_once("geoip/geoipregionvars.php");
      $gi = geoip_open(constant('GEOIP_DATABASE_FILE'), GEOIP_STANDARD);
      $record = geoip_record_by_addr($gi, $_SERVER['REMOTE_ADDR']);
      $ret = $record->country_name . ', ' . $record->region;
      geoip_close($gi);
      return $ret;
    } else {
      return "(GeoIP database file missing)";
    }
  }

  function debuglog($msg) {
    $f = fopen(constant('DEBUG_LOG_FILE'), 'a');
    fwrite($f, $msg . "\n");
    fclose($f);
  }

  function studylog($tickets, $cmd, $fsize, $fname) {
    $f = fopen(constant('STUDY_LOG_FILE'), 'a');
    fwrite($f, gmdate('Y-m-d H:i:s') . "\t" . implode("\t", $tickets) . "\t" . $cmd . "\t" . sprintf('%9s', $fsize) . "\t". $fname . "\n");
    fclose($f);

    $f = fopen(constant('STUDYP_LOG_FILE'), 'a');
    $prefix = sprintf('%3s/%-3s', strtok(trim($_SERVER['REMOTE_ADDR']), '.') , strtok('.'));
    fwrite($f, gmdate('Y-m-d H:i:s') . "\t" . $prefix . "\t" . implode("\t", $tickets) . "\t" . $cmd . "\t" . sprintf('%9s', $fsize) . "\t". $fname . "\n");
    fclose($f);
  }

  function sane_string($s, $allowed_regex) {
    return strlen($s) < 200 && count(preg_grep($allowed_regex, array($s))) == 1;
  }

  function sane_path($f, $indir) {
    $rindir = realpath($indir);
    $rf     = realpath($f);
    return ($rindir && $rf && strncmp($rindir, $rf, strlen($rindir)) == 0);
  }

  function process(&$success) {
    $success = false;

    if (array_key_exists('cmd', $_GET) && $_GET['cmd'] == 'lnc') {
      $tickets = array('       (N/A)','       (N/A)',' (N/A)','');
      $cmd = 'lnc';
    } else {
      if (!array_key_exists('tickets' , $_POST) || !array_key_exists('cmd', $_POST) || !sane_string($_POST['tickets'], '/^[0-9a-f,]*$/'))
        return 'bad base parameters';

      $tickets = explode(',', $_POST['tickets']);
      if (count($tickets) != 4 || strlen($tickets[0]) == 0 || strlen($tickets[1]) == 0)
        return 'bad tickets';
      $cmd = $_POST['cmd'];
    }

    $server_ticket = strtolower(substr(sha1('stick ' . trim($_SERVER['REMOTE_ADDR'])), 0, constant('SERVER_TICKET_BYTES') * 2));
    if ($tickets[2] == '')
      $tickets[2] = $server_ticket;
    if ($tickets[3] == '')
      $tickets[3] = $server_ticket;
    if ($tickets[3] != $server_ticket) {
      return 'server ticket mismatch';
    }

    if ($cmd == 'lnc') {
      require_once('templates.php');
      output_jnlpfile(array());
      $sent_ver = array_key_exists('ver', $_GET) ? $_GET['ver'] : '(no ver)';
      studylog($tickets, $cmd, '(N/A)', $sent_ver . ';' . $_SERVER['HTTP_USER_AGENT']);
      $success = true;
      return 'launch ok';
    }

    $updir = constant('UPLOAD_DIR') . DIRECTORY_SEPARATOR . $tickets[1];
    if        ($cmd == 'gsi') {
      # TODO: Move the GeoIP info elsewhere.

      header('X-StudyCaster-ServerTicket: ' . $server_ticket);
      header('X-StudyCaster-ServerTime: ' . time());
      header('X-StudyCaster-OK: gsi');

      studylog($tickets, $cmd, '(N/A)', get_geoip_info());
      $success = true;
      return 'get server info ok';
    } else if ($cmd == 'upc') {
      if (!array_key_exists('file', $_POST))
        return 'no filename specified';
      if (!sane_string($_POST['file'], '/^[0-9a-zA-Z_.]+$/'))
        return 'insane filename specified';
      $fullpath = $updir . DIRECTORY_SEPARATOR . $_POST['file'];
      if (!file_exists($updir) && !mkdir($updir))
        return 'failed to create client upload directory';
      if (file_exists($fullpath)) {
        $suffix = 1;
        do {
          $moveto = $fullpath . '.' . $suffix;
          $suffix++;
        } while (file_exists($moveto));
        if (!rename($fullpath, $moveto))
          return 'failed to rename existing file with same name';
      }
      if (($cfile = fopen($fullpath, 'xb')) == false)
        return 'failed to create file';
      if (!fclose($cfile))
        return 'failed to close newly created file';
      header('X-StudyCaster-OK: upc');
      studylog($tickets, $cmd, 0, basename($fullpath));
      $success = true;
      return 'create ok';
    } else if ($cmd == 'upa') {
      if (!array_key_exists('file', $_FILES))
        return 'no uploaded file found';
      if (!sane_string($_FILES['file']['name'], '/^[0-9a-zA-Z_.]+$/'))
        return 'insane filename specified';
      if ($_FILES['file']['error'] != 0)
        return 'nonzero internal error code (' . $_FILES['file']['error'] . ')';
      if ($_FILES['file']['size'] > constant('MAX_APPEND_CHUNK'))
        return 'append source too large';
      $fullpath = $updir . DIRECTORY_SEPARATOR . $_FILES['file']['name'];
      if (!file_exists($fullpath))
        return 'append target does not exist';
      if (($oldSize = filesize($fullpath)) < 0)
        return 'failed to get size of append target';
      if ($oldSize + $_FILES['file']['size'] > constant('MAX_FILE_SIZE'))
        return 'concatenated file too large';
      if (($data = file_get_contents($_FILES['file']['tmp_name'])) == false)
        return 'failed to read append source';
      if (($atarget = fopen($fullpath, 'ab')) == false)
        return 'failed to open append target ' . $fullpath;
      if ($_FILES['file']['size'] != strlen($data))
        return 'inconsistent length after reading file';
      $fwr = fwrite($atarget, $data, $_FILES['file']['size']);
      if ($fwr == false)
        return 'write failed';
      if ($fwr != $_FILES['file']['size'])
        return 'unexpected short write';
      if (!fclose($atarget))
        return 'failed to close append target';
      header('X-StudyCaster-OK: upa');
      studylog($tickets, $cmd, $_FILES['file']['size'], basename($fullpath));
      $success = true;
      return 'append ok';
    } else if ($cmd == 'dnl') {
      if (!array_key_exists('file', $_POST))
        return 'no filename specified';
      $fullpath = constant('DOWNLOAD_DIR') . DIRECTORY_SEPARATOR . $_POST['file'];
      if (!sane_path($fullpath, constant('DOWNLOAD_DIR'))) {
        header('HTTP/1.0 404 Not Found');
        studylog($tickets, $cmd, 'not found', $_POST['file']);
        $success = true;
        return 'invalid path or file does not exist';
      }
      header('Content-Type: application/octet-stream');
      header('Content-Disposition: attachment; filename="' . basename($fullname) . '"');
      $fsize = filesize($fullpath);
      header('Content-Length: ' . $fsize);
      header('X-StudyCaster-OK: dnl');
      readfile($fullpath);
      studylog($tickets, $cmd, $fsize, $_POST['file']);
      $success = true;
      return 'download ok';
    } else {
      return 'unknown command';
    }
  }

  function main() {
    session_cache_limiter('nocache');

    if (empty($_POST) && empty($_GET) && empty($_FILES)) {
      require_once('templates.php');
      output_launch();
      return;
    }
    if (array_key_exists('cmd', $_GET) && $_GET['cmd'] == 'inf') {
      phpinfo();
      return;
    }

    $success = false;
    $message = process($success);
    // debuglog('Result was: ' . $message);
    if (!$success) {
      header('HTTP/1.0 400 Bad Request');

      $flog = fopen(constant('FAIL_LOG_FILE'), 'a');
      fwrite($flog, "================== Unsuccessful request ==================\n");
      fwrite($flog, 'TIME = ' . date('Y-m-d H:i:s') . "\n");
      fwrite($flog, 'CAUSE = ' . $message . "\n");
      foreach ($_SERVER as $i => $value)
        fwrite($flog, 'SERVER,' . $i . ' = ' . $_SERVER[$i] . "\n");
      foreach ($_POST as $i => $value)
        fwrite($flog, 'POST,' . $i . ' = ' . $_POST[$i] . "\n");
      foreach ($_GET as $i => $value)
        fwrite($flog, 'GET,' . $i . ' = ' . $_GET[$i] . "\n");
      foreach ($_FILES as $i => $value)
        foreach ($_FILES[$i] as $j => $value)
          fwrite($flog, 'FILES,' . $i . ',' . $j . ' = ' . $_FILES[$i][$j] . "\n");
      fwrite($flog, "==========================================================\n");
      fclose($flog);
    }
  }

  main();
?>
