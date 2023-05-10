import { responseHandler } from "./parseResponse";

export const getRule = async (ruleId: number) => {
    const getRuleResponse = fetch(`${location}rules/${ruleId}`, {
         method: 'GET',
    }).then(responseHandler);
    return getRuleResponse
 }