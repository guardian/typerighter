import { createAction, createReducer } from "typesafe-actions";

import AppTypes from "AppTypes";

export type SearchMode = "MATCHES" | "ARTICLES";

type UIState = {
  readonly selectedArticle: string | undefined;
  readonly displayAllArticles: boolean;
  readonly searchMode: SearchMode;
};

const initialState: UIState = {
  selectedArticle: undefined,
  displayAllArticles: true,
  searchMode: "ARTICLES",
};

const doSelectArticle = createAction("SELECT_ARTICLE")<string>();

const doToggleDisplayAllArticles = createAction(
  "TOGGLE_DISPLAY_ALL_ARTICLES"
)();

const doSetSearchMode = createAction("SET_SEARCH_MODE")<SearchMode>();

const selectSelectedArticle = (state: AppTypes.RootState) =>
  state.ui.selectedArticle;

const selectDisplayAllArticles = (state: AppTypes.RootState) =>
  state.ui.displayAllArticles;

const selectSearchMode = (state: AppTypes.RootState) => state.ui.searchMode;

export const reducer = createReducer(initialState)
  .handleAction(doSelectArticle, (state, action) => ({
    ...state,
    selectedArticle: action.payload,
  }))
  .handleAction(doToggleDisplayAllArticles, (state, action) => ({
    ...state,
    displayAllArticles: !state.displayAllArticles,
  }))
  .handleAction(doSetSearchMode, (state, action) => ({
    ...state,
    searchMode: action.payload
  }));

export const selectors = {
  selectSelectedArticle,
  selectDisplayAllArticles,
  selectSearchMode,
};
export const actions = {
  doSelectArticle,
  doToggleDisplayAllArticles,
  doSetSearchMode,
};
