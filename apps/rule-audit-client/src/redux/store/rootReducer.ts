import { combineReducers } from "redux";

import { reducer as capi } from "../modules/capiContent";
import { reducer as ui } from "../modules/ui";
import { reducer as searchMatches } from "../modules/searchMatches";

export default combineReducers({
  capi,
  ui,
  searchMatches
});
