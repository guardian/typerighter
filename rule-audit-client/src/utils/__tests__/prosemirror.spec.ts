import { getBlocksFromHtmlString } from "../prosemirror";

describe("Prosemirror utils", () => {
  describe("getBlocksFromHtmlString", () => {
    it("should get blocks from a html string", () => {
      const exampleString =
        "<p>An example string with nested<ul><li>elements</li></ul></p>";
      const blocks = getBlocksFromHtmlString(exampleString);
      expect(blocks).toEqual([
        {
          from: 1,
          id: "id-from:1-to:31",
          text: "An example string with nested",
          to: 31
        },
        { from: 34, id: "id-from:34-to:43", text: "elements", to: 43 },
        { from: 46, id: "id-from:46-to:47", text: "", to: 47 }
      ]);
    });
  });
});
