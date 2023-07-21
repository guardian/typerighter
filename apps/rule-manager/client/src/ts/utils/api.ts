import {DraftRule} from "../components/hooks/useRule";

export interface OkIResponse<T>{
    status: 'ok'
    data: T
}

export interface OkIStringResponse {
    status: 'ok'
    data: string
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

export const createOkResponse = <T>(apiRule: T): OkIResponse<T> => ({
    status: 'ok',
    data: apiRule
})

export const responseHandler = async <T>(response: Response) => {
    if (response.ok){
        const message = await response.json()
        return createOkResponse<T>(message)
    } else {
        return createErrorResponse(response.statusText, response.status)
    }
}

export const textResponseHandler = async (response: Response) => {
    if (response.ok){
        const message = await response.text()
        return createOkResponse<string>(message)
    } else {
        return createErrorResponse(response.statusText, response.status)
    }
}

