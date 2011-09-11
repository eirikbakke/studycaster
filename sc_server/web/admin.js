/* JSLint directives:  */
/*jslint devel: true, browser: true, indent: 2 */
/*global jQuery, $ */

function unused() {
  "use strict";
}

function submitDBsetup() {
  "use strict";
  if (!$("#connectionURLfield").val()) {
    alert("Please enter a JDBC connection URL.");
    return;
  }
  if ($("#createRadio").attr("checked")) {
    if (!$("#newPasswordField").val()) {
      alert("Please select a new admin password.");
      return;
    }
    if (!confirm("This may erase existing data in the database. Continue?")) {
      return;
    }
  }
  function setInProgress(inProgress) {
    if (inProgress) {
      $("#progressWheel").show();
      $("#dbSetupSubmit").attr("disabled", "disabled");
    } else {
      $("#progressWheel").hide();
      $("#dbSetupSubmit").removeAttr("disabled");
    }
  }
  setInProgress(true);
  $.post("admin", $("#dbSetupForm").serializeArray(),
    function (data, textStatus, jqXHR) {
      unused(textStatus, jqXHR);
      setInProgress(false);
      alert("Result: " + data);
    })
    .error(function (jqXHR, textStatus, errorThrown) {
      unused(textStatus, jqXHR);
      setInProgress(false);
      alert("Unexpected error: " + errorThrown);
    });
}

$(document).ready(function () {
  "use strict";
  $("#dbSetupSubmit").click(submitDBsetup);
  $("input[name='dbAction']").change(function () {
    if ($("#createRadio").attr("checked")) {
      $("#newPasswordField").removeAttr("disabled");
    } else {
      $("#newPasswordField").attr("disabled", "disabled");
    }
  });
});
