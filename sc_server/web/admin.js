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
  // TODO: Is there a way to not assign unused parameters to names?
  $.post("admin", $("#dbSetupForm").serializeArray(),
    function (data, textStatus, jqXHR) {
      unused(textStatus, jqXHR);
      alert("Result: " + data);
    })
    .error(function (jqXHR, textStatus, errorThrown) {
      unused(textStatus, jqXHR);
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
