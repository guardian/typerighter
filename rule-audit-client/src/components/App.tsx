import { hot } from "react-hot-loader/root";
import * as React from "react";
import { Provider } from "react-redux";

import store from "redux/store";
import CapiFeed from "./capiFeed/CapiFeed";
import Document from "./document/Document";

const App = () => (
  <Provider store={store}>
    <div className="row">
      <div className="col-4">
        <CapiFeed />
      </div>
      <div className="col-8">
        <Document />
      </div>
    </div>
  </Provider>
);

export default hot(App);
