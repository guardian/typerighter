/** @jsx jsx */
import { text, background } from "@guardian/src-foundations/palette";
import { jsx, css } from "@emotion/react";
import { Fragment } from "react";

const headline = css`
  color: ${text.error};
  background: ${background.secondary};
`

const App = () => (
  <Fragment>
    <div css={headline}>Hello world! This is the rule manager client!</div>
  </Fragment>
);

export default App;
