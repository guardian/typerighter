import {useEffect, useState} from "react";
import {
  ErrorIResponse,
  responseHandler,
  transformApiFormData,
  transformApiFormDataBatchedit,
  transformRuleFormData
} from "../../utils/api";
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

export function useBatchRules(ruleIds: number[] | undefined) {
  const [isLoading, setIsLoading] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [publishValidationErrors, setPublishValidationErrors] = useState<FormError[] | undefined>(undefined);
  const [errors, setErrors] = useState<string | undefined>(undefined);
  const [rules, setRules] = useState<RuleData[] | undefined>(undefined);

  const fetchRules = async (ruleIds: number[]) => {
    setIsLoading(true);
    setIsValidating(true); // Mark the rule as pending validation until the server tells us otherwise

    try {
      const response = await fetch(`${location}rules/batch/${ruleIds.join(',')}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch rules: ${response.status} ${response.statusText}`);
      }
      const rulesData: RuleData[] = await response.json();
      setRules(rulesData)

    } catch (error) {
      setErrors(errorToString(error));
    }

    setIsLoading(false);
  }

  const updateRules = async (ruleForm: DraftRule[]) => {
    setIsLoading(true);

    const formDataForApi = {
      "ids": ruleForm.map(rule => {
        if (!rule.id) {
          throw new Error("Update endpoint requires a rule ID")
        }
        return rule.id
      }),
      "fields": {
        "category": ruleForm[0].category,
        "tags": ruleForm.map(rule => rule.tags),
      }
    }

    const response = await fetch(`${location}rules/batch`, {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json'
      },
      body: JSON.stringify(formDataForApi)
    })


    const parsedResponse = await responseHandler(response);
    if (parsedResponse.status === "ok") {
      setRules({ ...rules, draft: parsedResponse.data});
    } else {
      setErrors(parsedResponse.errorMessage);
    }

    setIsLoading(false);

    return parsedResponse;
  }
  //
  // const createRule = async (ruleForm: DraftRule) => {
  //   setIsLoading(true);
  //
  //   const transformedRuleFormData = transformRuleFormData(ruleForm);
  //   const createRuleResponse = await fetch(`${location}rules`, {
  //       method: 'POST',
  //       headers: {
  //           'Content-Type': 'application/json'
  //       },
  //       body: JSON.stringify(transformedRuleFormData)
  //   })
  //
  //   const parsedResponse = await responseHandler(createRuleResponse);
  //   if (parsedResponse.status === "ok") {
  //     setRules({ ...rules || { live: [] }, draft: parsedResponse.data });
  //   } else {
  //     setErrors(parsedResponse.errorMessage);
  //   }
  //
  //   setIsLoading(false);
  //
  //   return parsedResponse;
  // }

  useEffect(() => {
    if (ruleIds) {
      fetchRules(ruleIds);
    } else {
      setRules([]);
    }
  }, [ruleIds])

  return { fetchRule: fetchRules, updateRules, /*createRule,*/ isLoading, errors, rules}
}
