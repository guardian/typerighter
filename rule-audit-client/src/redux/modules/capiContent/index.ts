import { Dispatch } from "redux";
import set from "lodash/fp/set";
import AppTypes from "AppTypes";
import { createAsyncResourceBundle } from "redux-bundle-creator";
import { IBlock } from "@guardian/prosemirror-typerighter/dist/interfaces/IMatch";

import { CapiContentWithMatches, fetchCapiSearch } from "services/capi";
import { getBlocksFromHtmlString } from "utils/prosemirror";
import { fetchTyperighterMatches } from "services/typerighter";
import { notEmpty } from "utils/predicates";

const bundle = createAsyncResourceBundle<CapiContentWithMatches, {}, "capi">("capi", {
  indexById: true,
});

const fetchSearch = (
  query: string,
  tags: string[],
  sections: string[]
) => async (dispatch: Dispatch): Promise<void> => {
  dispatch(bundle.actions.fetchStart());
  try {
    const content = await fetchCapiSearch(query, tags, sections);
    const models = content.results?.map((article) => ({
      ...article,
      meta: {
        blocks: getBlocksFromHtmlString(article.fields.body),
        matches: undefined,
      },
    }));
    dispatch(
      bundle.actions.fetchSuccess(models, {
        pagination: {
          pageSize: content.pageSize,
          totalPages: content.pages,
          currentPage: content.currentPage,
        },
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
  const articles = selectLastFetchedArticles(getState());
  Object.values(articles).map(async (article) => {
    dispatch(bundle.actions.updateStart(article));
    const { matches } = await fetchTyperighterMatches(
      article.id,
      article.meta.blocks
    );
    const articleWithMatches = set(["meta", "matches"], matches, article);
    dispatch(bundle.actions.updateSuccess(article.id, articleWithMatches));
  });
};

const selectLastFetchedArticles = (
  state: AppTypes.RootState
): CapiContentWithMatches[] =>
  selectors
    .selectLastFetchOrder(state)
    .map((id) => selectors.selectById(state, id))
    .filter(notEmpty);

export const actions = {
  ...bundle.actions,
};

export const thunks = {
  fetchSearch,
  fetchMatches,
};

export const selectors = { ...bundle.selectors, selectLastFetchedArticles };

export const reducer = bundle.reducer;
