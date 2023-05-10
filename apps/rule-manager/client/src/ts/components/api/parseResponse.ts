import { RuleFormData } from "../RuleForm";
import { Rule } from "../RulesTable";
import { FormDataForApiEndpoint } from "./createRule";

export interface OkIResponse {
    status: 'ok'
    rule: RuleFormData
}

export interface ErrorIResponse {
    status: 'error';
    errorMessage: string;
    statusCode?: number;
}

const transformApiFormData = (apiFormData: FormDataForApiEndpoint) => (
    {...apiFormData, tags: apiFormData.tags ? apiFormData.tags.split(",") : []} as RuleFormData
)

export const createErrorResponse = (errorMessage: string, statusCode: number): ErrorIResponse => ({
    status: 'error',
    errorMessage,
    statusCode
});

export const createOkResponse = (apiRule: FormDataForApiEndpoint): OkIResponse => ({
    status: 'ok',
    rule: transformApiFormData(apiRule)
})

export const responseHandler = async (response: Response) => {
    if (response.ok){
        const message = await response.json()
        return createOkResponse(message)
    } else {
        return createErrorResponse(response.statusText, response.status)
    }
}