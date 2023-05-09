import { RuleFormData } from "../RuleForm";
import { transformRuleFormData } from "../api/createRule";
import { ErrorIResponse, OkIResponse, responseHandler } from "./parseResponse";

export const updateRule = async (ruleForm: RuleFormData): Promise<ErrorIResponse | OkIResponse> => {
    const formDataForApi = transformRuleFormData(ruleForm);
    // We would always expect the ruleForm to include an ID when updating a rule
    if (!ruleForm.id) return ({status: 'error', errorMessage: "Update endpoint requires a rule ID"})
    const updateRuleResponse = fetch(`${location}rules/${ruleForm.id}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(ruleForm)
    }).then(response => responseHandler(response))
    return updateRuleResponse
}