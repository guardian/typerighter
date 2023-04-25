import { RuleFormData } from "../RuleForm";

type FormDataForApiEndpoint =  {
    ruleType: string,
    pattern?: string,
    replacement?: string,
    category?: string,
    tags: string,
    description?: string,
    ignore: boolean,
    forceRedRule?: boolean,
    advisoryRule?: boolean
}

export const transformRuleFormData = (ruleForm: RuleFormData): FormDataForApiEndpoint => {
    return {...ruleForm, tags: ruleForm.tags.toString()};
}

export const createRule = async (ruleForm: FormDataForApiEndpoint) => {
   const createRuleResponse = fetch(`${location}rules`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(ruleForm)
    });
    return createRuleResponse
}