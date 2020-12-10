import * as React from "react";
import { connect } from "react-redux";
import { Dispatch, bindActionCreators } from "redux";
import { useEffect, useState } from "react";
import {
  InputSupper,
  QueryElement,
  Filter,
  SelectAsyncFilter,
} from "@guardian/threads";
import { debounce } from "lodash";

import AppTypes from "AppTypes";
import { selectors, thunks, actions } from "redux/modules/searchMatches";
import { selectors as capiSelectors, thunks as capiThunks } from "redux/modules/capiContent";
import {
  selectors as uiSelectors,
  actions as uiActions,
} from "redux/modules/ui";
import { fetchCapiTags, fetchCapiSections } from "services/capi";

type IProps = ReturnType<typeof mapDispatchToProps> &
  ReturnType<typeof mapStateToProps>;

const filters: (Filter | SelectAsyncFilter)[] = [
  {
    name: "Tag",
    type: "select_async",
    onInputChange: async (input: string) => {
      const tags = await fetchCapiTags(input);
      return (
        tags?.results?.map((tag) => ({
          label: tag.webTitle,
          value: tag.id,
        })) || []
      );
    },
  },
  {
    name: "Section",
    type: "select_async",
    onInputChange: async (input: string) => {
      const tags = await fetchCapiSections(input);
      return (
        tags?.results?.map((tag) => ({
          label: tag.webTitle,
          value: tag.id,
        })) || []
      );
    },
  },
];

const getSearchEntitiesFromQueryElements = (elements: QueryElement[]) => ({
  query: elements
    .filter((element) => element.type === "text")
    .map((_) => _.value)
    .join(""),
  tags: elements
    .filter((element) => element.type === "filter" && element.name === "Tag")
    .map((_) => _.value),
  sections: elements
    .filter(
      (element) => element.type === "filter" && element.name === "Section"
    )
    .map((_) => _.value),
});

const CapiSearchOptions = ({
  fetchSearch,
  fetchMatches,
  isLoading,
  searchMode,
  setSearchMode,
  searchMatchesLimit,
  setSearchMatchesLimit,
  searchMatches,
}: IProps) => {
  const [queryElements, setQueryElements] = useState<QueryElement[]>([]);

  useEffect(() => {
    if (searchMode !== "ARTICLES") {
      return;
    }
    const { query, tags, sections } = getSearchEntitiesFromQueryElements(
      queryElements
    );
    fetchSearch(query, tags, sections, 1);
  }, [queryElements]);

  const searchMatchesWithCurrentQuery = () => {
    const { query, tags, sections } = getSearchEntitiesFromQueryElements(
      queryElements
    );
    searchMatches(query, tags, sections);
  };

  return (
    <div>
      <ul className="nav nav-tabs mb-2">
        <li className="nav-item">
          <a
            className={`nav-link ${searchMode === "ARTICLES" && "active"}`}
            href="#"
            onClick={() => setSearchMode("ARTICLES")}
          >
            Search articles
          </a>
        </li>
        <li className="nav-item">
          <a
            className={`nav-link ${searchMode === "MATCHES" && "active"}`}
            href="#"
            onClick={() => setSearchMode("MATCHES")}
          >
            Search matches
          </a>
        </li>
      </ul>
      <InputSupper
        elements={queryElements}
        availableFilters={filters}
        onChange={setQueryElements}
      />
      {searchMode === "MATCHES" && (
        <div className="input-group mt-2">
          <div className="input-group-prepend">
            <button
              className="btn btn-primary"
              onClick={searchMatchesWithCurrentQuery}
              disabled={isLoading}
            >
              Search for matches, limit
            </button>
          </div>
          <input
            className="form-control"
            type="number"
            step="1"
            min="1"
            max="100"
            value={searchMatchesLimit}
            onChange={(e) => setSearchMatchesLimit(parseInt(e.target.value))}
          />
        </div>
      )}
      <button
        className="btn btn-secondary mt-2 w-100"
        onClick={() => fetchMatches()}
      >
        Refresh matches
      </button>
    </div>
  );
};

const mapStateToProps = (state: AppTypes.RootState) => ({
  searchMatchesLimit: selectors.selectSearchMatchesLimit(state),
  searchMode: uiSelectors.selectSearchMode(state),
  isLoading:
    capiSelectors.selectIsLoading(state) ||
    selectors.selectIsSearchMatchesInProgress(state),
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  const boundActions = bindActionCreators(
    {
      fetchSearch: capiThunks.doFetchCapi,
      fetchMatches: capiThunks.doFetchMatchesForLastSearch,
      searchMatches: thunks.doSearchMatches,
      setSearchMatchesLimit: actions.doSetSearchMatchesLimit,
      setSearchMode: uiActions.doSetSearchMode,
    },
    dispatch
  );
  return {
    ...boundActions,
    fetchSearch: debounce(boundActions.fetchSearch, 500),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(CapiSearchOptions);
