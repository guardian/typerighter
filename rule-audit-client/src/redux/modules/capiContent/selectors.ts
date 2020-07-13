import AppTypes from "AppTypes";
import { notEmpty } from "utils/predicates";
import { selectors } from ".";

export const selectLastFetchedArticleIds = (
  state: AppTypes.RootState,
  selectAll = true
): string[] =>
  selectors
    .selectLastFetchOrder(state)
    .map((id) => selectors.selectById(state, id))
    .filter(notEmpty)
    .filter((article) => selectAll || article?.meta.matches.length)
    .map((_) => _.id);
