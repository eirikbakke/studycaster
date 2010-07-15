<?php
  define("MAX_FILE_SIZE" , 5000000);
  define("DOWNLOAD_DIR"  , "downloads");
  define("UPLOAD_DIR"    , "uploads");
  define("STUDY_LOG_FILE", "study.log");
  define("FAIL_LOG_FILE" , "fail.log");
  define("DEBUG_LOG_FILE" , "debug.log");

  function debuglog($msg) {
    $f = fopen(constant("DEBUG_LOG_FILE"), "a");
    fwrite($f, $msg . "\n");
    fclose($f);
  }

  function studylog($tickets, $msg) {
    $f = fopen(constant("STUDY_LOG_FILE"), "a");
    $ipstart = strtok(trim($_SERVER['REMOTE_ADDR']), ".") . "/" . strtok(".");
    fwrite($f, gmdate("Y-m-d H:i:s") . "\t" . $ipstart . "\t" . $tickets . "\t" . $msg . "\n");
    fclose($f);
  }

  function sane_string($s, $allowed_regex) {
    return strlen($s) < 200 && count(preg_grep($allowed_regex, array($s))) == 1;
  }

  function process() {
    if (!array_key_exists("ct" , $_POST) || !array_key_exists("cmd", $_POST) || !sane_string($_POST["ct"], "/^[0-9a-fN\t]*$/"))
      return "bad base parameters";

    $ata = explode("\t", $_POST["ct"]);
    if (count($ata) != 4 || strlen($ata[0]) == 0 || strlen($ata[1]) == 0)
      return "bad tickets";

    $server_ticket = strtolower(substr(sha1("stick " . trim($_SERVER["REMOTE_ADDR"])), 0, strlen($ata[0])));
    if ($ata[2] == "N")
      $ata[2] = $server_ticket;
    if ($ata[3] == "N")
      $ata[3] = $server_ticket;
    if ($ata[3] != $server_ticket) {
      return "server ticket mismatch";
    }
    $at = implode("\t", $ata);

    $cmd = $_POST["cmd"];
    if        ($cmd == "gsi") {
      header("X-StudyCaster-ServerTicket: " . $server_ticket);
      header("X-StudyCaster-ServerTime: " . time());
      studylog($at, "gsi\t-");
      return "SUCCESS";
    } else if ($cmd == "upl") {
      if (!array_key_exists("file", $_FILES)) {
        return "no uploaded file found";
      } else if ($_FILES["f"]["error"] != 0) {
        return "nonzero internal error code";
      } else if (!sane_string($_FILES["file"]["name"], "/^[0-9a-zA-Z_.]+$/")) {
        return "insane filename";
        return true;
      } else if ($_FILES["file"]["size"] > constant("MAX_FILE_SIZE")) {
        return "file too large";
      }
      $fullpath = constant("UPLOAD_DIR") . "/" . $_FILES["file"]["name"];
      if (file_exists($fullpath))
        return "file already exists";
      if (move_uploaded_file($_FILES["file"]["tmp_name"], $fullpath) == false)
        return "move failed";
      studylog($at, "upl\t" . $_FILES["file"]["size"] . "," . $_FILES["file"]["name"]);
      header("X-StudyCaster-UploadOK: true");
      return "SUCCESS";
    } else if ($cmd == "dnl") {
      return "unimplemented";
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
