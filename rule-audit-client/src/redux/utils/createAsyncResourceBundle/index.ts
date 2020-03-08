import without from 'lodash/without';
import isEqual from 'lodash/isEqual';

interface BaseResource {
  id: string;
}

const FETCH_START = 'FETCH_START';  
const FETCH_SUCCESS = 'FETCH_SUCCESS';
const FETCH_SUCCESS_IGNORE = 'FETCH_SUCCESS_IGNORE'; // clears loading ids for unchanged collections during collection polling
const FETCH_ERROR = 'FETCH_ERROR';
const UPDATE_START = 'UPDATE_START';
const UPDATE_SUCCESS = 'UPDATE_SUCCESS';
const UPDATE_ERROR = 'UPDATE_ERROR';

interface FetchStartAction {
  entity: string;
  type: 'FETCH_START';
  payload: { ids?: string[] | string };
}

interface FetchSuccessAction<Resource> {
  entity: string;
  type: 'FETCH_SUCCESS';
  payload: {
    data: Resource | Resource[] | any;
    order?: string[];
    pagination?: IPagination;
    time: number;
  };
}

interface FetchSuccessIgnoreAction<Resource> {
  entity: string;
  type: 'FETCH_SUCCESS_IGNORE';
  payload: {
    data: Resource | Resource[] | any;
    time: number;
  };
}

interface FetchErrorAction {
  entity: string;
  type: 'FETCH_ERROR';
  payload: {
    error: string;
    time: number;
    ids?: string | string[];
  };
}

interface UpdateStartAction<Resource> {
  entity: string;
  type: 'UPDATE_START';
  payload: { id?: string | string; data: Resource | any };
}

interface UpdateSuccessAction<Resource> {
  entity: string;
  type: 'UPDATE_SUCCESS';
  payload: { data: Resource | any; id: string; time: number };
}

interface UpdateErrorAction {
  entity: string;
  type: 'UPDATE_ERROR';
  payload: {
    error: string;
    id: string;
    time: number;
  };
}

type Actions<Resource> =
  | FetchStartAction
  | FetchSuccessAction<Resource>
  | FetchSuccessIgnoreAction<Resource>
  | FetchErrorAction
  | UpdateStartAction<Resource>
  | UpdateSuccessAction<Resource>
  | UpdateErrorAction;

const defaultArray = [] as string[];
const globalLoadingIndicator = '@@ALL';

const getStatusIdsFromData = (
  data: BaseResource | BaseResource[] | any
): string[] | string =>
  data instanceof Array
    ? data.map((resource: BaseResource) => resource.id || '')
    : data.id || '';

const applyStatusIds = (
  currentIds: string[],
  incomingIds?: string | string[]
) => currentIds.concat(incomingIds || globalLoadingIndicator);

const removeStatusIds = (
  currentIds: string[],
  incomingIds: string[] | string = ''
): string[] =>
  incomingIds instanceof Array
    ? without(currentIds, ...incomingIds, globalLoadingIndicator)
    : without<string>(currentIds, incomingIds);

function formatIncomingResourceData<Resource extends BaseResource>(
  data: { [id: string]: Resource } | {},
  newData: Resource | Resource[],
  resourceName: string
): Resource | { [id: string]: Resource } {
  if (newData instanceof Array) {
    const result: { [id: string]: Resource } = {
      ...data,
      ...newData.reduce((acc, model: BaseResource, index) => {
        if (!model.id) {
          throw new Error(
            `[asyncResourceBundle]: Cannot apply new data - incoming resource ${resourceName} is missing ID at index ${index}.`
          );
        }
        return {
          ...acc,
          [model.id]: model
        };
      }, {})
    };
    return result;
  }

  if (!newData.id) {
    throw new Error(
      `[asyncResourceBundle]: Cannot apply new data - incoming resource ${resourceName} with keys ${Object.keys(
        newData
      ).join(', ')} is missing id.`
    );
  }

  return {
    ...data,
    [newData.id]: newData
  };
}

