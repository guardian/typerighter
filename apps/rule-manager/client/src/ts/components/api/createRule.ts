import { responseHandler } from "./parseResponse";
import {DraftRule, DraftRuleFromServer} from "../hooks/useRule";

export const transformRuleFormData = (ruleForm: DraftRule): DraftRuleFromServer => {
    return {...ruleForm, tags: ruleForm?.tags?.length ? ruleForm.tags.join(",") : undefined};
}

export const createRule = async (ruleForm: DraftRule) => {
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
