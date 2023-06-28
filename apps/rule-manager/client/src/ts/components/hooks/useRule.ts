import {useEffect, useState} from "react";
import {ErrorIResponse, responseHandler, transformApiFormData, transformRuleFormData} from "../../utils/api";
import { errorToString } from "../../utils/error";
import { FormError } from "../RuleForm";
import {getRuleState, RuleState} from "../../utils/rule";

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
  isArchived: boolean,
  isPublished: boolean
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
  const [ruleState, setRuleState] = useState<RuleState>("draft")

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

  const publishRule = async (ruleId: number, reason: string) => {
    setIsPublishing(true);

    try {
      const response = await fetch(`${location}rules/${ruleId}/publish`, {
        method: "POST",
        headers: [["Content-Type", "application/json"]],
        body: JSON.stringify({ reason })
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

  const archiveRule = async (ruleId: number) => {
    setIsLoading(true);

    try {
      const response = await fetch(`${location}rules/${ruleId}/archive`, {
        method: 'POST',
      });

      if (!response.ok) {
        throw new Error(`Failed to archive rule: ${response.status} ${response.statusText}`);
      }

      const rules: RuleDataFromServer = await response.json();
      setRule({
        draft: transformApiFormData(rules.draft),
        live: rules.live.map(transformApiFormData)
      });
    } catch (error) {
      setErrors(errorToString(error));
    } finally {
      setIsLoading(false);
    }
  }

  const unarchiveRule = async (ruleId: number) => {
    setIsLoading(true);

    try {
      const response = await fetch(`${location}rules/${ruleId}/unarchive`, {
        method: 'POST',
      });

      if (!response.ok) {
        throw new Error(`Failed to unarchive rule: ${response.status} ${response.statusText}`);
      }

      const rules: RuleDataFromServer = await response.json();
      setRule({
        draft: transformApiFormData(rules.draft),
        live: rules.live.map(transformApiFormData)
      });
    } catch (error) {
      setErrors(errorToString(error));
    } finally {
      setIsLoading(false);
    }
  }

  const unpublishRule = async (ruleId: number) => {
    setIsLoading(true);

    try {
      const response = await fetch(`${location}rules/${ruleId}/unpublish`, {
        method: 'POST',
      });

      if (!response.ok) {
        throw new Error(`Failed to unpublish rule: ${response.status} ${response.statusText}`);
      }

      const rules: RuleDataFromServer = await response.json();
      setRule({
        draft: transformApiFormData(rules.draft),
        live: rules.live.map(transformApiFormData)
      });
    } catch (error) {
      setErrors(errorToString(error));
    } finally {
      setIsLoading(false);
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

  const updateRule = async (ruleForm: DraftRule) => {
    setIsLoading(true);

    const formDataForApi = transformRuleFormData(ruleForm);
    // We would always expect the ruleForm to include an ID when updating a rule
    if (!ruleForm.id) return {status: 'error', errorMessage: "Update endpoint requires a rule ID"} as ErrorIResponse;

    const response = await fetch(`${location}rules/${ruleForm.id}`, {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json'
      },
      body: JSON.stringify(formDataForApi)
    })


    const parsedResponse = await responseHandler(response);
    if (parsedResponse.status === "ok") {
      setRule({ ...rule || { live: [] }, draft: parsedResponse.data });
    } else {
      setErrors(parsedResponse.errorMessage);
    }

    setIsLoading(false);

    return parsedResponse;
  }

  const createRule = async (ruleForm: DraftRule) => {
    setIsLoading(true);

    const transformedRuleFormData = transformRuleFormData(ruleForm);
    const createRuleResponse = await fetch(`${location}rules`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(transformedRuleFormData)
    })

    const parsedResponse = await responseHandler(createRuleResponse);
    if (parsedResponse.status === "ok") {
      setRule({ ...rule || { live: [] }, draft: parsedResponse.data });
    } else {
      setErrors(parsedResponse.errorMessage);
    }

    setIsLoading(false);

    return parsedResponse;
  }

  useEffect(() => {
    if (ruleId) {
      fetchRule(ruleId);
    } else {
      setRule(undefined);
    }
  }, [ruleId])

  useEffect(() => {
    setRuleState(getRuleState(rule?.draft));
  }, [rule])

  return { fetchRule, updateRule, createRule, isLoading, errors, rule, publishRule, isPublishing, validateRule, isValidating, publishValidationErrors, resetPublishValidationErrors, archiveRule, unarchiveRule, unpublishRule, ruleState }
}
