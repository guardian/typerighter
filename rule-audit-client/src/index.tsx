import * as React from "react";
import * as ReactDOM from "react-dom";

import App from "./components/App";

let rootElem: HTMLElement | null;

if (ENV === 'prod') {
  rootElem = document.getElementById("rule-audit-app");
} else {
  rootElem = document.createElement("div");
  document.body.append(rootElem);
}

ReactDOM.render(<App />, rootElem);
