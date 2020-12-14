/** @jsx jsx */
import { text, background } from "@guardian/src-foundations/palette";
import { jsx, css } from "@emotion/core";
import { Fragment } from "react";
import { Button } from "@guardian/src-button";

const headline = css`
  color: ${text.error};
  background: ${background.secondary};
`;

const action = () => console.log("button clicked");
const App = () => (
  <Fragment>
    <div css={headline}>Hello world! This is the rule manager client!</div>
    <Button
    priority="primary"
    size="default"
    iconSide="left"
    hideLabel={false}
    onClick={action}>Click me!</Button>
  </Fragment>
);

export default App;
