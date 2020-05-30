import React, { useState } from "react";
import AppTypes from "AppTypes";
import { Dispatch, bindActionCreators } from "redux";
import { connect } from "react-redux";

import { selectors as capiSelectors } from "redux/modules/capiContent";
import { CapiContentModel } from "services/capi";
import { notEmpty } from "utils/predicates";
import CapiFeedItem from "./CapiFeedItem";

type IProps = ReturnType<typeof mapStateToProps>;

const filterArticles = (
  articles: (CapiContentModel | undefined)[],
  showArticlesWithMatchesOnly: boolean
) =>
  showArticlesWithMatchesOnly
    ? (articles.filter(
        (_) => (_ !== undefined && !_.meta.matches) || _?.meta.matches.length
      ) as CapiContentModel[])
    : (articles.filter((_) => _ !== undefined) as CapiContentModel[]);

const CapiResults = ({
  articles,
  isLoading,
  pagination,
}: IProps) => {
  const [
    showArticlesWithMatchesOnly,
    setShowArticlesWithMatchesOnly,
  ] = useState(false);
  const checkboxId = "checkbox-show-article-matches";
  const articleArray = Object.values(articles).filter(notEmpty);
  const filteredArticles = filterArticles(
    articleArray,
    showArticlesWithMatchesOnly
  );
  return (
    <>
      {isLoading ? (
        <h5 className="text-secondary mt-2 mb-0">Loading</h5>
      ) : (
        <h5 className="mt-2 mb-0">
          {filteredArticles.length} of {pagination?.totalPages} articles
        </h5>
      )}
      <div className="form-check form-check-inline mt-2">
        <input
          className="form-check-input"
          type="checkbox"
          id={checkboxId}
          value={showArticlesWithMatchesOnly.toString()}
          onChange={(_) => setShowArticlesWithMatchesOnly(_.target.checked)}
        />
        <label className="form-check-label" htmlFor={checkboxId}>
          <small>Hide articles that don't have matches</small>
        </label>
      </div>
      <div className="list-group mt-2">
        {filteredArticles.map((article) => (
          <CapiFeedItem id={article.id} />
        ))}
      </div>
    </>
  );
};

const mapStateToProps = (state: AppTypes.RootState) => ({
  articles: capiSelectors.selectLastFetchedArticles(state),
  isLoading: capiSelectors.selectIsLoading(state),
  pagination: capiSelectors.selectPagination(state),
});

export default connect(mapStateToProps)(CapiResults);
