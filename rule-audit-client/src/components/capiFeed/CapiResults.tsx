import React from "react";
import AppTypes from "AppTypes";
import { selectors } from "redux/modules/capiContent";
import { MapStateToProps, connect } from "react-redux";
import { CapiContent } from "services/Capi";

type IProps = ReturnType<typeof mapStateToProps>;

const CapiResults = ({ content }: IProps) => (
  <div className="mt-2">
    {Object.values(content).map((article: CapiContent) => (
      <div className="card mt-2">
        <div className="card-body">
          <div className="card-text">{article.webTitle}</div>
          <div className="badge badge-warning mr-1">2</div>
          <div className="badge badge-danger">4</div>
        </div>
      </div>
    ))}
  </div>
);

const mapStateToProps = (state: AppTypes.RootState) => ({
  content: selectors.selectAll(state) as {[id: string]: CapiContent}
});

export default connect(mapStateToProps)(CapiResults);
