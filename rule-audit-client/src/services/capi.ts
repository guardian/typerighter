import { urls } from "../constants";
import {
  IMatch,
  IBlock,
} from "@guardian/prosemirror-typerighter/dist/src/ts/interfaces/IMatch";

export type CapiResponse<TContent> = {
  status: "ok" | "error";
  total: number;
  startIndex: number;
  pageSize: number;
  currentPage: number;
  pages: number;
  results: TContent[] | undefined;
};

export type CapiContentResponse = CapiResponse<CapiContent>;

export type CapiTagsResponse = CapiResponse<CapiTag>;

export type CapiSectionsResponse = CapiResponse<CapiSection>;

export type CapiContent = {
  id: string;
  type: string;
  sectionId: string;
  sectionName: string;
  webPublicationDate: string;
  webTitle: string;
  webUrl: string;
  apiUrl: string;
  isHosted: string;
  pillarId: string;
  pillarName: string;
  fields: { body: string };
};

export type CapiContentWithMatches = CapiContent & {
  // Added by our application
  meta: {
    blocks: IBlock[];
    matches: IMatch[];
  };
};

export type CapiSection = {
  id: string;
  type: string;
  sectionId: string;
  sectionName: string;
  webTitle: string;
  webUrl: string;
  apiUrl: string;
};

export type CapiTag = {
  id: string;
  type: string;
  sectionId: string;
  sectionName: string;
  webTitle: string;
  webUrl: string;
  apiUrl: string;
};

export const fetchCapiSearch = async (
  query: string,
  tags: string[],
  sections: string[],
  page?: number
): Promise<CapiContentResponse> => {
  const params = new URLSearchParams();
  page && params.append("page", page.toString());
  params.append("query", query);

  // Do not include empty tag or section values.
  tags.filter(_ => _).forEach(_ => params.append("tags", _));
  sections.filter(_ => _).forEach(_ => params.append("sections", _));

  return (
    await fetch(`${urls.capiQuery}/search?${params.toString()}`)
  ).json();
};

export const fetchCapiTags = async (
  query: string
): Promise<CapiTagsResponse> => {
  return (await fetch(`${urls.capiQuery}/tags/${query}`, {})).json();
};

export const fetchCapiSections = async (
  query: string
): Promise<CapiSectionsResponse> => {
  return (await fetch(`${urls.capiQuery}/sections/${query}`)).json();
};
