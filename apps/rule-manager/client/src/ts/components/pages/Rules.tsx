import React from "react";
import {EuiPageBody, EuiPageSection} from "@elastic/eui";
import RulesTable from "../RulesTable";

export const Rules = () => {
    return (
      <EuiPageBody>
        <EuiPageSection bottomBorder={true}>
            <RulesTable/>
        </EuiPageSection>
      </EuiPageBody>
    );
}
