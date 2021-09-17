import "@aws-cdk/assert/jest";
import { SynthUtils } from "@aws-cdk/assert";
import { App } from "@aws-cdk/core";
import { Typerighter } from "./";

describe("The typerighter stack", () => {
    it("matches the snapshot", () => {
        const app = new App();
        const stack = new Typerighter(app, "typerighter", {
            stack: "flexible",
        });

        expect(SynthUtils.toCloudFormation(stack)).toMatchSnapshot();
    })
})
