import { EuiFieldText, EuiFlexGroup, EuiFlexItem, EuiForm, EuiFormRow, EuiRadioGroup } from "@elastic/eui"
import { css } from "@emotion/react";
import React, { useState } from "react"
import { RuleContent } from "./RuleContent";
import { RuleType } from "./RuleType";

export const RuleForm = () => {
    return <EuiForm component="form">
            <EuiFlexGroup  direction="column">   
                <RuleContent />
                <RuleType />
        </EuiFlexGroup>
    </EuiForm>
}