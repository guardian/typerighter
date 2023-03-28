import { Template } from "aws-cdk-lib/assertions";
import { App } from "aws-cdk-lib";
import { Typerighter } from "./";

describe("The typerighter stack", () => {
    it("matches the snapshot", () => {
        const app = new App();
        const stack = new Typerighter(app, "typerighter", {
            stack: "flexible",
            stage: "TEST",
            instanceCount: 1,
            domainSuffix: "test.dev-gutools.co.uk"
        });

        const template = Template.fromStack(stack);

        expect(template.toJSON()).toMatchSnapshot();
    })
})
