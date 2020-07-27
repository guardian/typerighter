import React from "react";
import classnames from "classnames";
import { connect } from "react-redux";

import AppTypes from "AppTypes";
import { selectors as capiSelectors } from "redux/modules/capiContent";
import {
  selectors as uiSelectors,
  actions as uiActions,
} from "redux/modules/ui";
import { selectors as searchMatchesSelectors } from "redux/modules/searchMatches";
import CapiFeedItem from "./CapiFeedItem";
import { bindActionCreators } from "redux";

type IProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const articleNumberFormat = new Intl.NumberFormat("en-GB");
const checkboxId = "capi-results__show-all-articles";

const CapiResults = ({
  articleIds,
  isLoading,
  pagination,
  searchMatchesLoadingText,
  displayAllArticles,
  doToggleDisplayAllArticles,
}: IProps) => {
  const totalArticles = pagination
    ? articleNumberFormat.format(
        articleIds.length < pagination.pageSize
          ? articleIds.length
          : pagination.totalPages * pagination.pageSize
      )
    : "?";

  return (
    <>
      <p className={classnames("mt-2 mb-0", { "text-secondary": isLoading })}>
        <span className="h5">{`Showing ${articleIds.length}  of ~${totalArticles} articles`}</span>
        {isLoading && (
          <span
            className="spinner-border spinner-border-sm text-primary float-right"
            role="status"
          >
            <span className="sr-only">Loading...</span>
          </span>
        )}
      </p>
      {searchMatchesLoadingText && <p>{searchMatchesLoadingText}</p>}
      <div className="form-check form-check-inline mt-2">
        <input
          className="form-check-input"
          type="checkbox"
          id={checkboxId}
          checked={displayAllArticles}
          onChange={doToggleDisplayAllArticles}
        />
        <label className="form-check-label" htmlFor={checkboxId}>
          <small>Show articles that don't have matches</small>
        </label>
      </div>
      <div className="list-group mt-2">
        {articleIds.map((id) => (
          <CapiFeedItem id={id} key={id} />
        ))}
      </div>
    </>
  );
};

const mapStateToProps = (state: AppTypes.RootState) => {
  const selectAllArticles = uiSelectors.selectDisplayAllArticles(state);
  const searchMode = uiSelectors.selectSearchMode(state);

  return {
    articleIds:
      searchMode === "MATCHES"
        ? searchMatchesSelectors.selectSearchMatchesArticleIds(state, selectAllArticles)
        : capiSelectors.selectLastFetchedArticleIds(state, selectAllArticles),
    isLoading:
      capiSelectors.selectIsLoading(state) ||
      searchMatchesSelectors.selectIsSearchMatchesInProgress(state),
    searchMatchesLoadingText: searchMatchesSelectors.selectSearchMatchesLoadingText(
      state
    ),
    pagination: capiSelectors.selectPagination(state),
    selectedArticle: uiSelectors.selectSelectedArticle(state),
    displayAllArticles: uiSelectors.selectDisplayAllArticles(state),
  };
};

const mapDispatchToProps = (dispatch: AppTypes.Dispatch) =>
  bindActionCreators(
    {
      doToggleDisplayAllArticles: uiActions.doToggleDisplayAllArticles,
    },
    dispatch
  );

export default connect(mapStateToProps, mapDispatchToProps)(CapiResults);
