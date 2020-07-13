import { createAction } from "typesafe-actions";

export const doSetSearchMatchesLimit = createAction("SET_SEARCH_MATCHES_ARTICLE_LIMIT")<number>();

export const doAppendSearchMatchesArticleIds = createAction("SET_SEARCH_MATCHES_ARTICLE_IDS")<
  string[]
>();

export const doSearchMatchesStart = createAction(
  "SEARCH_MATCHES_ARTICLES_WITH_MATCHES_START"
)();

export const doSearchMatchesEnd = createAction(
  "SEARCH_MATCHES_ARTICLES_WITH_MATCHES_END"
)();
