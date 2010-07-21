<?php
  define("MAX_FILE_SIZE"      , 50000000);
  // TODO: Get this from the client.
  define("SERVER_TICKET_BYTES", 3);
  define("DOWNLOAD_DIR"       , "downloads");
  define("UPLOAD_DIR"         , "uploads");
  define("STUDY_LOG_FILE"     , "study.log");
  define("STUDYP_LOG_FILE"    , "studyp.log");
  define("FAIL_LOG_FILE"      , "fail.log");
  define("DEBUG_LOG_FILE"     , "debug.log");

  function debuglog($msg) {
    $f = fopen(constant("DEBUG_LOG_FILE"), "a");
    fwrite($f, $msg . "\n");
    fclose($f);
  }

  function studylog($tickets, $cmd, $fsize, $fname) {
    $f = fopen(constant("STUDY_LOG_FILE"), "a");
    fwrite($f, gmdate("Y-m-d H:i:s") . "\t" . implode("\t", $tickets) . "\t" . $cmd . "\t" . sprintf("%9s", $fsize) . "\t". $fname . "\n");
    fclose($f);

    $f = fopen(constant("STUDYP_LOG_FILE"), "a");
    $prefix = sprintf("%3s/%-3s", strtok(trim($_SERVER['REMOTE_ADDR']), ".") , strtok("."));
    fwrite($f, gmdate("Y-m-d H:i:s") . "\t" . $prefix . "\t" . implode("\t", $tickets) . "\t" . $cmd . "\t" . sprintf("%9s", $fsize) . "\t". $fname . "\n");
    fclose($f);
  }

  function sane_string($s, $allowed_regex) {
    return strlen($s) < 200 && count(preg_grep($allowed_regex, array($s))) == 1;
  }

  function process() {
    if (!array_key_exists("ct" , $_POST) || !array_key_exists("cmd", $_POST) || !sane_string($_POST["ct"], "/^[0-9a-fN\t]*$/"))
      return "bad base parameters";

    $tickets = explode("\t", $_POST["ct"]);
    if (count($tickets) != 4 || strlen($tickets[0]) == 0 || strlen($tickets[1]) == 0)
      return "bad tickets";

    $server_ticket = strtolower(substr(sha1("stick " . trim($_SERVER["REMOTE_ADDR"])), 0, constant("SERVER_TICKET_BYTES") * 2));
    if ($tickets[2] == "N")
      $tickets[2] = $server_ticket;
    if ($tickets[3] == "N")
      $tickets[3] = $server_ticket;
    if ($tickets[3] != $server_ticket) {
      return "server ticket mismatch";
    }

    $cmd = $_POST["cmd"];
    if        ($cmd == "gsi") {
      header("X-StudyCaster-ServerTicket: " . $server_ticket);
      header("X-StudyCaster-ServerTime: " . time());
      studylog($tickets, $_POST["cmd"], "(N/A)", "(N/A)");
      return "SUCCESS";
    } else if ($cmd == "upl") {
      if (!array_key_exists("file", $_FILES)) {
        return "no uploaded file found";
      } else if ($_FILES["f"]["error"] != 0) {
        return "nonzero internal error code";
      } else if (!sane_string($_FILES["file"]["name"], "/^[0-9a-zA-Z_.]+$/")) {
        return "insane filename specified";
        return true;
      } else if ($_FILES["file"]["size"] > constant("MAX_FILE_SIZE")) {
        return "file too large";
      }
      $prefix = constant("UPLOAD_DIR") . "/" . $_FILES["file"]["name"];
      $fullpath = $prefix;
      $i = 0;
      while (file_exists($fullpath)) {
        $i++;
        $fullpath = $prefix . "_" . $i;
      }
      if (move_uploaded_file($_FILES["file"]["tmp_name"], $fullpath) == false)
        return "move failed";
      studylog($tickets, $_POST["cmd"], $_FILES["file"]["size"], basename($fullpath));
      header("X-StudyCaster-UploadOK: true");
      return "SUCCESS";
    } else if ($cmd == "dnl") {
      if (!array_key_exists("file", $_POST))
        return "no filename specified";
      if (!sane_string($_POST["file"], "/^[0-9a-zA-Z_.]+$/"))
        return "insane filename requested";
      $fullpath = constant("DOWNLOAD_DIR") . "/" . $_POST["file"];
      if (!file_exists($fullpath))
        return "file does not exist";
      header("Content-Type: application/octet-stream");
      header('Content-Disposition: attachment; filename="' . basename($fullname) . '"');
      $fsize = filesize($fullpath);
      header("Content-Length: " . $fsize);
      header("X-StudyCaster-DownloadOK: true");
      readfile($fullpath);
      studylog($tickets, $_POST["cmd"], $fsize, $_POST["file"]);
      return "SUCCESS";
    } else {
      return "unknown command";
    }
  }

  function main() {
    $result = process();
    if ($result != "SUCCESS" && !(empty($_POST) && empty($_GET))) {
      header("HTTP/1.0 400 Bad Request");

      $flog = fopen(constant("FAIL_LOG_FILE"), "a");
      fwrite($flog, "================== Unsuccessful request ==================\n");
      fwrite($flog, "TIME = " . date("Y-m-d H:i:s") . "\n");
      fwrite($flog, "CAUSE = " . $result . "\n");
      foreach ($_SERVER as $i => $value)
        fwrite($flog, "SERVER," . $i . " = " . $_SERVER[$i] . "\n");
      foreach ($_POST as $i => $value)
        fwrite($flog, "POST," . $i . " = " . $_POST[$i] . "\n");
      foreach ($_GET as $i => $value)
        fwrite($flog, "GET," . $i . " = " . $_GET[$i] . "\n");
      foreach ($_FILES as $i => $value)
        foreach ($_FILES[$i] as $j => $value)
          fwrite($flog, "FILES," . $i . "," . $j . " = " . $_FILES[$i][$j] . "\n");
      fwrite($flog, "==========================================================\n");
      fclose($flog);
    }
  }

  main();

/*
  $fullname = "downloads/exflat.xls";
  header("Content-type: application/octet-stream");
  header('Content-Disposition: attachment; filename="' . basename($fullname) . '"');
  header("Content-Length: ". filesize($fullname));
  readfile($fullname);
*/
?>
