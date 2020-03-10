import { createAction, createReducer } from "typesafe-actions";

import AppTypes from "AppTypes";

export type UIState = {
  readonly selectedArticle: string | undefined;
};

const initialState: UIState = {
  selectedArticle: undefined
};

const selectArticle = createAction("SELECT_ARTICLE")<string>();

const selectSelectedArticle = (state: AppTypes.RootState) => state.ui.selectedArticle;

export const reducer = createReducer(initialState).handleAction(
  selectArticle,
  (state, action) => ({
    ...state,
    selectedArticle: action.payload
  })
);

export const selectors = { selectSelectedArticle };
export const actions = { selectArticle };
