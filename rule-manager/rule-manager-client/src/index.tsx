import * as React from "react";
import * as ReactDOM from "react-dom";

import App from "./components/App";

let rootElem: HTMLElement | null;

rootElem = document.getElementById("rule-manager-app");

ReactDOM.render(<App />, rootElem);
