import { IBlock } from "@guardian/prosemirror-typerighter/src/ts/interfaces/IMatch";
import { convertTyperighterResponse } from "@guardian/prosemirror-typerighter";
import { IMatcherResponse } from "@guardian/prosemirror-typerighter/src/ts/interfaces/IMatch";
import { urls } from "../constants";

export const fetchTyperighterMatches = async (
  articleId: string,
  blocks: IBlock[]
): Promise<IMatcherResponse> => {
  const response = await fetch(urls.matches, {
    method: "POST",
    headers: new Headers({
      "Content-Type": "application/json"
    }),
    body: JSON.stringify({
      requestId: `audit-${articleId}`,
      blocks
    })
  });
  const json = await response.json();
  return convertTyperighterResponse(articleId, json);
};
