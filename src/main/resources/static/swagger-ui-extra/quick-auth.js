(function() {
  var users = {
    admin: "admin123!",
    ops: "ops123!",
    runner: "runner123!",
    reader: "reader123!"
  };

  function insertQuickAuth() {
    if (document.getElementById("swagger-quick-auth")) {
      return true;
    }
    var topbar = document.querySelector(".swagger-ui .topbar .topbar-wrapper");
    if (!topbar) {
      return false;
    }
    var template = document.getElementById("swagger-quick-auth-template");
    if (!template || !template.content) {
      return false;
    }
    var wrapper = template.content.querySelector("#swagger-quick-auth");
    if (!wrapper) {
      return false;
    }
    var clone = wrapper.cloneNode(true);
    var logo = topbar.querySelector("a");
    if (logo && logo.nextSibling) {
      topbar.insertBefore(clone, logo.nextSibling);
    } else {
      topbar.appendChild(clone);
    }
    return true;
  }

  function setStatus(message, isError) {
    var el = document.getElementById("swagger-quick-auth-status");
    if (!el) {
      return;
    }
    el.textContent = message || "";
    el.classList.remove("is-error", "is-success");
    if (message) {
      el.classList.add(isError ? "is-error" : "is-success");
    }
  }

  function findSecurityScheme() {
    if (!window.ui || !window.ui.getSystem) {
      return null;
    }
    var spec = window.ui.getSystem().specSelectors.specJson();
    if (!spec) {
      return null;
    }
    var schemes = spec.getIn(["components", "securitySchemes"]);
    var definitions = spec.get("securityDefinitions");
    return (schemes && schemes.get("bearerAuth")) || (definitions && definitions.get("bearerAuth"));
  }

  function authorizeWhenReady(token, user, remaining) {
    if (!window.ui || !window.ui.preauthorizeApiKey) {
      setStatus("Swagger UI not ready.", true);
      return;
    }
    var scheme = findSecurityScheme();
    if (!scheme) {
      if (remaining <= 0) {
        setStatus("Swagger UI not ready.", true);
        return;
      }
      setTimeout(function() {
        authorizeWhenReady(token, user, remaining - 1);
      }, 100);
      return;
    }
    window.ui.preauthorizeApiKey("bearerAuth", token);
    setStatus("Authorized as " + user + ".", false);
  }

  async function login(user) {
    var password = users[user];
    if (!password) {
      setStatus("Select a user.", true);
      return;
    }
    setStatus("Signing in...", false);
    try {
      var response = await fetch("/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: user, password: password })
      });
      if (!response.ok) {
        setStatus("Login failed (" + response.status + ").", true);
        return;
      }
      var json = await response.json();
      var token = json && json.accessToken;
      if (!token) {
        setStatus("Login response missing token.", true);
        return;
      }
      authorizeWhenReady(token, user, 50);
    } catch (e) {
      setStatus("Login error.", true);
    }
  }

  window.addEventListener("load", function() {
    var attempts = 0;
    var timer = setInterval(function() {
      attempts += 1;
      if (insertQuickAuth() || attempts > 50) {
        clearInterval(timer);
        var button = document.getElementById("swagger-quick-auth-btn");
        var select = document.getElementById("swagger-quick-auth-user");
        if (!button || !select) {
          return;
        }
        button.addEventListener("click", function() {
          login(select.value);
        });
      }
    }, 100);
  });
})();
