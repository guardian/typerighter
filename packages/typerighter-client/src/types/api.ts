export type BaseRule = (IRegexRule | ILTRule)

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

export interface ICategory {
  id: string
  name: string
  colour: string
}

export interface ILTRule {
  id: string
  category: ICategory
  languageShortcode?: string
  patternTokens?: IPatternToken[]
  pattern?: Pattern
  description: string
  message: string
  url?: string
  suggestions: ITextSuggestion[]
}

export interface IPatternToken {
  token: string
  caseSensitive: boolean
  regexp: boolean
  inflected: boolean
}

export interface IRegexRule {
  id: string
  category: ICategory
  description: string
  suggestions: ITextSuggestion[]
  replacement?: ITextSuggestion
  regex: string
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

export type Pattern = string

export type Suggestion = ITextSuggestion
