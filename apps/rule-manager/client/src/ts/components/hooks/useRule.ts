import {useEffect, useState} from "react";
import {transformApiFormData} from "../api/parseResponse";

export type RuleType = 'regex' | 'languageToolXML';

export type BaseRule = {
  ruleType: RuleType,
  pattern?: string,
  replacement?: string,
  category?: string,
  tags: string[],
  description?: string,
  ignore: boolean,
  forceRedRule?: boolean,
  advisoryRule?: boolean,
  revisionId: number,
  createdBy: string,
  createdAt: string,
  updatedBy: string,
  updatedAt: string,
  id?: number,
  isArchived: boolean
}

export type DraftRule = BaseRule
export type DraftRuleFromServer = Omit<BaseRule, "tags"> & {
  tags: string | undefined;
}
export type LiveRule = BaseRule & {
  reason: string
}
export type LiveRuleFromServer = Omit<LiveRule, "tags"> & {
  tags: string | undefined;
}

export type RuleDataFromServer = {
  draft: DraftRuleFromServer
  live: LiveRuleFromServer[] | null
}

export type RuleData = {
  draft: DraftRule
  live: LiveRule[]
}

export function useRule(ruleId: number | undefined) {
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<Error[] | undefined>(undefined);
  const [rule, setRule] = useState<RuleData | undefined>(undefined);

  const fetchRule = async (ruleId: number) => {
    setIsLoading(true);

    try {
      const response = await fetch(`${location}rules/${ruleId}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch rules: ${response.status} ${response.statusText}`);
      }
      const rules: RuleDataFromServer = await response.json();
      setRule({
        draft: transformApiFormData(rules.draft),
        live: rules.live.map(transformApiFormData)
      });
    } catch (error) {
      setErrors(error);
    }

    setIsLoading(false);
  }

  useEffect(() => {
    if (ruleId) {
      fetchRule(ruleId);
    } else {
      setRule(undefined);
    }
  }, [ruleId])

  return {fetchRules: fetchRule, isLoading, errors, rule}
}
