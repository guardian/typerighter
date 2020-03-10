import { StateType, ActionType } from "typesafe-actions";
import store from "./index";
import rootAction from "./rootAction";
import rootReducer from "./rootReducer";

declare module "AppTypes" {
  export type Store = StateType<typeof store>;
  export type RootAction = ActionType<typeof rootAction>;
  export type RootState = StateType<ReturnType<typeof rootReducer>>;
}

declare module "typesafe-actions" {
  interface Types {
    RootAction: ActionType<typeof rootAction>;
  }
}
