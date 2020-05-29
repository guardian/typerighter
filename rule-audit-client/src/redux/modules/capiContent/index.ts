import { Dispatch } from "redux";
import { createAsyncResourceBundle } from "redux-bundle-creator";

import AppTypes from "AppTypes";
import { CapiContent, fetchCapiSearch } from "services/Capi";

const bundle = createAsyncResourceBundle<CapiContent, {}, "capi">("capi");

const fetchSearch = (
  query: string,
  tags: string[],
  sections: string[]
) => async (dispatch: Dispatch): Promise<void> => {
  dispatch(bundle.actions.fetchStart());
  try {
    const content = await fetchCapiSearch(query, tags, sections);
    dispatch(
      bundle.actions.fetchSuccess(content.results, {
        pagination: {
          pageSize: content.pageSize,
          totalPages: content.pages,
          currentPage: content.currentPage
        }
      })
    );
  } catch (e) {
    dispatch(bundle.actions.fetchError(e.message));
  }
};

export const actions = { ...bundle.actions, fetchSearch };
export const reducer = bundle.reducer;
export const selectors = bundle.selectors;
