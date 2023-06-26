import {DraftRule} from "../components/hooks/useRule";
import {IconColor} from "@elastic/eui/src/components/icon";

export type RuleState = "live" | "archived" | "draft" | "error";

export const getRuleState = (rule: DraftRule | undefined): RuleState => {
  if(!rule) {
    return "draft";
  }

  if(rule.isPublished && rule.isArchived) {
    return "error";
  }

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

const stateToColourMap: {[state in RuleState]: IconColor} = {
  error: "danger",
  live: "success",
  archived: "subdued",
  draft: "#DA8B45"
}
