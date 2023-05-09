import { RuleFormData } from "../RuleForm";

export interface OkIResponse {
    status: 'ok'
    rule: RuleFormData
}

export interface ErrorIResponse {
    status: 'error';
    errorMessage: string;
    statusCode?: number;
}

export const createErrorResponse = (errorMessage: string, statusCode: number): ErrorIResponse => ({
    status: 'error',
    errorMessage,
    statusCode
});

export const createOkResponse = (rule: RuleFormData): OkIResponse => ({
    status: 'ok',
    rule
})

export const responseHandler = async (response: Response) => {
    if (response.ok){
        const message = await response.json()
        return createOkResponse(message)
    } else {
        return createErrorResponse(response.statusText, response.status)
    }
}