import { IBlock } from "@guardian/prosemirror-typerighter/dist/interfaces/IMatch";
import { urls } from "../constants";

export const fetchTyperighterMatches = (articleId: string, blocks: IBlock[]) =>
  fetch(urls.matches, {
    method: "POST",
    headers: new Headers({
      "Content-Type": "application/json"
    }),
    body: JSON.stringify({
      requestId: `audit-${articleId}`,
      blocks
    })
  }).then(_ => _.json());
