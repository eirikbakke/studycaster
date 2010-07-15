<?php
  define("MAX_FILE_SIZE", 5000000);
  define("DEBUG_LOG_FILE", "debug.log");
  define("STUDY_LOG_FILE", "study.log");
  define("FAIL_LOG_FILE" , "fail.log");

  function debuglog($msg) {
    $f = fopen(constant("DEBUG_LOG_FILE"), "a");
    fwrite($f, $msg . "\n");
    fclose($f);
  }

  function studylog($tickets, $msg) {
    $f = fopen(constant("STUDY_LOG_FILE"), "a");
    $ipstart = strtok(trim($_SERVER['REMOTE_ADDR']), ".") . "/" . strtok(".");
    fwrite($f, date("Y-m-d H:i:s") . "\t" . $ipstart . "\t" . $tickets . "\t" . $msg . "\n");
    fclose($f);
  }

  function sane_string($s, $allowed_chars) {
    return strlen($s) < 200 && (ereg_replace($allowed_chars, "", $s) == $s);
  }

  function process() {
    if (!array_key_exists("ct" , $_POST) || !array_key_exists("cmd", $_POST) || !sane_string($_POST["ct"], "[^0-9a-fN\t]"))
      return true;

    $cta = explode("\t", $_POST["ct"]);
    if (count($cta) != 4 || strlen($cta[0]) == 0 || strlen($cta[1]) == 0)
      return true;

    $server_ticket = strtolower(substr(sha1("stick " . trim($_SERVER["REMOTE_ADDR"])), 0, strlen($cta[0])));
    if ($cta[2] == "N")
      $cta[2] = $server_ticket;
    if ($cta[3] == "N")
      $cta[3] = $server_ticket;
    if ($cta[3] != $server_ticket) {
      debuglog("server ticket mismatch");
      return true;
    }
    $ct = implode("\t", $cta);

    $cmd = $_POST["cmd"];
    if        ($cmd == "gsi") {
      header("X-StudyCaster-ServerTicket: " . $server_ticket);
      header("X-StudyCaster-ServerTime: " . time());
      studylog($ct, "gsi");
      return false;
    } else if ($cmd == "up" && array_key_exists("file", $_FILES) && sane_filename($_FILES["file"]["name"]) &&
               $_FILES["file"]["size"] < constant("MAX_FILE_SIZE"))
    {

    } else if ($cmd == "dn") {
    }
    return true;
  }

  function main() {
    if (process() && !(empty($_POST) && empty($_GET))) {
      header("HTTP/1.0 400 Bad Request");

      $flog = fopen(constant("FAIL_LOG_FILE"), "a");
      fwrite($flog, "================== Unsuccessful request ==================\n");
      fwrite($flog, "TIME = " . date("Y-m-d H:i:s") . "\n");
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
