import { hot } from "react-hot-loader/root";
import * as React from "react";
import { Provider } from "react-redux";

import { createStore } from "redux/store";
import CapiFeed from "./capiFeed/CapiFeed";
import Document from "./document/Document";

const store = createStore();

const App = () => (
  <Provider store={store}>
    <style>
      {
      /**
       * Necessary to apply themed colors to @guardian/threads components (e.g. chips).
       * This approach is hopefully temporary – see https://github.com/guardian/threads/issues/4
       */
      `:root {
        --color-primary: #6c757d;
        --color-danger: #dc3545;
        --color-secondary: white;
        --color-selected: #e9ecef;
        --color-selected-text: #495057;
      }
      `}
    </style>
    <div className="row">
      <div className="col-3">
        <h5>Search CAPI Content</h5>
        <CapiFeed />
      </div>
      <div className="col-9">
        <h5>Current document</h5>
        <Document />
      </div>
    </div>
    <div id="overlays" />
  </Provider>
);

export default hot(App);
