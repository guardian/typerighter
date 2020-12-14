/** @jsx jsx */
import { text, background } from "@guardian/src-foundations/palette";
import { jsx, css } from "@emotion/core";
import { useState } from "react";
import { Button } from "@guardian/src-button";
import { TextInput } from "@guardian/src-text-input";
import { Fragment } from "react";
import { Container, Columns, Column } from "@guardian/src-layout";
import { headline, body, textSans } from "@guardian/src-foundations/typography";

const action = () => console.log("button clicked");

const h1 = css`
  ${headline.large()}
`;

const App = () => {
  const [state, setState] = useState("");

  return (
    <Fragment>
      <Container>
        <Columns>
          <Column width={4 / 4}>
            <h1 css={h1}>Typerigher rule manager</h1>
          </Column>
        </Columns>
        <Columns>
          <Column width={7 / 8}>
            <TextInput
              label="Search all rules"
              supporting="name, description, patterns, tags"
              value={state}
              onChange={(event) => setState(event.target.value)}
              optional={true}
            />
          </Column>
          <Column width={1 / 8}>
            <Button
              priority="primary"
              size="default"
              iconSide="left"
              hideLabel={false}
              onClick={action}
            >
              Search
            </Button>
          </Column>
        </Columns>
        <Columns collapseBelow="tablet">
          <Column width={1 / 8}>
            <Button
              priority="primary"
              size="default"
              iconSide="left"
              hideLabel={false}
              onClick={action}
            >
             Disable
            </Button>
          </Column>
          <Column width={1 / 8}>
            <Button
              priority="primary"
              size="default"
              iconSide="left"
              hideLabel={false}
              onClick={action}
            >
             Archive
            </Button>
          </Column>
          <Column width={5 / 8}>
            <div></div>
          </Column>
          <Column width={1 / 8}>
            <Button
              priority="primary"
              size="default"
              iconSide="left"
              hideLabel={false}
              onClick={action}
            >
              Create rule
            </Button>
          </Column>
        </Columns>
      </Container>
    </Fragment>
  );
};

export default App;
