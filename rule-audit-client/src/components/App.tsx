import { hot } from "react-hot-loader/root";
import * as React from "react";
import { Provider } from "react-redux";

import store from "redux/store";
import CapiFeed from "./capiFeed/CapiFeed";
import Document from "./document/Document";

const App = () => (
  <Provider store={store}>
    <style>
      {
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
