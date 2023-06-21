import {DraftRule, DraftRuleFromServer} from "../hooks/useRule";

export interface OkIResponse {
    status: 'ok'
    data: DraftRule
}

export interface ErrorIResponse {
    status: 'error';
    errorMessage: string;
    statusCode?: number;
}

export const transformApiFormData = <Rule extends { tags: string | undefined }>(draftRule: Rule): Omit<Rule, "tags"> & { tags: string[] } => (
    {...draftRule, tags: draftRule.tags ? draftRule.tags.split(",") : []}
)

export const createErrorResponse = (errorMessage: string, statusCode: number): ErrorIResponse => ({
    status: 'error',
    errorMessage,
    statusCode
});

export const createOkResponse = (apiRule: DraftRuleFromServer): OkIResponse => ({
    status: 'ok',
    data: transformApiFormData(apiRule)
})

export const responseHandler = async (response: Response) => {
    if (response.ok){
        const message = await response.json()
        return createOkResponse(message)
    } else {
        return createErrorResponse(response.statusText, response.status)
    }
}

export const transformRuleFormData = (ruleForm: DraftRule): DraftRuleFromServer => {
  return {...ruleForm, tags: ruleForm?.tags?.length ? ruleForm.tags.join(",") : undefined};
}
