import React from "react";
import AppTypes from "AppTypes";
import { selectors } from "redux/modules/capiContent";
import { connect } from "react-redux";

type IProps = ReturnType<typeof mapStateToProps>;

const CapiResults = ({ content }: IProps) => (
  <div className="mt-2">
    {Object.values(content).map(article => (
      <div className="card mt-2" key={article.id}>
        <div className="card-body">
          <div className="card-text">{article.webTitle}</div>
          {article.meta.matches ? (
            article.meta.matches.length ? (
              <div className="badge badge-danger">
                {article.meta.matches.length}
              </div>
            ) : (
              <div className="text-success">✓</div>
            )
          ) : (
            <div className="badge">?</div>
          )}
        </div>
      </div>
    ))}
  </div>
);

const mapStateToProps = (state: AppTypes.RootState) => ({
  content: selectors.selectAll(state)
});

export default connect(mapStateToProps)(CapiResults);
