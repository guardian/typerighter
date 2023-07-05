import React from "react";
import { EuiPageTemplate, EuiProvider } from "@elastic/eui";
import { Header } from "./Header";
import { Rules } from "../pages/Rules";
import { euiThemeOverrides } from "../../constants/euiTheme";
import { Routes, Route, useParams } from 'react-router-dom';

import createCache from "@emotion/cache";
import { FeatureSwitchesProvider } from "../context/featureSwitches";
import { PageDataProvider } from "../../utils/window";
import RulesTable from "../RulesTable";
import { PageNotFound } from "../PageNotFound";

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
          <Routes>
            <Route path="/" element={
                <>
                  <EuiPageTemplate.Section color="subdued" restrictWidth={false}>
                    <RulesTable />
                  </EuiPageTemplate.Section>
                </>
              }
            />
            <Route path="/tags" element={
                <>
                  <EuiPageTemplate.Section color="subdued" restrictWidth={false}>
                    Tags will be here
                  </EuiPageTemplate.Section>
                </>
              }
            />
            <Route path="/*" element={
                <>
                  <EuiPageTemplate.Section color="subdued" restrictWidth={false}>
                    <PageNotFound />
                  </EuiPageTemplate.Section>
                </>
              }
            />
          </Routes>
        </EuiPageTemplate>
      </EuiProvider>
    </FeatureSwitchesProvider>
  </PageDataProvider>
  
);
