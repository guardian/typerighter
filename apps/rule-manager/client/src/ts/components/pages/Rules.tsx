import React from "react";
import '@elastic/eui/dist/eui_theme_light.css';
import { EuiPageSection } from "@elastic/eui";
import RulesTable from "../RulesTable";

export const Rules = () => {
    return (
        <>
            <EuiPageSection bottomBorder={true}>
               <RulesTable />
            </EuiPageSection>
        </>
    );
}
