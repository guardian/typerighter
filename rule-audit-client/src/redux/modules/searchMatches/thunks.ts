import AppTypes from "AppTypes";
import { fetchCapiSearch } from "services/capi";
import { fetchTyperighterMatches } from "services/typerighter";
import * as selectors from "./selectors";
import * as actions from "./actions";
import { notEmpty } from "utils/predicates";
import { doFetchCapi, doFetchMatches } from "../capiContent/thunks";

export const doSearchMatches = (
  query: string,
  tags: string[],
  sections: string[],
  fetchCapiSearchService = fetchCapiSearch,
  fetchTyperighterMatchesService = fetchTyperighterMatches
): AppTypes.Thunk => async (
  dispatch: AppTypes.Dispatch,
  getState: () => AppTypes.RootState
): Promise<void> => {
  dispatch(actions.doSearchMatchesStart());

  const loop = async (
    noOfArticlesToFetch: number,
    page: number = 1
  ): Promise<void> => {
    const articles = await dispatch(
      doFetchCapi(query, tags, sections, page, fetchCapiSearchService)
    );

    const incomingArticleIds = (articles || []).map((_) => _.id);

    const articlesWithMatchData = await dispatch(
      doFetchMatches(
        articles.map((_) => _.id),
        fetchTyperighterMatchesService
      )
    );

    const articleIdsWithMatches = articlesWithMatchData
      .filter(notEmpty)
      .filter((_) => _.meta.matches.length)
      .map((_) => _.id);

    const noOfArticlesToFetchRemaining =
      noOfArticlesToFetch - articleIdsWithMatches.length;

    // If we have more articles with matches than the user has asked for, discard the remainder
    const lastMatchId =
      noOfArticlesToFetchRemaining < 0
        ? articleIdsWithMatches[
            articleIdsWithMatches.length + noOfArticlesToFetchRemaining
          ]
        : undefined;
    const lastValidIndex = lastMatchId
      ? incomingArticleIds.indexOf(lastMatchId)
      : undefined;
    const articleIdsToAdd = incomingArticleIds.slice(0, lastValidIndex);

    dispatch(actions.doAppendSearchMatchesArticleIds(articleIdsToAdd));

    if (
      noOfArticlesToFetchRemaining > 0 &&
      // Do not continue if the user has stopped the search operation
      selectors.selectIsSearchMatchesInProgress(getState())
    ) {
      await loop(noOfArticlesToFetchRemaining, page + 1);
    } else {
      dispatch(actions.doSearchMatchesEnd());
    }
  };

  try {
    const searchMatchesLimit = selectors.selectSearchMatchesLimit(getState());
    await loop(searchMatchesLimit);
  } catch (e) {
    dispatch(actions.doSearchMatchesEnd());
  }
};
