import {DraftRule, RuleData} from "../components/hooks/useRule";
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

export const getRuleStateColour = (rule: DraftRule | undefined): IconColor =>
  rule ? stateToColourMap[getRuleState(rule)] : stateToColourMap.draft;

const stateToColourMap: {[state in RuleState]: IconColor} = {
  error: "danger",
  live: "success",
  archived: "danger",
  draft: "#DA8B45"
}

export const hasUnpublishedChanges = (ruleData: RuleData) =>
  ruleData.live.length && !ruleData.live.some(liveRule => liveRule.revisionId === ruleData.draft.revisionId)
