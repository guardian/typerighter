import {
  createStore as _createStore,
  applyMiddleware,
  compose,
  CombinedState,
} from "redux";
import thunk from "redux-thunk";

import rootReducer from "./rootReducer";

const initialState = {};

const composeEnhancers =
  (window as any).__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose;

export const createStore = () =>
  _createStore(
    rootReducer,
    initialState,
    composeEnhancers(applyMiddleware(thunk))
  );
