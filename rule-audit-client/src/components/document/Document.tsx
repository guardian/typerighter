import * as React from "react";
import AppTypes from "AppTypes";
import { connect } from "react-redux";

import { selectors as capiSelectors } from "redux/modules/capiContent";
import { selectors as uiSelectors } from "redux/modules/ui";
import ProsemirrorDocument from "./ProsemirrorDocument";

type IProps = ReturnType<typeof mapStateToProps>;

const Document = ({ article }: IProps) => (
  <div className="card">
    <div className="card-body">
      {article ? (
        <ProsemirrorDocument
          htmlStr={article.fields.body}
          matches={article.meta.matches}
        />
      ) : (
        <div className="text-secondary">Select an article from the feed.</div>
      )}
    </div>
  </div>
);

const mapStateToProps = (state: AppTypes.RootState) => ({
  article: capiSelectors.selectById(
    state,
    uiSelectors.selectSelectedArticle(state)
  )
});

export default connect(mapStateToProps)(Document);
