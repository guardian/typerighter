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
import { debounce } from 'lodash';

import AppTypes from "AppTypes";
import { selectors, thunks } from "redux/modules/capiContent";
import {
  fetchCapiTags,
  fetchCapiSections,
} from "services/capi";

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
    .map(_ => _.value)
    .join(""),
  tags: elements
    .filter((element) => element.type === "filter" && element.name === "Tag")
    .map(_ => _.value),
  sections: elements
    .filter(
      (element) => element.type === "filter" && element.name === "Section"
    )
    .map(_ => _.value)
});

const CapiSearchOptions = ({ fetchCapi, fetchMatches }: IProps) => {
  const [queryElements, setQueryElements] = useState<QueryElement[]>([]);

  useEffect(() => {
    const { query, tags, sections } = getSearchEntitiesFromQueryElements(
      queryElements
    );
    fetchCapi(query, tags, sections);
  }, [queryElements]);

  return (
    <div>
      <InputSupper
        elements={queryElements}
        availableFilters={filters}
        onChange={setQueryElements}
      />
      <button
        className="btn btn-primary mt-2 w-100"
        onClick={() => fetchMatches()}
      >
        Find matches for this search
      </button>
    </div>
  );
};

const mapStateToProps = (state: AppTypes.RootState) => ({
  content: selectors.selectAll(state),
});

const mapDispatchToProps = (dispatch: Dispatch) => {
  const { fetchCapi, fetchMatches } = bindActionCreators(
    {
      fetchCapi: thunks.doFetchCapi,
      fetchMatches: thunks.doFetchMatchesForLastSearch,
    },
    dispatch
  );
  return {
    fetchCapi: debounce(fetchCapi, 500),
    fetchMatches
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(CapiSearchOptions);
