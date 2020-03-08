import { IPagination } from "./";

export const FETCH_START = "FETCH_START";
export const FETCH_SUCCESS = "FETCH_SUCCESS";
export const FETCH_SUCCESS_IGNORE = "FETCH_SUCCESS_IGNORE"; // clears loading ids for unchanged collections during collection polling
export const FETCH_ERROR = "FETCH_ERROR";
export const UPDATE_START = "UPDATE_START";
export const UPDATE_SUCCESS = "UPDATE_SUCCESS";
export const UPDATE_ERROR = "UPDATE_ERROR";

interface FetchStartAction {
  entity: string;
  type: "FETCH_START";
  payload: { ids?: string[] | string };
}

interface FetchSuccessAction<Resource> {
  entity: string;
  type: "FETCH_SUCCESS";
  payload: {
    data: Resource | Resource[] | any;
    order?: string[];
    pagination?: IPagination;
    time: number;
  };
}

interface FetchSuccessIgnoreAction<Resource> {
  entity: string;
  type: "FETCH_SUCCESS_IGNORE";
  payload: {
    data: Resource | Resource[] | any;
    time: number;
  };
}

interface FetchErrorAction {
  entity: string;
  type: "FETCH_ERROR";
  payload: {
    error: string;
    time: number;
    ids?: string | string[];
  };
}

interface UpdateStartAction<Resource> {
  entity: string;
  type: "UPDATE_START";
  payload: { id?: string | string; data: Resource | any };
}

interface UpdateSuccessAction<Resource> {
  entity: string;
  type: "UPDATE_SUCCESS";
  payload: { data: Resource | any; id: string; time: number };
}

interface UpdateErrorAction {
  entity: string;
  type: "UPDATE_ERROR";
  payload: {
    error: string;
    id: string;
    time: number;
  };
}

export type Actions<Resource> =
  | FetchStartAction
  | FetchSuccessAction<Resource>
  | FetchSuccessIgnoreAction<Resource>
  | FetchErrorAction
  | UpdateStartAction<Resource>
  | UpdateSuccessAction<Resource>
  | UpdateErrorAction;

const createActions = <Resource>(entityName: string) => {
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

  return {
    fetchStart: fetchStartAction,
    fetchSuccess: fetchSuccessAction,
    fetchSuccessIgnore: fetchSuccessIgnoreAction,
    fetchError: fetchErrorAction,
    updateStart: updateStartAction,
    updateSuccess: updateSuccessAction,
    updateError: updateErrorAction
  };
};

export default createActions;
