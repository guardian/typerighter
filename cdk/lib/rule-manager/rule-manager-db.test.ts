import "@aws-cdk/assert/jest";
import { SynthUtils } from "@aws-cdk/assert";
import { App } from "@aws-cdk/core";
import { RuleManagerDB } from "./rule-manager-db";

describe("The rule manager db stack", () => {
    it("matches the snapshot", () => {
        const app = new App();
        const stack = new RuleManagerDB(app, "rule-manager-db", {
            app: "typerighter-rule-manager",
            stack: "flexible",
        });

        expect(SynthUtils.toCloudFormation(stack)).toMatchSnapshot();
    })
})
