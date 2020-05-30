import { Dispatch } from "redux";
import set from "lodash/fp/set";
import AppTypes from "AppTypes";
import { createAsyncResourceBundle } from "redux-bundle-creator";

import { CapiContentModel, fetchCapiSearch } from "services/capi";
import { getBlocksFromHtmlString } from "utils/prosemirror";
import { fetchTyperighterMatches } from "services/typerighter";

const bundle = createAsyncResourceBundle<CapiContentModel, {}, "capi">("capi", {
  indexById: true
});

const fetchSearch = (
  query: string,
  tags: string[],
  sections: string[]
) => async (dispatch: Dispatch): Promise<void> => {
  dispatch(bundle.actions.fetchStart());
  try {
    const content = await fetchCapiSearch(query, tags, sections);
    const models = content.results?.map(article => ({
      ...article,
      meta: {
        blocks: getBlocksFromHtmlString(article.fields.body),
        matches: undefined
      }
    }));
    dispatch(
      bundle.actions.fetchSuccess(models, {
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

const fetchMatches = () => async (
  dispatch: Dispatch,
  getState: () => AppTypes.RootState
) => {
  const articles = bundle.selectors.selectAll(getState());
  Object.values(articles).map(async article => {
    const { matches } = await fetchTyperighterMatches(
      article.id,
      article.meta.blocks
    );
    const articleWithMatches = set(["meta", "matches"], matches, article);
    dispatch(bundle.actions.updateSuccess(article.id, articleWithMatches));
  });
};

export const actions = {
  ...bundle.actions
};

export const thunks = {
  fetchSearch,
  fetchMatches
};

export const reducer = bundle.reducer;
export const selectors = bundle.selectors;
