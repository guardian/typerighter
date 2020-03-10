import React, { useState } from "react";
import AppTypes from "AppTypes";
import { Dispatch, bindActionCreators } from "redux";
import { connect } from "react-redux";
import classnames from "classnames";

import { selectors as capiSelectors } from "redux/modules/capiContent";
import {
  selectors as uiSelectors,
  actions as uiActions
} from "redux/modules/ui";
import { CapiContentModel } from "services/capi";

type IProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps>;

const filterArticles = (
  articles: (CapiContentModel | undefined)[],
  showArticlesWithMatchesOnly: boolean
) =>
  showArticlesWithMatchesOnly
    ? (articles.filter(
        _ => (_ !== undefined && !_.meta.matches) || _?.meta.matches.length
      ) as CapiContentModel[])
    : (articles.filter(_ => _ !== undefined) as CapiContentModel[]);

const CapiResults = ({
  content,
  selectArticle,
  selectedArticle,
  isLoading,
  pagination
}: IProps) => {
  const [
    showArticlesWithMatchesOnly,
    setShowArticlesWithMatchesOnly
  ] = useState(false);
  const checkboxId = "checkbox-show-article-matches";
  const articles = Object.values(content);
  const filteredArticles = filterArticles(
    articles,
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
          onChange={_ => setShowArticlesWithMatchesOnly(_.target.checked)}
        />
        <label className="form-check-label" htmlFor={checkboxId}>
          <small>Hide articles that don't have matches</small>
        </label>
      </div>
      <div className="list-group mt-2">
        {filteredArticles.map(article => (
          <a
            href="#"
            className={classnames(
              "list-group-item list-group-item-action d-flex justify-content-between align-items-center",
              {
                active: article.id === selectedArticle,
                "list-group-item-success":
                  article.meta.matches && !article.meta.matches.length,
                "list-group-item-danger":
                  article.meta.matches && article.meta.matches.length
              }
            )}
            key={article.id}
            onClick={() => selectArticle(article.id)}
          >
            <div className="card-text">{article.webTitle}</div>
            {article.meta.matches ? (
              article.meta.matches.length ? (
                <div className="badge badge-danger ml-1">
                  {article.meta.matches.length}
                </div>
              ) : (
                <div className="badge badge-success ml-1">✓</div>
              )
            ) : (
              <div className="badge ml-1">?</div>
            )}
          </a>
        ))}
      </div>
    </>
  );
};

const mapStateToProps = (state: AppTypes.RootState) => ({
  content: capiSelectors
    .selectLastFetchOrder(state)
    .map(id => capiSelectors.selectById(state, id))
    .filter(_ => _),
  isLoading: capiSelectors.selectIsLoading(state),
  pagination: capiSelectors.selectPagination(state),
  selectedArticle: uiSelectors.selectSelectedArticle(state)
});

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      selectArticle: uiActions.selectArticle
    },
    dispatch
  );

export default connect(mapStateToProps, mapDispatchToProps)(CapiResults);
