import { Dispatch } from "redux";
import set from "lodash/fp/set";

import AppTypes from "AppTypes";
import { getBlocksFromHtmlString } from "utils/prosemirror";
import { fetchTyperighterMatches } from "services/typerighter";
import { fetchCapiSearch, CapiContentWithMatches } from "services/capi";
import { actions, selectors } from ".";

export const doFetchCapi = (
  query: string,
  tags: string[],
  sections: string[],
  page?: number,
  fetchCapiSearchService = fetchCapiSearch
) => async (dispatch: AppTypes.Dispatch): Promise<CapiContentWithMatches[]> => {
  dispatch(actions.fetchStart());
  try {
    const content = await fetchCapiSearchService(query, tags, sections, page);
    const models = content.results?.map((article) => ({
      ...article,
      meta: {
        blocks: getBlocksFromHtmlString(article.fields.body),
        matches: [],
      },
    }));
    dispatch(
      actions.fetchSuccess(models, {
        pagination: {
          pageSize: content.pageSize,
          totalPages: content.pages,
          currentPage: content.currentPage,
        },
      })
    );
    return models || [];
  } catch (e) {
    dispatch(actions.fetchError(e.message));
    return [];
  }
};

export const doFetchMatchesForLastSearch = (
  fetchTyperighterMatchesService = fetchTyperighterMatches
) => async (
  dispatch: AppTypes.Dispatch,
  getState: () => AppTypes.RootState
) => {
  const articles = selectors.selectLastFetchedArticleIds(getState(), true);
  return dispatch(doFetchMatches(articles, fetchTyperighterMatchesService));
};

export const doFetchMatches = (
  articleIds: string[],
  fetchTyperighterMatchesService = fetchTyperighterMatches
) => async (
  dispatch: Dispatch,
  getState: () => AppTypes.RootState
): Promise<(CapiContentWithMatches | undefined)[]> => {
  const state = getState();
  const fetchMatchPromises = articleIds.map(async (id) => {
    const article = selectors.selectById(state, id);
    if (!article) {
      return Promise.resolve(undefined);
    }
    dispatch(actions.updateStart(article));
    const { matches } = await fetchTyperighterMatchesService(
      id,
      article.meta.blocks
    );
    const articleWithMatches = set(["meta", "matches"], matches, article);
    dispatch(actions.updateSuccess(id, articleWithMatches));
    return articleWithMatches;
  });
  return await Promise.all(fetchMatchPromises);
};
