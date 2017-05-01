Notifications.init($("#notifications"));

Popup.init($("#popup"), $("#popup-table"));

Dependencies.init($("#search-btn"), $("#query"), $("#dependencies"));

ProjectLocation.init($("#cwd"), $("#name"));

["#create-project-btn", "#copy-project-btn", "#flow-create-btn", "#flow-copy-btn"].forEach(id => $(id)[0].disabled = false);

$("#flow-copy-btn")[0].style.width = $("#flow-create-btn")[0].offsetWidth + "px";

function setFlowCreate() {
  $("#flow-create").show();
  $("#flow-copy").hide();

  let createBtn = $("#flow-create-btn");
  let copyBtn = $("#flow-copy-btn");
  createBtn.blur();
  createBtn[0].disabled = true;
  copyBtn[0].disabled = false;

  ProjectLocation.updateName($("#name").val());
}

function createProject() {
  let button = $("#create-project-btn")[0];
  let buttonText = button.innerHTML;
  button.innerHTML = "...";
  button.blur();
  button.disabled = true;
  ajax("/project/new", {
    name: $("#name").val(),
    pack: $("#package").val(),
    dependencies: Dependencies.serialize(),
    flags: getFlags()
  }, "post").done(() => {
    Notifications.show("Project created.");
  }).always(() => {
    button.innerHTML = buttonText;
    button.disabled = false;
  });
}

function setFlowCopy() {
  $("#flow-copy").show();
  $("#flow-create").hide();

  let createBtn = $("#flow-create-btn");
  let copyBtn = $("#flow-copy-btn");
  copyBtn.blur();
  copyBtn[0].disabled = true;
  createBtn[0].disabled = false;

  Examples.unselectExample();

  Examples.fetchExamples();
}

function copyProject() {
  let name = $("#selected-example").html();
  let button = $("#copy-project-btn")[0];
  let buttonText = button.innerHTML;
  button.innerHTML = "...";
  button.blur();
  button.disabled = true;
  ajax("/project/copy", {name: name}, "post").done(() => {
    Notifications.show("Project copied.");
  }).always(() => {
    button.innerHTML = buttonText;
    button.disabled = false;
  });
}
