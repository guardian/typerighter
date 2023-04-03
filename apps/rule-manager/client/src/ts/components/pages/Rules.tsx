import React from "react";
import {EuiPageSection} from "@elastic/eui";
import RulesTable from "../RulesTable";

export const Rules = () => {
    return (
        <EuiPageSection bottomBorder={true}>
            <RulesTable/>
        </EuiPageSection>
    );
}
