import AppTypes from "AppTypes";

import { selectors } from "../capiContent";

export const selectSearchMatchesArticleIds = (
  state: AppTypes.RootState,
  selectAll = true
): string[] =>
  selectAll
    ? state.searchMatches.searchMatchesArticleIds
    : selectSearchMatchesArticleIdsWithMatches(state);

export const selectSearchMatchesArticleIdsWithMatches = (
  state: AppTypes.RootState
): string[] =>
  selectSearchMatchesArticleIds(state).filter(
    (id) => selectors.selectById(state, id)?.meta.matches.length
  );

export const selectIsSearchMatchesInProgress = (state: AppTypes.RootState): boolean =>
  state.searchMatches.isSearchMatchesInProgress;

export const selectSearchMatchesLoadingText = (
  state: AppTypes.RootState
): string => {
  const {
    searchMatches: { searchMatchesArticleIds },
  } = state;
  const idsWithMatches = selectSearchMatchesArticleIdsWithMatches(state);

  if (
    !searchMatchesArticleIds.length ||
    !idsWithMatches.length
  ) {
    return '';
  }

  return `${idsWithMatches.length} have matches (${Math.floor(
    (idsWithMatches.length / searchMatchesArticleIds.length) * 100
  )})%`;
};

export const selectSearchMatchesLimit = (state: AppTypes.RootState): number =>
  state.searchMatches.searchMatchesLimit;
