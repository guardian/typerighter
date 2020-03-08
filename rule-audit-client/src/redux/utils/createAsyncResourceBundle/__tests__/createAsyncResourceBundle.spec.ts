import createAsyncResourceBundle, {
  globalLoadingIndicator,
  State,
  StateData
} from "../";

type Book = { id: string };
type Actions = unknown;

const { actions, reducer, selectors, initialState } = createAsyncResourceBundle<
  Book,
  Actions,
  "books"
>("books");

const createState = <Resource extends {}>(
  state: Partial<State<Resource, "id">>
): { books: State<Resource, "id"> } => ({
  books: {
    data: {} as StateData<Resource, "id">,
    pagination: null,
    lastError: null,
    error: null,
    lastFetch: null,
    loadingIds: [],
    updatingIds: [],
    ...state
  }
});

describe("createAsyncResourceBundle", () => {
  const { now } = Date;

  beforeAll(() => {
    (Date as any).now = jest.fn(() => 1337);
  });

  afterAll(() => {
    (Date as any).now = now;
  });
  describe("actionNames", () => {
    it("should provide action names for a given resource name, in upper snake case", () => {
      const { actionNames } = createAsyncResourceBundle<Book, Actions, "books">(
        "books"
      );
      expect(actionNames).toEqual({
        fetchStart: "FETCH_START",
        fetchSuccess: "FETCH_SUCCESS",
        fetchSuccessIgnore: "FETCH_SUCCESS_IGNORE",
        fetchError: "FETCH_ERROR",
        updateStart: "UPDATE_START",
        updateSuccess: "UPDATE_SUCCESS",
        updateError: "UPDATE_ERROR"
      });
    });
  });

  describe("actions", () => {
    it("should provide actions for a given data type", () => {
      expect(actions.fetchStart()).toEqual({
        entity: "books",
        type: "FETCH_START",
        payload: { ids: undefined }
      });
      expect(actions.fetchStart(["bookId"])).toEqual({
        entity: "books",
        type: "FETCH_START",
        payload: { ids: ["bookId"] }
      });
      expect(actions.fetchSuccess({ data: "exampleData" })).toEqual({
        entity: "books",
        type: "FETCH_SUCCESS",
        payload: {
          data: { data: "exampleData" },
          pagination: undefined,
          time: 1337
        }
      });
      expect(actions.fetchSuccessIgnore({ data: "exampleData" })).toEqual({
        entity: "books",
        type: "FETCH_SUCCESS_IGNORE",
        payload: { data: { data: "exampleData" }, time: 1337 }
      });
      expect(actions.fetchError("Something went wrong")).toEqual({
        entity: "books",
        type: "FETCH_ERROR",
        payload: { error: "Something went wrong", time: 1337 }
      });
    });
  });

  describe("selectors", () => {
    it("should provide a selector to get the loading state", () => {
      expect(selectors.selectIsLoading({ books: initialState })).toBe(false);
      expect(
        selectors.selectIsLoading({
          books: { ...initialState, loadingIds: ["@@ALL"] }
        })
      ).toBe(true);
    });
    it("should provide a selector to get the loading state for specific IDs", () => {
      const bundle = createAsyncResourceBundle<Book, Actions, "books">(
        "books",
        {
          indexById: true
        }
      );
      const state = {
        books: { ...bundle.initialState, loadingIds: ["1", "2"] }
      };
      expect(bundle.selectors.selectIsLoadingById(state, "1")).toBe(true);
      expect(bundle.selectors.selectIsLoadingById(state, "2")).toBe(true);
      expect(bundle.selectors.selectIsLoadingById(state, "3")).toBe(false);
    });
    it("should accept a state selector to allow selectors to work on non-standard mount points", () => {
      const bundle = createAsyncResourceBundle<Book, Actions, "otherBooks">(
        "books",
        {
          selectLocalState: (state: any) => state.otherBooks
        }
      );
      expect(
        bundle.selectors.selectIsLoading({
          otherBooks: { ...bundle.initialState, loadingIds: ["@@ALL"] }
        })
      ).toBe(true);
    });
    it("should provide a selector to get the current error", () => {
      expect(selectors.selectCurrentError({ books: initialState })).toBe(null);
      expect(
        selectors.selectCurrentError({
          books: { ...initialState, error: "Something went wrong" }
        })
      ).toBe("Something went wrong");
    });
    it("should provide a selector to get the last error", () => {
      expect(selectors.selectLastError({ books: initialState })).toBe(null);
      expect(
        selectors.selectLastError({
          books: { ...initialState, lastError: "Something went wrong" }
        })
      ).toBe("Something went wrong");
    });
    it("should provide a selector to get the last fetch time", () => {
      expect(selectors.selectLastFetch({ books: initialState })).toBe(null);
      expect(
        selectors.selectLastFetch({
          books: { ...initialState, lastFetch: 1337 }
        })
      ).toBe(1337);
    });
    it("should provide a selector to get all of the stored data", () => {
      expect(selectors.selectAll({ books: initialState })).toEqual(
        initialState.data
      );
    });
    it("should provide a selector to select data by id, if indexById is true", () => {
      const bundle = createAsyncResourceBundle<Book, Actions, "books">(
        "books",
        {
          indexById: true
        }
      );
      const state = createState({
        data: { "1": { id: "1" }, "2": { id: "2" } }
      });
      expect(bundle.selectors.selectById(state, "1")).toEqual({
        id: "1"
      });
      expect(bundle.selectors.selectById(state, "2")).toEqual({
        id: "2"
      });
    });
    it("should provide a selector to select whether a resource is loading for the first time", () => {
      const bundle = createAsyncResourceBundle<Book, Actions, "books">(
        "books",
        {
          indexById: true
        }
      );
      const state = createState({
        data: { "1": { id: "1" } },
        loadingIds: ["1", "2"]
      });
      expect(bundle.selectors.selectIsLoadingInitialDataById(state, "1")).toBe(
        false
      );
      expect(bundle.selectors.selectIsLoadingInitialDataById(state, "2")).toBe(
        true
      );
    });
    it("should provide a selector to select the current resource order", () => {
      const bundle = createAsyncResourceBundle<Book, Actions, "books">(
        "books",
        {
          indexById: true
        }
      );
      const state = createState({
        data: { "1": { id: "1" }, "2": { id: "2" }, "3": { id: "3" } },
        lastFetchOrder: ["1", "2", "3"]
      });
      expect(bundle.selectors.selectLastFetchOrder(state)).toEqual([
        "1",
        "2",
        "3"
      ]);
    });
  });

  describe("Reducer", () => {
    describe("Fetch action handlers", () => {
      describe("Start action handler", () => {
        it("should mark the state as loading when a start action is dispatched", () => {
          const newState = reducer(initialState, actions.fetchStart());
          expect(newState.loadingIds).toEqual(["@@ALL"]);
        });
        it("should add loading keys by uuid as strings", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const newState = bundle.reducer(
            initialState,
            actions.fetchStart("uuid")
          );
          expect(newState.loadingIds).toEqual(["uuid"]);
        });
        it("should add loading keys by uuid as arrays", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const newState = bundle.reducer(
            initialState,
            actions.fetchStart(["uuid", "uuid2"])
          );
          expect(newState.loadingIds).toEqual(["uuid", "uuid2"]);
        });
      });
      describe("Success action handler", () => {
        it("should merge data and mark the state as not loading when a success action is dispatched", () => {
          const newState = reducer(
            { ...initialState, loadingIds: ["@@ALL@@"] },
            actions.fetchSuccess({ uuid: { id: "uuid", author: "Mark Twain" } })
          );
          expect(newState.loadingIds).toEqual([]);
          expect(newState.data).toEqual({
            uuid: { id: "uuid", author: "Mark Twain" }
          });
        });
        it("should merge data by id if indexById is true", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const newState = bundle.reducer(
            { ...initialState, loadingIds: ["uuid"] },
            bundle.actions.fetchSuccess({ id: "uuid", author: "Mark Twain" })
          );
          expect(newState.loadingIds).toEqual([]);
          expect(newState.data).toEqual({
            uuid: { id: "uuid", author: "Mark Twain" }
          });
        });
        it("should merge arrays, too", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const newState = bundle.reducer(
            { ...initialState },
            bundle.actions.fetchSuccess([
              { id: "uuid", author: "Mark Twain" },
              { id: "uuid2", author: "Elizabeth Gaskell" }
            ])
          );
          expect(newState.loadingIds).toEqual([]);
          expect(newState.data).toEqual({
            uuid: { id: "uuid", author: "Mark Twain" },
            uuid2: { id: "uuid2", author: "Elizabeth Gaskell" }
          });
        });
        it("should remove global loading indicators when merging arrays", () => {
          const { reducer: indexedReducer } = createAsyncResourceBundle<
            Book,
            Actions,
            "books"
          >("books", {
            indexById: true
          });
          const newState = indexedReducer(
            { ...initialState, loadingIds: [globalLoadingIndicator] },
            actions.fetchSuccess([{ id: "uuid", author: "Mark Twain" }])
          );
          expect(newState.loadingIds).toEqual([]);
          expect(newState.data).toEqual({
            uuid: { id: "uuid", author: "Mark Twain" }
          });
        });
        it("should keep order information when merging arrays", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const newState = bundle.reducer(
            { ...initialState },
            bundle.actions.fetchSuccess([
              { id: "uuid", author: "Mark Twain" },
              { id: "uuid2", author: "Elizabeth Gaskell" }
            ])
          );
          expect(newState.lastFetchOrder).toEqual(["uuid", "uuid2"]);
        });
        it("should use a custom order if provided", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const newState = bundle.reducer(
            { ...initialState },
            bundle.actions.fetchSuccess(
              [
                { id: "uuid", author: "Mark Twain" },
                { id: "uuid2", author: "Elizabeth Gaskell" }
              ],
              { order: ["uuid2", "uuid"] }
            )
          );
          expect(newState.lastFetchOrder).toEqual(["uuid2", "uuid"]);
        });
        it("should not update the order reference if the order is the same by value comparison", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const firstState = bundle.reducer(
            { ...initialState },
            bundle.actions.fetchSuccess(
              [{ id: "uuid", author: "Mark Twain" }],
              { order: ["uuid2", "uuid"] }
            )
          );
          const secondState = bundle.reducer(
            firstState,
            bundle.actions.fetchSuccess(
              [{ id: "uuid", author: "Mark Twain" }],
              { order: ["uuid2", "uuid"] }
            )
          );
          expect(firstState.lastFetchOrder).toBe(secondState.lastFetchOrder);
        });
        it("should return undefined pagination object when no pagination data in response", () => {
          const newState = reducer(
            initialState,
            actions.fetchSuccess({ uuid: { id: "uuid", author: "Mark Twain" } })
          );
          expect(newState.pagination).toEqual(null);
        });
        it("should return pagination data when supplied by action", () => {
          const newState = reducer(
            initialState,
            actions.fetchSuccess(
              { uuid: { id: "uuid", author: "Mark Twain" } },
              { pagination: { pageSize: 20, currentPage: 1, totalPages: 20 } }
            )
          );
          expect(newState.pagination).toEqual({
            pageSize: 20,
            currentPage: 1,
            totalPages: 20
          });
        });
        it("should not replace identical pagination data", () => {
          const firstState = reducer(
            initialState,
            actions.fetchSuccess(
              { uuid: { id: "uuid", author: "Mark Twain" } },
              { pagination: { pageSize: 20, currentPage: 1, totalPages: 20 } }
            )
          );
          const secondState = reducer(
            firstState,
            actions.fetchSuccess(
              { uuid: { id: "uuid", author: "Mark Twain" } },
              { pagination: { pageSize: 20, currentPage: 1, totalPages: 20 } }
            )
          );
          expect(firstState.pagination).toEqual(secondState.pagination);
        });
      });
      describe("Success Ignore action handler", () => {
        const newState = reducer(
          { ...initialState, loadingIds: ["uuid"] },
          actions.fetchSuccessIgnore({
            uuid: { id: "uuid", author: "Mark Twain" }
          })
        );
        it("should return initial state data when a successIgnore action is dispatched", () => {
          expect(newState.data).toEqual(initialState.data);
        });
        it("should clear ID data from loadingIds when a successIgnore action is dispatched", () => {
          expect(newState.loadingIds).toEqual([]);
        });
      });
      describe("Error action handler", () => {
        it("should add an error and mark the state as not loading when an error action is dispatched", () => {
          const newState = reducer(
            { ...initialState, data: {}, loadingIds: ["uuid"] },
            actions.fetchError("uuid")
          );
          expect(newState.loadingIds).toEqual([]);
          expect(newState.data).toEqual({});
        });
        it("should handle strings and arrays of strings for loading uuids", () => {
          const newState = reducer(
            { ...initialState, data: {}, loadingIds: ["uuid", "uuid2"] },
            actions.fetchError("Error", ["uuid", "uuid2"])
          );
          expect(newState.loadingIds).toEqual([]);
          expect(newState.data).toEqual({});
        });
      });
    });
    describe("Update action handlers", () => {
      describe("Update start", () => {
        it("should mark the state as updating when a start action is dispatched", () => {
          const newState = reducer(initialState, actions.updateStart({}));
          expect(newState.updatingIds).toEqual(["@@ALL"]);
        });
        it("should add updating keys by uuid as strings", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const newState = bundle.reducer(
            initialState,
            actions.updateStart({
              id: "uuid"
            })
          );
          expect(newState.updatingIds).toEqual(["uuid"]);
        });
        it("should add the incoming updated model to the state", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const newState = bundle.reducer(
            initialState,
            actions.updateStart({ id: "uuid" })
          );
          expect(newState.data.uuid).toEqual({ id: "uuid" });
        });
      });
      describe("Update success", () => {
        it("should remove the updating id from the state", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const state = bundle.reducer(
            initialState,
            actions.updateStart({ id: "uuid" })
          );
          const newState = bundle.reducer(
            state,
            actions.updateSuccess("uuid", { id: "uuid" })
          );
          expect(newState.data.uuid).toEqual({ id: "uuid" });
          expect(newState.updatingIds).toEqual([]);
        });
        it("should replace the model data if data is supplied", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const state = bundle.reducer(
            initialState,
            actions.updateStart({ id: "uuid" })
          );
          const newState = bundle.reducer(
            state,
            actions.updateSuccess("uuid", {
              id: "uuid",
              lastModified: 123456789
            })
          );
          expect(newState.data.uuid).toEqual({
            id: "uuid",
            lastModified: 123456789
          });
          expect(newState.updatingIds).toEqual([]);
        });
        it("should remove the error message if it exists", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const newState = bundle.reducer(
            {
              ...initialState,
              error: "There was a problem",
              lastError: "There was a problem"
            },
            actions.updateSuccess("uuid", {
              id: "uuid",
              lastModified: 123456789
            })
          );
          expect(newState.error).toEqual(null);
          expect(newState.lastError).toEqual("There was a problem");
        });
      });
      describe("Update error", () => {
        it("should remove the updating id from the state, and add an error message", () => {
          const bundle = createAsyncResourceBundle<Book, Actions, "books">(
            "books",
            {
              indexById: true
            }
          );
          const state = bundle.reducer(
            initialState,
            actions.updateStart({ id: "uuid" })
          );
          const newState = bundle.reducer(
            state,
            actions.updateError("There was a problem", "uuid")
          );
          expect(newState.data.uuid).toEqual({ id: "uuid" });
          expect(newState.updatingIds).toEqual([]);
          expect(newState.error).toEqual("There was a problem");
          expect(newState.lastError).toEqual("There was a problem");
        });
      });
    });
  });
});
