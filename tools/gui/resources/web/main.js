Notification.requestPermission();

$.get("/api/cwd").done(cwd => {
  $("#cwd").append(cwd[0]);
}).fail(e => notifyFail(e));

document.body.onkeydown = function (event) {
  if (event.keyCode == 27)
    hidePopup();
};

$("#create-project")[0].disabled = false;

let dependencies = [];

function getFlags() {
  return "readme/" + $("#readme-flag")[0].checked
      + " dotty/" + $("#dotty-flag")[0].checked
      + " uberJar/" + $("#uberJar-flag")[0].checked
      + " wartremover/" + $("#wartremover-flag")[0].checked;
}

function createProject() {
  let button = $("#create-project")[0];
  let buttonText = button.innerHTML;
  button.innerHTML = "...";
  button.blur();
  button.disabled = true;
  $.post("/api/project", {
    name: $("#name").val(),
    pack: $("#package").val(),
    dependencies: dependencies.length == 0 ? "" :
        dependencies.map(l => l.group + "/" + l.artifact + "/" + l.version).reduce((a, b) => a + " " + b),
    flags: getFlags()
  }).done(() => {
    notify("Done.");
  }).fail(e =>
    notifyFail(e)
  ).always(() => {
    button.innerHTML = buttonText;
    button.disabled = false;
  });
}

function handleSearchInput(event) {
  if (event.keyCode == 13)
    search();
}
function search() {
  let query = $("#query").val();
  if (query) {
    $.get("/api/dependency", { query: query }).done(data => {
      let entries = data.response.docs.map(x => ({
        group: x.g,
        artifact: x.a
      }));
      showPopup(makeRowsFrom(entries, selectDependency));
    }).fail(e => notifyFail(e));
    document.activeElement.blur();
  }
}
function selectDependency(selected) {
  $.get("/api/dependency/version", { group: selected.group, artifact: selected.artifact }).done(data => {
    let versions = data.response.docs.map(x => ({ version: x.v }));
    $("#popup-table").html(makeRowsFrom(versions, function (version) {
      selectDependencyVersion(selected.group, selected.artifact, version.version);
    }));
  }).fail(e => notifyFail(e));
}
function selectDependencyVersion(group, artifact, version) {
  let dependency = { group: group, artifact: artifact, version: version };
  dependencies.push(dependency);
  let scalaName = artifact.match(/^(.+)_\d+\.\d+$/);
  let name = scalaName && scalaName[1] ? scalaName[1] : artifact;
  var depDiv = $("<div class='entry removable'>" + name + " " + version + "</div>");
  depDiv.click(function () {
    removeDependency(dependency, depDiv);
  });
  $("#dependencies").append(depDiv);
  hidePopup();
}
function removeDependency(d, div) {
  dependencies.splice(dependencies.indexOf(d), 1);
  div.remove();
}

function showPopup(contents) {
  $("#popup").show();
  $("#popup-table").html(contents);
}
function hidePopup() {
  $("#popup").hide();
  $("#popup-table").html("");
}

function makeRowsFrom(results, rowAction) {
  if (results.length == 0) {
    return [];
  } else {
    let rows = [];
    let row = $("<tr></tr>");
    rows.push(row);
    let fields = [];
    for (let field in results[0])
      if (results[0].hasOwnProperty(field)) {
        fields.push(field);
        $("<td>" + field + "</td>").appendTo(row);
      }
    results.forEach(result => {
      let row = $("<tr></tr>");
      row.click(function () {
        rowAction(result);
      });
      rows.push(row);
      fields.forEach(field => $("<td>" + result[field] + "</td>").appendTo(row));
    });
    return rows;
  }
}

function notify(text, title) {
  new Notification(title || "", { body: text });
}

function notifyFail(e) {
  let head = e.status == 0 ? "Error" : e.status + " " + e.statusText;
  let body = e.status == 0 ? "No response from UI server." : e.responseText;
  notify(body, head);
}
