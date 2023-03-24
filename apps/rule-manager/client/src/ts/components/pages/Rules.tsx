import React from "react";
import '@elastic/eui/dist/eui_theme_light.css';
import {
    EuiTitle,
    EuiButton,
    EuiPageSection,
    EuiFlexGroup,
    EuiFlexItem
} from "@elastic/eui";
import RulesTable from "../rules-table";

export const Rules = () => {
    return (
        <>

            <EuiPageSection bottomBorder={true}>
               <RulesTable />
            </EuiPageSection>

        </>
    );
}
