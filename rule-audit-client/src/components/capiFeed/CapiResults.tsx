import React, { useState } from "react";
import AppTypes from "AppTypes";
import { connect } from "react-redux";

import { selectors as capiSelectors } from "redux/modules/capiContent";
import { selectors as uiSelectors } from "redux/modules/ui";
import { CapiContentWithMatches } from "services/capi";
import { notEmpty } from "utils/predicates";
import CapiFeedItem from "./CapiFeedItem";

type IProps = ReturnType<typeof mapStateToProps>;

const articleNumberFormat = new Intl.NumberFormat("en-GB");
const checkboxId = "capi-results__show-all-articles";

const CapiResults = ({ articleIds, isLoading, pagination }: IProps) => {
  const totalArticles = pagination
    ? articleNumberFormat.format(
        articleIds.length < pagination.pageSize
          ? articleIds.length
          : pagination.totalPages * pagination.pageSize
      )
    : "?";
  return (
    <>
      {isLoading ? (
        <h5 className="text-secondary mt-2 mb-0">Loading</h5>
      ) : (
        <h5 className="mt-2 mb-0">
          {articleIds.length} of {totalArticles} articles
        </h5>
      )}
      <div className="list-group mt-2">
        {articleIds.map((id) => (
          <CapiFeedItem id={id} />
        ))}
      </div>
    </>
  );
};

const mapStateToProps = (state: AppTypes.RootState) => ({
  articleIds: capiSelectors.selectLastFetchedArticleIds(state),
  isLoading: capiSelectors.selectIsLoading(state),
  pagination: capiSelectors.selectPagination(state),
  selectedArticle: uiSelectors.selectSelectedArticle(state),
});

export default connect(mapStateToProps)(CapiResults);
