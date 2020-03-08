import { State } from "./";
import { defaultArray } from "./utils";

/**
 * Create selectors for an AsyncResourceBundle.
 */
const createSelectors = <Resource extends {}, RootState extends {}>(
  selectLocalState: (state: RootState) => State<Resource>
) => {
  const selectPagination = (state: RootState) =>
    selectLocalState(state).pagination;

  const selectCurrentError = (state: RootState) =>
    selectLocalState(state).error;

  const selectLastError = (state: RootState) =>
    selectLocalState(state).lastError;

  const selectLastFetch = (state: RootState) =>
    selectLocalState(state).lastFetch;

  const selectIsLoading = (state: RootState) =>
    !!selectLocalState(state).loadingIds.length;

  const selectIsLoadingById = (state: RootState, id: string) =>
    selectLocalState(state).loadingIds.indexOf(id) !== -1;

  const selectById = (state: RootState, id: string): Resource | undefined =>
    selectLocalState(state).data[id];

  const selectIsLoadingInitialDataById = (state: RootState, id: string) =>
    !selectById(state, id) &&
    selectLocalState(state).loadingIds.indexOf(id) !== -1;

  const selectLastFetchOrder = (state: RootState): string[] =>
    selectLocalState(state).lastFetchOrder || defaultArray;

  const selectAll = (state: RootState) => selectLocalState(state).data;

  return {
    selectPagination,
    selectCurrentError,
    selectLastError,
    selectLastFetch,
    selectIsLoading,
    selectIsLoadingById,
    selectIsLoadingInitialDataById,
    selectById,
    selectLastFetchOrder,
    selectAll
  };
};

export default createSelectors;
