import { RuleFormData } from "../RuleForm";
import { responseHandler } from "./parseResponse";

export type FormDataForApiEndpoint =  {
    ruleType: string,
    pattern?: string,
    replacement?: string,
    category?: string,
    tags?: string,
    description?: string,
    ignore: boolean,
    forceRedRule?: boolean,
    advisoryRule?: boolean
}

export const transformRuleFormData = (ruleForm: RuleFormData): FormDataForApiEndpoint => {
    return {...ruleForm, tags: ruleForm.tags && ruleForm.tags.length ? ruleForm.tags.join(",") : undefined};
}

export const createRule = async (ruleForm: RuleFormData) => {
    const transformedRuleFormData = transformRuleFormData(ruleForm);
    const createRuleResponse = fetch(`${location}rules`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(transformedRuleFormData)
    }).then(responseHandler);
    return createRuleResponse
}