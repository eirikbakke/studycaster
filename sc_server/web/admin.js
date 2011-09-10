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
  $.post("admin", $("#dbSetupForm").serializeArray(), function () {
    alert("The call returned.");
  }, "json");
}

$(document).ready(function () {
  "use strict";
  $("#dbSetupSubmit").click(submitDBsetup);
  $("input[name='dbActionChoice']").change(function () {
    if ($("#createRadio").attr("checked")) {
      $("#newPasswordField").removeAttr("disabled");
    } else {
      $("#newPasswordField").attr("disabled", "disabled");
    }
  });
});