function getOrderFromIncomingResourceData<Resource extends BaseResource>(
  newData: Resource | Resource[],
  resourceName: string,
  currentOrder: string[] = defaultArray,
  newOrder?: string[]
): string[] {
  const order =
    newOrder ||
    (newData instanceof Array
      ? (newData as Resource[]).map((model, index) => {
          if (!model.id) {
            throw new Error(
              `[asyncResourceBundle]: Cannot apply new data - incoming resource ${resourceName} is missing ID at index ${index}.`
            );
          }
          return model.id;
        })
      : []);
  return isEqual(currentOrder, order) ? currentOrder : order;
}

interface IPagination {
  pageSize: number;
  totalPages: number;
  currentPage: number;
}
interface State<Resource> {
  data: Resource | { [id: string]: Resource } | any;
  pagination: IPagination | null;
  lastError: string | null;
  error: string | null;
  lastFetch: number | null;
  loadingIds: string[];
  updatingIds: string[];
  // The ids of the resources that were last added to the state, in the order they came in.
  // Used to store order information when indexById is true --  see the resource creation options.
  lastFetchOrder?: string[];
}

// @todo -- figure out a way to provide root state definition
// without circular dependencies
type RootState = any;

/**
 * Creates a bundle of actions, selectors, and a reducer to handle
 * common actions and selections for data that needs to be fetched:
 * start, success and error actions, storing and selecting error states,
 * and storing and selecting staleness data, as well as storing the
 * fetched data itself.
 *
 * Consumers can add add their own actions and selectors, and extend
 * the given reducer, to provide additional functionality.
 */
