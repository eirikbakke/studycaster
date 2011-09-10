function validateConnectionURL(evt) {
  "use strict";
  $.post("admin", {}, function () {
    alert("The call returned.");
  }, "json")
}

$(document).ready(function () {
  "use strict";
  $("#dbSetupSubmit").click(validateConnectionURL);
});
