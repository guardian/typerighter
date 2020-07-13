import { StateType, ActionType, Action } from "typesafe-actions";
import { createStore } from "./index";
import rootAction from "./rootAction";
import rootReducer from "./rootReducer";
import { ThunkAction, ThunkDispatch } from "redux-thunk";

declare module "AppTypes" {
  export type Store = StateType<ReturnType<typeof createStore>>;
  export type RootAction = ActionType<typeof rootAction>;
  export type RootState = ReturnType<typeof rootReducer>;
  export type Thunk<ReturnType = void> = ThunkAction<
    ReturnType,
    RootState,
    unknown,
    Action<string>
  >;
  export type Dispatch = ThunkDispatch<RootState, {}, RootAction>;
}

declare module "typesafe-actions" {
  interface Types {
    RootAction: ActionType<typeof rootAction>;
  }
}
