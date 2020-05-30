import { createAction, createReducer } from "typesafe-actions";

import AppTypes from "AppTypes";

export type UIState = {
  readonly selectedArticle: string | undefined;
};

const initialState: UIState = {
  selectedArticle: undefined
};

const doSelectArticle = createAction("SELECT_ARTICLE")<string>();

const selectSelectedArticle = (state: AppTypes.RootState) => state.ui.selectedArticle;

export const reducer = createReducer(initialState).handleAction(
  doSelectArticle,
  (state, action) => ({
    ...state,
    selectedArticle: action.payload
  })
);

export const selectors = { selectSelectedArticle };
export const actions = { doSelectArticle };
