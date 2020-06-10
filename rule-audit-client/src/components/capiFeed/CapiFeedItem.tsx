import React from "react";
import { connect } from "react-redux";
import { bindActionCreators, Dispatch } from "redux";

import AppTypes from "AppTypes";
import {
  selectors as uiSelectors,
  actions as uiActions,
} from "redux/modules/ui";
import { selectors as capiSelectors } from "redux/modules/capiContent";
import { CapiContentWithMatches } from "services/capi";

type ExternalProps = { id: string };

type IProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  ExternalProps;

const CapiFeedItem = ({
  doSelectArticle,
  selectedArticle,
  article,
  isLoading,
}: IProps) => {
  if (!article) {
    return (
      <div className="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
        Article not found
      </div>
    );
  }
  return (
    <a
      href="#"
      className="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
      key={article.id}
      onClick={() => doSelectArticle(article.id)}
    >
      <div className="card-text">{article.webTitle}</div>
      {renderBadges(article, isLoading)}
    </a>
  );
};

const renderBadges = (article: CapiContentWithMatches, isLoading: boolean) => {
  if (isLoading) {
    return (
      <div className="badge ml-1" title="Loading">
        ...
      </div>
    );
  }
  if (!article.meta.matches) {
    return (
      <div className="badge ml-1" title="Not yet checked">
        ?
      </div>
    );
  }
  if (article.meta.matches.length) {
    const noOfCorrectMatches = article.meta.matches.filter(
      (_) => _.markAsCorrect
    ).length;
    const noOfIncorrectMatches =
      article.meta.matches.length - noOfCorrectMatches;
    return (
      <span>
        {!!noOfCorrectMatches && (
          <div
            className="badge badge-success ml-1"
            title={`${noOfCorrectMatches} matches marked as correct`}
          >
            {noOfCorrectMatches}
          </div>
        )}
        {!!noOfIncorrectMatches && (
          <div
            className="badge badge-danger ml-1"
            title={`${noOfIncorrectMatches} matches marked as incorrect`}
          >
            {noOfIncorrectMatches}
          </div>
        )}
      </span>
    );
  }

  return (
    <div className="badge badge-default ml-1" title="No matches">
      ✓
    </div>
  );
};

const mapStateToProps = (state: AppTypes.RootState, { id }: ExternalProps) => ({
  article: capiSelectors.selectById(state, id),
  isLoading: capiSelectors.selectIsLoadingById(state, id),
  selectedArticle: uiSelectors.selectSelectedArticle(state),
});

const mapDispatchToProps = (dispatch: Dispatch, _: ExternalProps) =>
  bindActionCreators(
    {
      doSelectArticle: uiActions.doSelectArticle,
    },
    dispatch
  );

export default connect(mapStateToProps, mapDispatchToProps)(CapiFeedItem);
