import { createReducer } from "typesafe-actions";
import { doSetSearchMatchesLimit, doSearchMatchesStart, doAppendSearchMatchesArticleIds, doSearchMatchesEnd } from "./actions";

const initialState = {
  searchMatchesLimit: 10,
  isSearchMatchesInProgress: false,
  searchMatchesArticleIds: [] as string[]
};

export type SearchMatchesState = typeof initialState

const reducer = createReducer(initialState)
  .handleAction(doSetSearchMatchesLimit, (state, action) => ({
    ...state,
    searchMatchesLimit: action.payload,
  }))
  .handleAction(doSearchMatchesStart, (state) => ({
    ...state,
    isSearchMatchesInProgress: true,
    searchMatchesArticleIds: []
  }))
  .handleAction(doAppendSearchMatchesArticleIds, (state, action) => ({
    ...state,
    searchMatchesArticleIds: state.searchMatchesArticleIds.concat(action.payload),
  }))
  .handleAction(doSearchMatchesEnd, (state) => ({
    ...state,
    isSearchMatchesInProgress: false,
  }));

export default reducer;
