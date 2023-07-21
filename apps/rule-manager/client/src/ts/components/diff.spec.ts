import { FieldObject, doValuesMatch, findFieldsWithDiffs, findNonIntersectingFields, getHumanReadableValues } from "./Diff"
import { ruleTypeOptions } from "./RuleContent"
import { RuleData } from "./hooks/useRule"
import { Tag } from "./hooks/useTags"

const mockRuleData: RuleData = {
    draft: {
        ruleType: 'regex',
        pattern: 'pattern1',
        replacement: 'replacement1',
        category: 'category1',
        tags: [1, 2],
        ignore: false,
        revisionId: 0,
        createdBy: "me",
        createdAt: "1",
        updatedBy: "1",
        updatedAt: "1",
        isArchived: true,
        isPublished: true,
        hasUnpublishedChanges: true
    },
    live: [{
        ruleType: 'languageToolXML',
        pattern: 'pattern2',
        category: 'category1',
        tags: [2, 3],
        ignore: false,
        revisionId: 0,
        createdBy: "me",
        createdAt: "1",
        updatedBy: "1",
        updatedAt: "1",
        isArchived: true,
        isPublished: true,
        hasUnpublishedChanges: true,
        reason: "My reason"
    }]
}

const mockTags: Record<number, Tag> = {
    1: {id: 1, name: "Tag1"},
    2: {id: 2, name: "Tag2"},
    3: {id: 3, name: "Tag3"}
}

const expectedDiffFields = [
    {fieldName: "replacement", draft: "replacement1", live: undefined},
    {fieldName: "ruleType", draft: "regex", live: "languageToolXML"},
    {fieldName: "pattern", draft: "pattern1", live: "pattern2"},
    {fieldName: "tags", draft: [1,2], live: [2,3]}
]

describe("Diff helper functions", () => {
    describe("findFieldsWithDiffs", () => {
        it("should find divergent fields", () => {
            const fieldsWithDiffs = findFieldsWithDiffs(mockRuleData);
            expect(fieldsWithDiffs).toEqual(expectedDiffFields)
        })
        it("should not provide diffs for identical tag arrays", () => {
            const mockRuleDataWithIdenticalTags = {
                live: [{...mockRuleData.live[0]}],
                draft: {...mockRuleData.draft}
            }
            mockRuleDataWithIdenticalTags.draft.tags = [2,3];
            const expectedDiff = expectedDiffFields.filter(field => field.fieldName != "tags")
            const fieldsWithDiffs = findFieldsWithDiffs(mockRuleDataWithIdenticalTags);
            expect(fieldsWithDiffs).toEqual(expectedDiff)
        })
    })
    describe("getHumanReadableValues", () => {
        it("should convert ruleTypes to the names used on the rule form", () => {
            const humanReadableFields = getHumanReadableValues(expectedDiffFields, mockTags);
            const languageToolName = ruleTypeOptions.find(option => option.id === "languageToolXML")?.label
            const regexName = ruleTypeOptions.find(option => option.id === "regex")?.label
            const expectedRuleTypeField = {fieldName: "ruleType", draft: regexName, live: languageToolName}
            expect(humanReadableFields.find(field => field.fieldName === "ruleType")).toEqual(expectedRuleTypeField)
        })
        it("should convert tags from their IDs to their readable names", () => {
            const humanReadableFields = getHumanReadableValues(expectedDiffFields, mockTags);
            const expectedTagsField = {fieldName: "tags", draft: ["Tag1", "Tag2"], live: ["Tag2", "Tag3"]}
            expect(humanReadableFields.find(field => field.fieldName === "tags")).toEqual(expectedTagsField)
        })
    })
    describe("findNonIntersectingFields", () => {
        it("should identify fields that appear in either array but not both", () => {
            const draft: FieldObject[] = [{
                fieldName: "field1",
                value: 1
            },{
                fieldName: "field2",
                value: 2
            }];
            const live: FieldObject[] = [{
                fieldName: "field2",
                value: 2
            },{
                fieldName: "field3",
                value: 3
            }]
            const expected = [{
                fieldName: "field1",
                draft: 1,
                live: undefined
            },{
                fieldName: "field3",
                draft: undefined,
                live: 3,
            }]
            const actual = findNonIntersectingFields(draft, live);
            expect(actual).toEqual(expected)
        })
    })
    describe("doValuesMatch", () => {
        it("should match two primitive values", () => {
            const result = doValuesMatch(1,1)
            expect(result).toBe(true)
        })
        it("should not match two different primitive values", () => {
            const result = doValuesMatch("me","you")
            expect(result).toBe(false)
        })
        it("should match two arrays of primitives", () => {
            const result = doValuesMatch([1,3],[1,3])
            expect(result).toBe(true)
        })
    })
})