import { State, ResourceWithId, IndexedResource, RootState } from "./";
import { defaultArray } from "./utils";

/**
 * Create selectors for an AsyncResourceBundle.
 */
const createSelectors = <
  Resource extends {},
  IdProp extends string,
  LocalRootState extends RootState<Resource, IdProp>
>(
  selectLocalState: (state: LocalRootState) => State<Resource, IdProp>
) => {
  const selectPagination = (state: LocalRootState) =>
    selectLocalState(state).pagination;

  const selectCurrentError = (state: LocalRootState) =>
    selectLocalState(state).error;

  const selectLastError = (state: LocalRootState) =>
    selectLocalState(state).lastError;

  const selectLastFetch = (state: LocalRootState) =>
    selectLocalState(state).lastFetch;

  const selectIsLoading = (state: LocalRootState) =>
    !!selectLocalState(state).loadingIds.length;

  const selectIsLoadingById = (state: LocalRootState, id: string) =>
    selectLocalState(state).loadingIds.indexOf(id) !== -1;

  // It should be possible to remove the `any` here with conditional types.
  // This is a no-op for non-indexed resources.
  const selectById = (state: LocalRootState, id: string): Resource | undefined =>
    (selectLocalState(state).data as any)[id];

  const selectIsLoadingInitialDataById = (state: LocalRootState, id: string) =>
    !selectById(state, id) &&
    selectLocalState(state).loadingIds.indexOf(id) !== -1;

  const selectLastFetchOrder = (state: LocalRootState): string[] =>
    selectLocalState(state).lastFetchOrder || defaultArray;

  const selectAll = (state: LocalRootState) => selectLocalState(state).data;

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
