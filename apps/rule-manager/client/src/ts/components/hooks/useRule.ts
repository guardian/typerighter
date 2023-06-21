import {useEffect, useState} from "react";
import {transformApiFormData} from "../api/parseResponse";
import { errorToString } from "../../utils/error";
import { FormError } from "../RuleForm";

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
  live: LiveRuleFromServer[]
}

export type RuleData = {
  draft: DraftRule
  live: LiveRule[]
}

export function useRule(ruleId: number | undefined) {
  const [isLoading, setIsLoading] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [publishValidationErrors, setPublishValidationErrors] = useState<FormError[] | undefined>(undefined);
  const [errors, setErrors] = useState<string | undefined>(undefined);
  const [rule, setRule] = useState<RuleData | undefined>(undefined);

  const fetchRule = async (ruleId: number) => {
    setIsLoading(true);
    setIsValidating(true); // Mark the rule as pending validation until the server tells us otherwise

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
      setErrors(errorToString(error));
    }

    setIsLoading(false);
  }

  const publishRule = async (ruleId: number) => {
    setIsPublishing(true);

    try {
      const response = await fetch(`${location}rules/${ruleId}/publish`, {
        method: "POST",
        headers: [["Content-Type", "application/json"]],
        body: JSON.stringify({ reason: "Placeholder reason" })
      });
      if (!response.ok) {
        throw new Error(`Failed to publish rules: ${response.status} ${response.statusText}`);
      }

      const rules: RuleDataFromServer = await response.json();
      setRule({
        draft: transformApiFormData(rules.draft),
        live: rules.live.map(transformApiFormData)
      });
    } catch (error) {
      setErrors(errorToString(error));
    } finally {
      setIsPublishing(false);
    }
  }

  const validateRule = async (ruleId: number) => {
    setIsValidating(false);

    try {
      const response = await fetch(`${location}rules/${ruleId}/publish`);
      if (response.status === 400) {
        const validationErrors: FormError[] = await response.json();
        return setPublishValidationErrors(validationErrors);
      }
      setPublishValidationErrors(undefined);
    } catch (error) {
      setErrors(errorToString(error));
    } finally {
      setIsValidating(false)
    }
  }

  const resetPublishValidationErrors = () => setPublishValidationErrors(undefined);

  useEffect(() => {
    if (ruleId) {
      fetchRule(ruleId);
    } else {
      setRule(undefined);
    }
  }, [ruleId])

  return { fetchRule, isLoading, errors, rule, publishRule, isPublishing, validateRule, isValidating, publishValidationErrors, resetPublishValidationErrors }
}
