import { actions as capiActions } from "../modules/capiContent";
import { actions as uiActions } from "../modules/ui";
import { actions as searchMatchesActions } from "../modules/searchMatches";

export default {
    capi: capiActions,
    ui: uiActions,
    searchMatches: searchMatchesActions
}
