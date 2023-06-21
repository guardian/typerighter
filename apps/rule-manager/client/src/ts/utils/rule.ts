import {DraftRule} from "../components/hooks/useRule";
import {IconColor} from "@elastic/eui/src/components/icon";

type RuleState = "live" | "archived" | "draft";

export const getRuleState = (rule: DraftRule): RuleState => {
  if(rule.isPublished) {
    return "live";
  }

  if(rule.isArchived) {
    return "archived";
  }

  return "draft"
}

export const getRuleStateColour = (rule: DraftRule): IconColor =>
  stateToColourMap[getRuleState(rule)];

const stateToColourMap: {[state: RuleState]: IconColor} = {
  live: "success",
  archived: "danger",
  draft: "#DA8B45"
}
