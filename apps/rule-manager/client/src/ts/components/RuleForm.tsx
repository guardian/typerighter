import { EuiFlexGroup, EuiForm } from "@elastic/eui";
import React from "react"
import { RuleContent } from "./RuleContent";
import { RuleType } from "./RuleType";
import {RuleMetadata} from "./RuleMetadata";

export const RuleForm = () => {
    return <EuiForm component="form">
            <EuiFlexGroup  direction="column">   
                <RuleContent />
                <RuleType />
                <RuleMetadata />
        </EuiFlexGroup>
    </EuiForm>
}