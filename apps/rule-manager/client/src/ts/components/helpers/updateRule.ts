import { FormDataForApiEndpoint } from "./createRule";

export const updateRule = async (ruleForm: FormDataForApiEndpoint) => {
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