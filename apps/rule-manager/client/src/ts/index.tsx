import * as React from "react";
import * as ReactDOM from "react-dom";
import {Page} from "./components/layout/Page";
import "../css/reset.css";
import "../css/typography.css";

// For development mode with Vite
import 'vite/modulepreload-polyfill';

let rootElem: HTMLElement | null;

rootElem = document.getElementById("rule-manager-app");

ReactDOM.render(<Page />, rootElem);
