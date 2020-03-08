import * as React from "react";
import { connect } from "react-redux";
import { Dispatch, bindActionCreators } from "redux";
import { useEffect, useState } from "react";
import Select from "react-select";

import AppTypes from "AppTypes";
import { actions, selectors, thunks } from "redux/modules/capiContent";
import {
  CapiTag,
  CapiSection,
  fetchCapiTags,
  fetchCapiSections
} from "services/capi";

type IProps = ReturnType<typeof mapDispatchToProps> &
  ReturnType<typeof mapStateToProps>;

type Selection = { value: string; label: string };

const CapiSearchOptions = ({ fetchSearch, fetchMatches }: IProps) => {
  const [query, setQuery] = useState<string>("");
  const [selectedTags, setSelectedTags] = useState<Selection[]>([]);
  const [selectedSections, setSelectedSections] = useState<Selection[]>([]);
  const [tags, setTags] = useState<CapiTag[]>([]);
  const [sections, setSections] = useState<CapiSection[]>([]);

  useEffect(() => {
    fetchSearch(
      query,
      selectedTags.map(_ => _.value),
      selectedSections.map(_ => _.value)
    );
  }, [query, selectedTags, selectedSections]);

  return (
    <div>
      <h5>Search CAPI Content</h5>
      <div className="row">
        <div className="col">
          <input
            value={query}
            type="text"
            className="form-control"
            placeholder="e.g. UK Politics"
            onChange={_ => setQuery(_.target.value)}
          />
        </div>
      </div>
      <div className="row mt-2">
        <div className="col-6">
          <Select
            isMulti
            options={tags.map(tag => ({ value: tag.id, label: tag.webTitle }))}
            placeholder="Tags"
            value={selectedTags}
            onChange={(tags: any) => setSelectedTags(tags || [])}
            onInputChange={val => {
              fetchCapiTags(val).then(_ => setTags(_.results || []));
            }}
          />
        </div>
        <div className="col-6">
          <Select
            isMulti
            options={sections.map(section => ({
              value: section.id,
              label: section.webTitle
            }))}
            placeholder="Sections"
            value={selectedSections}
            onChange={(sections: any) => setSelectedSections(sections || [])}
            onInputChange={(val: any) => {
              fetchCapiSections(val).then(_ => setSections(_.results || []));
            }}
          />
        </div>
      </div>
      <button className="btn btn-primary mt-2 w-100" onClick={() => fetchMatches()}>
        Find matches for this search
      </button>
    </div>
  );
};

const mapStateToProps = (state: AppTypes.RootState) => ({
  content: selectors.selectAll(state)
});

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      fetchSearch: thunks.fetchSearch,
      fetchMatches: thunks.fetchMatches
    },
    dispatch
  );

export default connect(mapStateToProps, mapDispatchToProps)(CapiSearchOptions);
