export const createCapiResponse = (noOfArticles: number, responseNo = 0, pages = 1000) => ({
  status: "ok",
  userTier: "developer",
  total: 2142557,
  startIndex: 1,
  pageSize: 10,
  currentPage: 1,
  pages,
  orderBy: "relevance",
  results: new Array(noOfArticles)
    .fill(undefined)
    .map((_, index) => createCapiArticle(`article-${responseNo}-${index + 1}`)),
});

export const createCapiArticle = (articleId: string) => ({
  id: `commentisfree/2020/may/25/${articleId}`,
  type: "article",
  sectionId: "commentisfree",
  sectionName: "Opinion",
  webPublicationDate: "2020-05-25T17:04:56Z",
  webTitle: "Kadyrov is a despot, not a strongman | Brief letters",
  webUrl:
    "https://www.theguardian.com/commentisfree/2020/may/25/kadyrov-is-a-despot-not-a-strongman",
  apiUrl:
    "https://content.guardianapis.com/commentisfree/2020/may/25/kadyrov-is-a-despot-not-a-strongman",
  fields: {
    body:
      '<p>I encourage the Guardian to drop the description “strongman” for despots like Ramzan Kadyrov, the Chechen leader who rules with fear, torture and murder (<a href="https://www.theguardian.com/world/2020/may/21/head-of-chechen-republic-hospitalised-with-suspected-covid-19" title="">Chechen strongman Ramzan Kadyrov ‘hospitalised with suspected Covid-19’</a>, 21 May). It conflates strength and violence. And it denigrates other men. True strength is found in those who courageously oppose Kadyrov.<br><strong>Tim Nichols</strong><br><em>Hackney, London</em></p>',
  },
  tags: [],
  references: [],
  isHosted: false,
  pillarId: "pillar/opinion",
  pillarName: "Opinion",
});

export const createTyperighterResponse = (noOfMatches = 0) => ({
  type: "CHECK_RESPONSE",
  categoryIds: ["regex"],
  blocks: [
    {
      id: "0-from:1-to:427",
      text: "block-text-placeholder",
      from: 1,
      to: 427,
    },
  ],
  matches: new Array(noOfMatches).fill(undefined).map(_ => ({
    rule: {
      id: "1181",
      category: {
        id: "General Election, 2019",
        name: "General Election, 2019",
        colour: "04d514",
      },
      description: "Placeholder description",
      suggestions: [],
      replacement: { type: "TEXT_SUGGESTION", text: "Placeholder replacement" },
    },
    fromPos: 183,
    toPos: 196,
    matchedText: "Placeholder matchedText",
    message: "Placeholder message",
    shortMessage: "Placeholder shortMessage",
    suggestions: [],
    markAsCorrect: true,
  })),
});
