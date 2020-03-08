import without from "lodash/without";
import isEqual from "lodash/isEqual";

import { BaseResource } from ".";

export const defaultArray = [] as string[];
export const globalLoadingIndicator = "@@ALL";

export const getStatusIdsFromData = (
  data: BaseResource | BaseResource[] | any
): string[] | string =>
  data instanceof Array
    ? data.map((resource: BaseResource) => resource.id || "")
    : data.id || "";

export const applyStatusIds = (
  currentIds: string[],
  incomingIds?: string | string[]
) => currentIds.concat(incomingIds || globalLoadingIndicator);

export const removeStatusIds = (
  currentIds: string[],
  incomingIds: string[] | string = ""
): string[] =>
  incomingIds instanceof Array
    ? without(currentIds, ...incomingIds, globalLoadingIndicator)
    : without<string>(currentIds, incomingIds);

export function formatIncomingResourceData<Resource extends BaseResource>(
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
      ).join(", ")} is missing id.`
    );
  }

  return {
    ...data,
    [newData.id]: newData
  };
}

export function getOrderFromIncomingResourceData<Resource extends BaseResource>(
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
