import { RuleFormData } from "../RuleForm";
import { transformRuleFormData } from "../api/createRule";

export const updateRule = async (ruleForm: RuleFormData) => {
    const formDataForApi = transformRuleFormData(ruleForm);
    // We would always expect the ruleForm to include an ID when updating a rule
    if (!ruleForm.id) return (new Response(null, {status: 400, statusText: "Update endpoint requires a rule ID"}))
    const updateRuleResponse = fetch(`${location}rules/${ruleForm.id}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(ruleForm)
    });
    return updateRuleResponse
}