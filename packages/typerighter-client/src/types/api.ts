export interface ApiResponse {
  foo: any
}

export type BaseRule = (ApiResponse | ApiResponse)

export interface IApiRequest {
  documentId?: string
  requestId: string
  categoryIds?: string[]
  blocks: ITextBlock[]
}

export interface IApiResponse {
  blocks: ITextBlock[]
  categoryIds: string[]
  matches: IRuleMatch[]
}

export interface IRuleMatch {
  rule: BaseRule
  fromPos: number
  toPos: number
  matchedText: string
  message: string
  shortMessage?: string
  suggestions: Suggestion[]
  markAsCorrect: boolean
}

export interface ITextBlock {
  id: string
  text: string
  from: number
  to: number
}

export interface ITextSuggestion {
  text: string
}

export type Suggestion = ITextSuggestion
