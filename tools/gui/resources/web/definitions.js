function ajax(url, data, method) {
  let f = method == "post" ? $.post : $.get;
  return f("/api" + url, data).fail(e =>
      Notifications.showFail(e)
  );
}

function getFlags() {
  return "readme/" + $("#readme-flag")[0].checked
      + " dotty/" + $("#dotty-flag")[0].checked
      + " uberJar/" + $("#uberJar-flag")[0].checked
      + " wartremover/" + $("#wartremover-flag")[0].checked;
}

let ProjectLocation = {
  _base: "",

  init: function (container, nameInput) {
    this._container = container;
    nameInput.keyup(function () {
      ProjectLocation.updateName(this.value);
    });
    ajax("/cwd").done(path => {
      ProjectLocation._container.append(path[0]);
      ProjectLocation._base = ProjectLocation._container.html();
    });
  },

  updateName: function (x) {
    this._container.html(this._base + (x ? "/" + x : ""));
  }
};

let Popup = {
  init: function (container, table) {
    this._container = container;
    this._table = table;
    document.body.onkeydown = function (event) {
      if (event.keyCode == 27)
        Popup.hide();
    };
  },

  show: function (contents) {
    this._container.show();
    this._table.html(contents);
  },

  hide: function () {
    this._container.hide();
    this._table.html("");
  }
};

let Notifications = {
  init: function (container) {
    this._container = container;
  },

  show: function (text, title) {
    let now = new Date();
    this._container.html(this._container.html() + now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds() +
        " <b>" + (title || "") + "</b><br>" + text + "<br><br>");
    this._container.animate({scrollTop: this._container[0].scrollHeight}, {duration: 500, queue: false});
  },

  showFail: function (e) {
    let head = e.status == 0 ? "Error" : e.status + " " + e.statusText;
    let body = e.status == 0 ? "No response from UI server." : e.responseText;
    this.show(body, head);
  }
};

let Dependencies = {
  _list: [],

  init: function (searchBtn, queryInput, dependenciesNode) {
    this._queryInput = queryInput;
    queryInput.keyup(function handleSearchInput(event) {
      if (event.keyCode == 13)
        Dependencies.search();
    });
    this._dependenciesNode = dependenciesNode;
  },

  serialize: function () {
    return this._list.length == 0 ? "" :
        this._list.map(l => l.group + "/" + l.artifact + "/" + l.version).reduce((a, b) => a + " " + b);
  },

  search: function () {
    let query = this._queryInput.val();
    if (query) {
      ajax("/dependency", {query: query}).done(data => {
        let entries = data.response.docs.map(x => ({
          group: x.g,
          artifact: x.a
        }));
        Popup.show(Dependencies._makeRowsFrom(entries, Dependencies.selectDependency));
      });
      document.activeElement.blur();
    }
  },

  selectDependency: function (selected) {
    ajax("/dependency/version", {group: selected.group, artifact: selected.artifact}).done(data => {
      let versions = data.response.docs.map(x => ({version: x.v}));
      Popup.show(Dependencies._makeRowsFrom(versions, function (version) {
        Dependencies.selectDependencyVersion(selected.group, selected.artifact, version.version);
      }));
    });
  },

  selectDependencyVersion: function (group, artifact, version) {
    let dependency = {group: group, artifact: artifact, version: version};
    this._list.push(dependency);
    let scalaName = artifact.match(/^(.+)_\d+\.\d+$/);
    let name = scalaName && scalaName[1] ? scalaName[1] : artifact;
    let depDiv = $("<div class='entry removable'>" + name + " " + version + "</div>");
    depDiv.click(function () {
      Dependencies.removeDependency(dependency, depDiv);
    });
    this._dependenciesNode.append(depDiv);
    Popup.hide();
  },

  removeDependency: function (d, div) {
    this._list.splice(this._list.indexOf(d), 1);
    div.remove();
  },

  _makeRowsFrom: function (results, rowAction) {
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
};

let Examples = {
  fetchExamples: () => {
    let examplesContainer = $("#examples");
    examplesContainer.html("");
    ajax("/examples").done(data => {
      data.forEach(name => {
        var example = $("<div class='link-btn'>" + name + "</div>");
        example.click(() => Examples.selectExample(name));
        example.appendTo(examplesContainer);
      });
    });
  },

  selectExample: name => {
    $("#examples-title").hide();
    let selectedExample = $("#selected-example");
    selectedExample.html(name);
    selectedExample.show();
    ProjectLocation.updateName(name);
    $("#examples").hide();
    $("#example-browser").show();
    $("#copy-project-btn").show();

    Examples._fetchExampleFiles(name);
  },

  unselectExample: () => {
    $("#examples-title").show();
    $("#selected-example").hide();
    ProjectLocation.updateName("");
    $("#examples").show();
    $("#example-browser").hide();
    $("#copy-project-btn").hide();
    $("#code-browser").hide();
  },

  _fetchExampleFiles: name => {
    let fileBrowser = $("#file-browser");
    fileBrowser.html("");
    ajax("/example/files", {name: name}).done(data => {
      Examples._appendTree(data, fileBrowser, 0);
    });
  },

  selectedFileNode: null,

  _appendTree: function (node, parent, dirDepth) {
    let div = $("<div class='browser-node'>" + node.name + "</div>");
    div.appendTo(parent);
    if (dirDepth % 2 == 0)
      div.addClass("even-node");
    if (node.children) {
      node.children.forEach(x => Examples._appendTree(x, div, dirDepth + 1));
    } else {
      div.addClass("file-node");
      div.click(() => {
        if (Examples.selectedFileNode)
          Examples.selectedFileNode.removeClass("selected-node");
        Examples.selectedFileNode = div;
        Examples.selectedFileNode.addClass("selected-node");
        ajax("/example/file", {path: node.path}).done(data => {
          var codeBrowser = $("#code-browser");
          codeBrowser.show();
          codeBrowser.html(node.name.endsWith(".md") ? data : ("<pre><code>" + data + "</code></pre>"));
          let lang = node.name.substring(node.name.lastIndexOf(".") + 1);
          codeBrowser.find("code").addClass(lang);
          $("pre code").each((i, block) => hljs.highlightBlock(block));
        });
      });
    }
    if (node.name.toLowerCase() == "readme.md")
      div.click();
  }
};
