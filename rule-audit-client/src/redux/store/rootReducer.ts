import { combineReducers } from "redux";

import { reducer as capiReducer } from "../modules/capiContent";
import { reducer as uiReducer } from "../modules/ui";

export default combineReducers({
  capi: capiReducer,
  ui: uiReducer,
});
