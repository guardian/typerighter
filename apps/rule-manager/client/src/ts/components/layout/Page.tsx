import React from "react";
import { EuiPageTemplate, EuiProvider } from "@elastic/eui";
import {Header, headerHeight} from "./Header";
import { Rules } from "../pages/Rules";
import { euiThemeOverrides } from "../../constants/euiTheme";
import { Routes, Route, useParams } from 'react-router-dom';

import createCache from "@emotion/cache";
import { FeatureSwitchesProvider } from "../context/featureSwitches";
import { PageDataProvider } from "../../utils/window";
import RulesTable from "../RulesTable";
import styled from "@emotion/styled";
import { PageNotFound } from "../PageNotFound";

// Necessary while SASS and Emotion styles coexist within EUI.
const cache = createCache({
  key: "eui",
  // Ensure SASS global styles override Emotion.
  prepend: true,
});

const PageContent = styled.div`
  height: 100vh;
  padding: calc(${headerHeight} + 24px) 24px 24px 24px;
`;

export const Page = () => (
  <PageDataProvider>
    <FeatureSwitchesProvider>
      <EuiProvider modify={euiThemeOverrides} cache={cache}>
        <EuiPageTemplate>
          <Header />
          <Routes>
            <Route path="/" element={
                <>
                  <PageContent>
                    <RulesTable />
                  </PageContent>
                </>
              }
            />
            <Route path="/tags" element={
                <>
                  <PageContent>
                    Tags will be here
                  </PageContent>
                </>
              }
            />
            <Route path="/*" element={
                <>
                  <PageContent>
                    <PageNotFound />
                  </PageContent>
                </>
              }
            />
          </Routes>
        </EuiPageTemplate>
      </EuiProvider>
    </FeatureSwitchesProvider>
  </PageDataProvider>

);
