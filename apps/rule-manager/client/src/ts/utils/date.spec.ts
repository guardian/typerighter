import { formatTimestampTZ } from "./date";

describe("date utils", () => {
  describe("formatTimestampTZ", () => {
    it("should format timestamps to make them human readable", () => {
      const timestamp = "2023-05-23T17:35:48.264152+01:00[Europe/London]";
      const formattedTimestamp = "23rd May 2023 17:35:48"
      expect(formatTimestampTZ(timestamp)).toBe(formattedTimestamp);
    })
  })
})
