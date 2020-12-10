import React from "react";
import CapiSearchOptions from "./CapiSearchOptions";
import CapiResults from "./CapiResults";

const CapiFeed = () => (
  <div className="d-flex flex-column">
    <CapiSearchOptions />
    <hr className="mt-3 mb-2" />
    <CapiResults />
  </div>
);

export default CapiFeed;
