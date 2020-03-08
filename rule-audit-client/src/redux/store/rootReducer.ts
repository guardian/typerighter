import { combineReducers } from "redux";

import { reducer as capiReducer } from "../modules/capiContent";

export default combineReducers({
  capi: capiReducer
});
