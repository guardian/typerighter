import { responseHandler } from "./parseResponse";

export const archiveRule = async (ruleId: number) => (
    fetch(`${location}rules/${ruleId}/archive`, {
        method: 'POST',
    }).then(responseHandler)
);