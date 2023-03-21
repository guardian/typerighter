import React from "react";
import { EuiPageTemplate, EuiProvider } from "@elastic/eui";
import {Header} from "./Header";
import {Rules} from "../pages/Rules";
import {euiThemeOverrides} from "../../constants/euiTheme";

export const Page = () =>
  <EuiProvider modify={euiThemeOverrides}>
    <EuiPageTemplate>
      <Header />
      <EuiPageTemplate.Section color="subdued">
        <Rules />
      </EuiPageTemplate.Section>
    </EuiPageTemplate>
  </EuiProvider>
