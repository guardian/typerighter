import React from "react";
import { EuiPageTemplate, EuiProvider } from "@elastic/eui";
import {Header} from "./Header";
import {Rules} from "../pages/Rules";
import {euiThemeOverrides} from "../../constants/euiTheme";

import createCache from '@emotion/cache'

// Necessary while SASS and Emotion styles coexist within EUI.
const cache = createCache({
  key: 'eui',
  // Ensure SASS global styles override Emotion.
  prepend: true
})

export const Page = () =>
  <EuiProvider modify={euiThemeOverrides} cache={cache}>
    <EuiPageTemplate>
      <Header />
      <EuiPageTemplate.Section color="subdued">
        <Rules />
      </EuiPageTemplate.Section>
    </EuiPageTemplate>
  </EuiProvider>
