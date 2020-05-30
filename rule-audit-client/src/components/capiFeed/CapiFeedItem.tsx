import React from "react";
import { connect } from "react-redux";
import { bindActionCreators, Dispatch } from "redux";
import classnames from "classnames";

import AppTypes from "AppTypes";
import {
  selectors as uiSelectors,
  actions as uiActions,
} from "redux/modules/ui";
import { selectors as capiSelectors } from "redux/modules/capiContent";
import { CapiContentModel } from "services/capi";

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
      className={classnames(
        "list-group-item list-group-item-action d-flex justify-content-between align-items-center",
        {
          active: article.id === selectedArticle,
          "list-group-item-success":
            article.meta.matches && !article.meta.matches.length,
          "list-group-item-danger":
            article.meta.matches && article.meta.matches.length,
        }
      )}
      key={article.id}
      onClick={() => doSelectArticle(article.id)}
    >
      <div className="card-text">{article.webTitle}</div>
      {renderBadge(article, isLoading)}
    </a>
  );
};

const renderBadge = (article: CapiContentModel, isLoading: boolean) => {
  if (isLoading) {
    return <div className="badge ml-1">...</div>;
  }
  if (!article.meta.matches) {
    return <div className="badge ml-1">?</div>;
  }
  return article.meta.matches.length ? (
    <div className="badge badge-danger ml-1">{article.meta.matches.length}</div>
  ) : (
    <div className="badge badge-success ml-1">✓</div>
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