function createAsyncResourceBundle<Resource, Action>(
  // The name of the entity for which this reducer is responsible
  entityName: string,
  options: {
    // The key the reducer provided by this bundle is mounted at.
    // Defaults to entityName if none is given.
    selectLocalState?: (state: RootState) => State<Resource>;
    // Do we index the incoming data by id, or just add it to the state as-is?
    indexById?: boolean;
    // Provides a namespace for the created actions, separated by a slash,
    // e.g.the resource 'books' namespaced with 'shared' becomes SHARED/BOOKS
    namespace?: string;
    // The initial state of the reducer data. Defaults to an empty object.
    initialData?: Resource;
  } = {
    indexById: false
  }
) {
  const { indexById } = options;
  const selectLocalState = options.selectLocalState
    ? options.selectLocalState
    : (state: any): State<Resource> => state[entityName];

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

  const initialState: State<Resource> = {
    data: options.initialData || {},
    pagination: null,
    lastError: null,
    error: null,
    lastFetch: null,
    loadingIds: [],
    updatingIds: []
  };

  const fetchStartAction = (ids?: string[] | string): FetchStartAction => ({
    entity: entityName,
    type: FETCH_START,
    payload: { ids }
  });

  const fetchSuccessAction = (
    data: Resource | Resource[] | any,
    { pagination, order }: { pagination?: IPagination; order?: string[] } = {}
  ): FetchSuccessAction<Resource> => ({
    entity: entityName,
    type: FETCH_SUCCESS,
    payload: { data, pagination, order, time: Date.now() }
  });

  const fetchSuccessIgnoreAction = (
    data: Resource | Resource[] | any
  ): FetchSuccessIgnoreAction<Resource> => ({
    entity: entityName,
    type: FETCH_SUCCESS_IGNORE,
    payload: { data, time: Date.now() }
  });

  const fetchErrorAction = (
    error: string,
    ids?: string | string[]
  ): FetchErrorAction => ({
    entity: entityName,
    type: FETCH_ERROR,
    payload: { error, ids, time: Date.now() }
  });

  const updateStartAction = (data: Resource): UpdateStartAction<Resource> => ({
    entity: entityName,
    type: UPDATE_START,
    payload: { data }
  });

  const updateSuccessAction = (
    id: string,
    data?: Resource
  ): UpdateSuccessAction<Resource> => ({
    entity: entityName,
    type: UPDATE_SUCCESS,
    payload: { id, data, time: Date.now() }
  });

  const updateErrorAction = (error: string, id: string): UpdateErrorAction => ({
    entity: entityName,
    type: UPDATE_ERROR,
    payload: { error, id, time: Date.now() }
  });

  const isAction = (
    action: Actions<Resource> | Action
  ): action is Actions<Resource> => {
    return (action as Actions<Resource>).entity !== undefined;
  };

  return {
    initialState,
    reducer: (
      state: State<Resource> = initialState,
      action: Actions<Resource> | Action
    ): State<Resource> => {
      if (!isAction(action)) {
        return state;
      }

      // The entity property lets us scope by module, whilst keeping
      // the 'type' property typed as string literal unions.
      if (action.entity !== entityName) {
        return state;
      }
      switch (action.type) {
        case FETCH_START: {
          return {
            ...state,
            loadingIds: applyStatusIds(state.loadingIds, action.payload.ids)
          };
        }
        case FETCH_SUCCESS: {
          return {
            ...state,
            data: !indexById
              ? action.payload.data
              : formatIncomingResourceData(
                  state.data,
                  action.payload.data,
                  entityName
                ),
            // Only update pagination if the values have changed. This saves components
            // having to rerender when pagination information hasn't changed.
            pagination: isEqual(state.pagination, action.payload.pagination)
              ? state.pagination
              : action.payload.pagination || null,
            lastFetch: action.payload.time,
            error: null,
            loadingIds: indexById
              ? removeStatusIds(
                  state.loadingIds,
                  getStatusIdsFromData(action.payload.data)
                )
              : [],
            lastFetchOrder: getOrderFromIncomingResourceData(
              action.payload.data,
              entityName,
              state.lastFetchOrder,
              action.payload.order
            )
          };
        }
        case FETCH_SUCCESS_IGNORE: {
          return {
            ...state,
            error: null,
            loadingIds: indexById
              ? removeStatusIds(
                  state.loadingIds,
                  getStatusIdsFromData(action.payload.data)
                )
              : []
          };
        }
        case FETCH_ERROR: {
          if (
            !action.payload ||
            !action.payload.error ||
            !action.payload.time
          ) {
            return state;
          }
          if (!action.payload.error) {
            return state;
          }
          return {
            ...state,
            lastError: action.payload.error,
            error: action.payload.error,
            loadingIds: indexById
              ? removeStatusIds(state.loadingIds, action.payload.ids)
              : []
          };
        }
        case UPDATE_START: {
          return {
            ...state,
            data: !indexById
              ? action.payload.data
              : formatIncomingResourceData(
                  state.data,
                  action.payload.data,
                  entityName
                ),
            updatingIds: applyStatusIds(
              state.updatingIds,
              indexById ? action.payload.data.id : undefined
            )
          };
        }
        case UPDATE_SUCCESS: {
          let data;
          if (action.payload.data) {
            data = !indexById
              ? action.payload.data
              : formatIncomingResourceData(
                  state.data,
                  action.payload.data,
                  entityName
                );
          } else {
            data = state.data; // eslint-disable-line prefer-destructuring
          }
          return {
            ...state,
            data,
            lastFetch: action.payload.time,
            error: null,
            updatingIds: removeStatusIds(state.updatingIds, action.payload.id)
          };
        }
        case UPDATE_ERROR: {
          return {
            ...state,
            error: action.payload.error,
            lastError: action.payload.error,
            updatingIds: removeStatusIds(state.updatingIds, action.payload.id)
          };
        }
        default: {
          return state;
        }
      }
    },
    selectLocalState,
    actionNames: {
      fetchStart: FETCH_START,
      fetchSuccess: FETCH_SUCCESS,
      fetchSuccessIgnore: FETCH_SUCCESS_IGNORE,
      fetchError: FETCH_ERROR,
      updateStart: UPDATE_START,
      updateSuccess: UPDATE_SUCCESS,
      updateError: UPDATE_ERROR
    },
    actions: {
      fetchStart: fetchStartAction,
      fetchSuccess: fetchSuccessAction,
      fetchSuccessIgnore: fetchSuccessIgnoreAction,
      fetchError: fetchErrorAction,
      updateStart: updateStartAction,
      updateSuccess: updateSuccessAction,
      updateError: updateErrorAction
    },
    selectors: {
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
    }
  };
}

export { Actions, State, IPagination, globalLoadingIndicator };
export default createAsyncResourceBundle;
