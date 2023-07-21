import { camelCaseToTitleCase } from "./camelCaseToTitleCase";

describe("camelCaseToTitleCase", () => {
    it("should convert a camel case string to a title case string", () => {
        const camelString = "theStringIWantToConvert";
        const expected = "The String I Want To Convert";
        expect(camelCaseToTitleCase(camelString)).toBe(expected);
    })
})