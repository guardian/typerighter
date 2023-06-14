import React from "react";
import { EuiPageTemplate, EuiProvider } from "@elastic/eui";
import { Header } from "./Header";
import { Rules } from "../pages/Rules";
import { euiThemeOverrides } from "../../constants/euiTheme";

import createCache from "@emotion/cache";
import { FeatureSwitchesProvider } from "../context/featureSwitches";
import { PageDataProvider } from "../../utils/window";
import RulesTable from "../RulesTable";

// Necessary while SASS and Emotion styles coexist within EUI.
const cache = createCache({
  key: "eui",
  // Ensure SASS global styles override Emotion.
  prepend: true,
});

export const Page = () => (
  <PageDataProvider>
    <FeatureSwitchesProvider>
      <EuiProvider modify={euiThemeOverrides} cache={cache}>
        <EuiPageTemplate>
          <Header />
          <EuiPageTemplate.Section color="subdued" restrictWidth={false}>
            <RulesTable />
          </EuiPageTemplate.Section>
        </EuiPageTemplate>
      </EuiProvider>
    </FeatureSwitchesProvider>
  </PageDataProvider>
);
