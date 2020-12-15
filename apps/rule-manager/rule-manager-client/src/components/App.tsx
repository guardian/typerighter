/** @jsx jsx */
import { jsx, css } from "@emotion/core";
import { useState } from "react";
import { Button } from "@guardian/src-button";
import { TextInput } from "@guardian/src-text-input";
import { Fragment } from "react";
import { Container, Columns, Column } from "@guardian/src-layout";
import { headline } from "@guardian/src-foundations/typography";
import { space } from "@guardian/src-foundations";

const action = () => console.log("button clicked");

const h1 = css`
  ${headline.large()}
`;

const search = css`
  position: absolute;
  bottom: 0;
`;


// 'space' is currently not working
const buttonrow = css`
  /* margin-top: ${space[5]};  */
  margin-top: 20px;
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
        <div
          css={`
            position: relative;
          `}
        >
          <Columns>
            <Column width={5 / 8}>
              <TextInput
                label="Search all rules"
                supporting="name, description, patterns, tags"
                value={state}
                onChange={(event) => setState(event.target.value)}
                optional={true}
              />
            </Column>
            <Column width={1 / 8}>
              <div css={search}>
                <Button
                  priority="primary"
                  size="default"
                  iconSide="left"
                  hideLabel={false}
                  onClick={action}
                >
                  Search
                </Button>
              </div>
            </Column>
          </Columns>
        </div>
        <div css={buttonrow}>
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
            <Column width={3 / 8}>
              <div/>
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
        </div>
      </Container>
    </Fragment>
  );
};

export default App;
