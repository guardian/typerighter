import React from "react";
import {RuleData} from "./hooks/useRule";
import {RuleFormSection} from "./RuleFormSection";
import {capitalize} from "lodash";
import {getRuleState, getRuleStateColour, hasUnpublishedChanges} from "../utils/rule";
import {EuiButton, EuiHealth, EuiText, EuiTextColor} from "@elastic/eui";
import {css} from "@emotion/react";
import {euiTextTruncate} from "@elastic/eui/src/global_styling/mixins/_typography";
import styled from "@emotion/styled";
import {LineBreak} from "./LineBreak";

const RuleStatusContainer = styled.div`display: flex; justify-content: space-between; align-items: center`;
const AnotherContainer = styled.div`display: flex;`
const UnpublishedChangesContainer = styled.div`display: flex; align-items: center; flex-grow: 0;`

export const RuleStatus = ({ruleData}: {
  ruleData: RuleData | undefined
}) => {
  const state = capitalize(getRuleState(ruleData?.draft));
  return <RuleFormSection title="RULE STATUS" additionalInfo={
    !!ruleData && hasUnpublishedChanges(ruleData) && "Has unpublished changes"}>
    <LineBreak/>
    <RuleStatusContainer>
      <AnotherContainer>
        <EuiHealth textSize="m" color={getRuleStateColour(ruleData?.draft)} />
        <EuiText css={css`${euiTextTruncate()}`}>{state}</EuiText>
      </AnotherContainer>

    </RuleStatusContainer>
  </RuleFormSection>
}
