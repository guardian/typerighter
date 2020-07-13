import { createAsyncResourceBundle } from "redux-bundle-creator";

import { CapiContentWithMatches } from "services/capi";
import * as _selectors from './selectors';

export * as thunks from './thunks';

export const { actions, reducer, ...bundle } = createAsyncResourceBundle<CapiContentWithMatches, {}, "capi">(
  "capi",
  {
    indexById: true,
  }
);

export const selectors = { ...bundle.selectors, ..._selectors };
