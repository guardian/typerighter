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
import {getRuleState, RuleState} from "../../utils/rule";
import {update} from "lodash";

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
    console.log('rules inside updateRules', rules)
    console.log('ruleForm inside updateRules', ruleForm)

    const hasCategoryChanged = rules && rules[0].draft.category !== ruleForm[0].category;

    const haveTagsChanged = () => {
        if (!rules) return false;
        const oldTags = rules.map(rule => rule.draft.tags).flat()
        const newTags = ruleForm.map(rule => rule.tags).flat()
        return !(oldTags.length === newTags.length && oldTags.every((tag, index) => tag === newTags[index]))
    }

    const newTags = [...new Set(ruleForm.map(rule => rule.tags).flat())]

    if (!ruleForm.some(rule => rule.id)) return {status: 'error', errorMessage: "Update endpoint requires a rule ID"} as ErrorIResponse;

    const ruleFormData = {
      "ids": ruleForm.map(rule => {
        if (!rule.id) {
          throw new Error("Update endpoint requires a rule ID")
        }
        return rule.id
      }),
      "fields": {
        ...(hasCategoryChanged ? { "category": ruleForm[0].category } : {}),
        ...(haveTagsChanged() ? { "tags": newTags } : {})
      }
    }

    console.log('formDataForApi', ruleFormData)

    const response = await fetch(`${location}rules/batch`, {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json'
      },
      body: JSON.stringify(ruleFormData)
    })

    const parsedResponse = await responseHandler(response);
    if (parsedResponse.status === "ok") {
      if (rules) {
        rules.map(rule => {
          if (rule.draft.id === parsedResponse.data.id) {
            return setRules({...rules, draft: parsedResponse.data});
          }
          return rule;
        })
      }
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
