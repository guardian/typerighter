import { createStore } from "../../../store";
import { doSearchMatches } from "../thunks";
import { ThunkDispatch } from "redux-thunk";
import AppTypes from "AppTypes";
import * as selectors from "../selectors";
import {selectors as capiSelectors } from "../../capiContent"
import * as actions from "../actions";
import { createCapiResponse, createTyperighterResponse } from "./fixtures";

const mockCapiService = jest.fn();
const mockTyperighterService = jest.fn();

const emptyTyperighterResponse = createTyperighterResponse(0);
const typerighterResponse = createTyperighterResponse(1);
const capiResponse = createCapiResponse(2);
const secondCapiResponse = createCapiResponse(2, 2);

const mockResponses = (
  capiResponses: typeof capiResponse[],
  typerighterResponses: typeof typerighterResponse[]
) => {
  capiResponses.forEach((response) =>
    mockCapiService.mockReturnValueOnce(Promise.resolve(response))
  );
  typerighterResponses.forEach((response) =>
    mockTyperighterService.mockReturnValueOnce(Promise.resolve(response))
  );
};

describe("doSearchMatches", () => {
  beforeEach(() => {
    mockCapiService.mockReset();
    mockTyperighterService.mockReset();
  });

  it("should ask for articles and their matches, persisting articles that have matches to the state", async () => {
    const store = createStore();
    store.dispatch(actions.doSetSearchMatchesLimit(2));
    mockResponses([capiResponse], [typerighterResponse, typerighterResponse]);

    await (store.dispatch as ThunkDispatch<
      AppTypes.RootState,
      {},
      AppTypes.RootAction
    >)(
      doSearchMatches(
        "query",
        [],
        [],
        mockCapiService,
        mockTyperighterService
      )
    );
    const state = store.getState();

    // We've called CAPI once, and it's returned two articles.
    // We then call Typerighter with those articles, and Typerighter
    // returns matches for both.
    expect(mockCapiService).toBeCalledTimes(1);
    expect(mockTyperighterService).toBeCalledTimes(2);

    capiResponse.results.forEach((article) => {
      const articleInState = capiSelectors.selectById(state, article.id);
      expect(articleInState).toMatchObject(article);
    });

    expect(selectors.selectSearchMatchesArticleIds(state, false)).toEqual(
      capiResponse.results.map(_ => _.id)
    );
  });
  it("should continue to ask for new articles until the given limit is reached", async () => {
    const store = createStore();
    store.dispatch(actions.doSetSearchMatchesLimit(2));
    mockResponses(
      [capiResponse, secondCapiResponse],
      [
        emptyTyperighterResponse,
        emptyTyperighterResponse,
        typerighterResponse,
        typerighterResponse,
      ]
    );

    await (store.dispatch as ThunkDispatch<
      AppTypes.RootState,
      {},
      AppTypes.RootAction
    >)(
      doSearchMatches(
        "query",
        [],
        [],
        mockCapiService,
        mockTyperighterService
      )
    );
    const state = store.getState();

    // We've called CAPI twice, and it's returned four articles.
    // We then call Typerighter with those articles, and Typerighter
    // returns matches for the last two articles.
    expect(mockCapiService).toBeCalledTimes(2);
    expect(mockTyperighterService).toBeCalledTimes(4);

    capiResponse.results.forEach((article) => {
      const articleInState = capiSelectors.selectById(state, article.id);
      expect(articleInState).toMatchObject(article);
    });

    expect(selectors.selectSearchMatchesArticleIds(state, false)).toEqual(
      secondCapiResponse.results.map(_ => _.id)
    );
  });
  it("should stop asking for articles if the cancel action is dispatched", async () => {
    const store = createStore();
    store.dispatch(actions.doSetSearchMatchesLimit(2));

    mockResponses(
      [capiResponse],
      [
        typerighterResponse,
        emptyTyperighterResponse
      ]
    );

   const thunk = (store.dispatch as ThunkDispatch<
      AppTypes.RootState,
      {},
      AppTypes.RootAction
    >)(
      doSearchMatches(
        "query",
        [],
        [],
        mockCapiService,
        mockTyperighterService
      )
    );

    store.dispatch(actions.doSearchMatchesEnd());
    await thunk;

    const state = store.getState();

    // We call CAPI, and it's returned two articles.
    // We then call Typerighter with those articles, and Typerighter
    // returns matches for one. We call CAPI again, but the user
    // cancels the search before the second CAPI request is processed,
    // so nothing else should go to Typerighter.
    expect(mockCapiService).toBeCalledTimes(1);
    expect(mockTyperighterService).toBeCalledTimes(2);

    capiResponse.results.forEach((article) => {
      const articleInState = capiSelectors.selectById(state, article.id);
      expect(articleInState).toMatchObject(article);
    });

    expect(selectors.selectSearchMatchesArticleIds(state, false)).toEqual([capiResponse.results[0].id]);
  });
  it("should stop asking for articles if no more remain", async () => {
    const store = createStore();
    store.dispatch(actions.doSetSearchMatchesLimit(2));

    mockResponses(
      [createCapiResponse(2, 0, 1)],
      [
        emptyTyperighterResponse,
        emptyTyperighterResponse
      ]
    );

   const thunk = (store.dispatch as ThunkDispatch<
      AppTypes.RootState,
      {},
      AppTypes.RootAction
    >)(
      doSearchMatches(
        "query",
        [],
        [],
        mockCapiService,
        mockTyperighterService
      )
    );

    await thunk;

    const state = store.getState();

    // We call CAPI, and it's returned two articles.
    // We then call Typerighter with those articles, and Typerighter
    // returns matches for one. We call CAPI again, but the user
    // cancels the search before the second CAPI request is processed,
    // so nothing else should go to Typerighter.
    expect(mockCapiService).toBeCalledTimes(1);
    expect(mockTyperighterService).toBeCalledTimes(2);

    capiResponse.results.forEach((article) => {
      const articleInState = capiSelectors.selectById(state, article.id);
      expect(articleInState).toMatchObject(article);
    });

    expect(selectors.selectSearchMatchesArticleIds(state, false)).toEqual([]);
  });
  it("should not give the user more matches than they asked for", async () => {
    const store = createStore();
    store.dispatch(actions.doSetSearchMatchesLimit(2));

    mockResponses(
      [createCapiResponse(3)],
      [
        typerighterResponse,
        typerighterResponse,
        typerighterResponse
      ]
    );

   await (store.dispatch as ThunkDispatch<
      AppTypes.RootState,
      {},
      AppTypes.RootAction
    >)(
      doSearchMatches(
        "query",
        [],
        [],
        mockCapiService,
        mockTyperighterService
      )
    );

    const state = store.getState();

    // We call CAPI, and it's returned two articles.
    // We then call Typerighter with those articles, and Typerighter
    // returns matches for one. We call CAPI again, but the user
    // cancels the search before the second CAPI request is processed,
    // so nothing else should go to Typerighter.
    expect(mockCapiService).toBeCalledTimes(1);
    expect(mockTyperighterService).toBeCalledTimes(3);

    capiResponse.results.forEach((article) => {
      const articleInState = capiSelectors.selectById(state, article.id);
      expect(articleInState).toMatchObject(article);
    });

    expect(selectors.selectSearchMatchesArticleIds(state, false)).toEqual(capiResponse.results.slice(0, 2).map(_ => _.id));
  });
});
