/* Lazy-loads the WASM sample into the home-page demo stage on click,
   so the ~14 MB WebAssembly bundle is only fetched when requested. */
(function () {
  function init() {
    var stage = document.getElementById("demo-stage");
    var launch = document.getElementById("demo-launch");
    if (!stage || !launch) {
      return;
    }

    launch.addEventListener("click", function () {
      stage.classList.add("is-loading");

      var iframe = document.createElement("iframe");
      iframe.className = "demo-frame";
      iframe.src = launch.getAttribute("data-src");
      iframe.title = "DrawBox interactive demo";
      iframe.setAttribute("allow", "fullscreen");
      iframe.addEventListener("load", function () {
        stage.classList.remove("is-loading");
        stage.classList.add("is-live");
      });

      launch.remove();
      stage.appendChild(iframe);
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();