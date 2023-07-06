import {useEffect, useState} from "react";
import { errorToString } from "../../utils/error";
import {DraftRule, RuleData} from "./useRule";
import {ErrorIResponse, responseHandler} from "../../utils/api";

export function useBatchRules(ruleIds: number[] | undefined) {
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<string | undefined>(undefined);
  const [rules, setRules] = useState<RuleData[] | undefined>(undefined);

  const fetchRules = async (ruleIds: number[]) => {
    setIsLoading(true);

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

    const hasCategoryChanged = rules && rules[0].draft.category !== ruleForm[0].category;

    const haveTagsChanged = () => {
        if (!rules) return false;

        const oldTags = rules.flatMap(rule => rule.draft.tags)
        const newTags = ruleForm.flatMap(rule => rule.tags)
        return !(oldTags.length === newTags.length && oldTags.every((tag, index) => tag === newTags[index]))
    }

    const newTags = [...new Set(ruleForm.flatMap(rule => rule.tags))]

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

    const response = await fetch(`${location}rules/batch`, {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json'
      },
      body: JSON.stringify(ruleFormData)
    })

    const parsedResponse = await responseHandler(response);
    if (parsedResponse.status === "ok") {
      if (rules  && parsedResponse.data) {
        const updatedRules = rules.map((rule, index) => {
          const updatedRule = (parsedResponse.data as unknown as DraftRule[]).find(updatedRule => rule.draft.id === updatedRule.id)
          return updatedRule ? {...rule, draft: updatedRule} : rule
        })
        setRules(updatedRules)
      }
    } else {
      setErrors(parsedResponse.errorMessage);
    }

    setIsLoading(false);

    return parsedResponse;
  }

  useEffect(() => {
    if (ruleIds) {
      fetchRules(ruleIds);
    } else {
      setRules([]);
    }
  }, [ruleIds])

  return { isLoading, fetchRules, updateRules, rules}
}
