import { RuleFormData } from "../RuleForm";

export const createRule = async (ruleForm: RuleFormData) => {
   const createRuleResponse = fetch(`${location}rules`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(ruleForm)
    });
    return createRuleResponse
}