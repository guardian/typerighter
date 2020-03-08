import isEqual from "lodash/isEqual";

import createSelectors from "./createSelectors";
import createActions, {
  Actions,
  FETCH_START,
  FETCH_SUCCESS,
  FETCH_SUCCESS_IGNORE,
  FETCH_ERROR,
  UPDATE_START,
  UPDATE_SUCCESS,
  UPDATE_ERROR
} from "./createActions";
import {
  formatIncomingResourceData,
  applyStatusIds,
  removeStatusIds,
  getOrderFromIncomingResourceData,
  getStatusIdsFromData,
  globalLoadingIndicator
} from "./utils";

export interface BaseResource {
  id: string;
}

interface IPagination {
  pageSize: number;
  totalPages: number;
  currentPage: number;
}

export type ResourceWithId<IdProp extends string> = {
  [id in IdProp]: string;
};

export type IndexedResource<
  Resource extends ResourceWithId<IdProp>,
  IdProp extends string
> = { [id: string]: Resource };

export type StateData<
  Resource extends {},
  IdProp extends string
> = Resource extends ResourceWithId<IdProp>
  ? IndexedResource<Resource, IdProp>
  : Resource;

export interface State<Resource, IdProp extends string> {
  // If we have a resource keyed by id, our data is a mapping from id to resource.
  // If not, our data is the entire resource.
  data: StateData<Resource, IdProp>;
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

type RootState<Resource, IdProp extends string, MountPoint extends string> = {
  [mount in MountPoint]: State<Resource, IdProp>;
};

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
const createAsyncResourceBundle = <
  Resource,
  Action,
  // Sadly, this must be specified – Typescript cannot selectively infer type
  // parameters when multiple params are present, it can either infer all or none.
  // See https://github.com/microsoft/TypeScript/issues/26242. When this issue is solved,
  // we can derive a default for the mount point from `entityName`.
  MountPoint extends string,
  IdProp extends string = "id"
>(
  // The name of the entity for which this reducer is responsible
  entityName: string,
  options: {
    // The key the reducer provided by this bundle is mounted at.
    // Defaults to entityName if none is given.
    selectLocalState?: (
      state: RootState<Resource, IdProp, MountPoint>
    ) => State<Resource, IdProp>;
    // Do we index the incoming data by id, or just add it to the state as-is?
    indexById?: boolean;
    // Provides a namespace for the created actions, separated by a slash,
    // e.g.the resource 'books' namespaced with 'shared' becomes SHARED/BOOKS
    namespace?: string;
    // The initial state of the reducer data. Defaults to an empty object.
    initialData?: StateData<Resource, IdProp>;
  } = {
    indexById: false
  }
) => {
  type LocalState = State<Resource, IdProp>;

  const { indexById } = options;
  const selectLocalState = options.selectLocalState
    ? options.selectLocalState
    : (state: { [mount in MountPoint]: LocalState }): LocalState =>
        state[entityName as MountPoint];

  const initialState: LocalState = {
    data: options.initialData || ({} as StateData<Resource, IdProp>),
    pagination: null,
    lastError: null,
    error: null,
    lastFetch: null,
    loadingIds: [],
    updatingIds: []
  };

  const isAction = (
    action: Actions<Resource> | Action
  ): action is Actions<Resource> => {
    return (action as Actions<Resource>).entity !== undefined;
  };

  return {
    initialState,
    reducer: (
      state: LocalState = initialState,
      action: Actions<Resource> | Action
    ): LocalState => {
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
    actions: createActions(entityName),
    selectors: createSelectors<
      Resource,
      IdProp,
      RootState<Resource, IdProp, MountPoint>
    >(selectLocalState)
  };
};

export { IPagination, globalLoadingIndicator };
export default createAsyncResourceBundle;
