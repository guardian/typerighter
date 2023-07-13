import {DraftRule} from "../components/hooks/useRule";

export interface OkIResponse {
    status: 'ok'
    data: DraftRule
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

export const createOkResponse = (apiRule: DraftRule): OkIResponse => ({
    status: 'ok',
    data: apiRule
})

export const responseHandler = async (response: Response) => {
    if (response.ok){
        const message = await response.json()
        return createOkResponse(message)
    } else {
        return createErrorResponse(response.statusText, response.status)
    }
}

