import "@aws-cdk/assert/jest";
import { SynthUtils } from "@aws-cdk/assert";
import { App } from "@aws-cdk/core";
import { RuleManager } from "./rule-manager";

describe("The rule manager stack", () => {
    it("matches the snapshot", () => {
        const app = new App();
        const stack = new RuleManager(app, "rule-manager", {
            stack: "flexible",
        });

        expect(SynthUtils.toCloudFormation(stack)).toMatchSnapshot();
    })
})
