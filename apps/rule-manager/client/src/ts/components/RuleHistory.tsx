import {EuiFormRow, EuiIcon} from "@elastic/eui";
import React from "react"
import {RuleFormSection} from "./RuleFormSection"
import {RuleData} from "./hooks/useRule";
import {maybeGetNameFromEmail} from "../utils/user";
import styled from "@emotion/styled";
import {LineBreak} from "./LineBreak";
import {formatTimestampTZ, friendlyTimestampFormat} from "../utils/date";
import {Person} from "./icons/person";
import {format} from "date-fns";

const subdeued = "#F7F8FC";
const lightShade = "#D3DAE6";

const Event = styled.div`
  display: flex;
`;

const EventTimeline = styled.div<{ isFirstPublished: boolean }>`
  position: relative;
  ${({isFirstPublished}) => !isFirstPublished && `border-left: 2px solid ${subdeued};`}
  margin-left: 16px;
  margin-right: 27px;
`;

const EventTimelinePersonContainer = styled.div`
  position: absolute;
  width: 32px;
  height: 32px;
  left: -16px;
  background-color: ${subdeued};
  border-radius: 50%;
`;

const EventDetails = styled.div<{ isFirstPublished: boolean }>`
  border: 1px solid ${subdeued};
  border-radius: 6px;
  flex-grow: 1;
  ${({isFirstPublished}) => !isFirstPublished && `margin-bottom: 8px;`}
`;

const EventDetailsWho = styled.div`
  background-color: ${subdeued};
  border-bottom: 1px solid ${lightShade};
  padding: 8px;
`;

const EventDetailsWhy = styled.div`
  background-color: #FFF;
    padding: 8px;
`;

const SheetIconContainer = styled.div`
  padding: 7px 8px;
`
const SheetIcon = () => <SheetIconContainer><EuiIcon type="pageSelect" size="lg"/></SheetIconContainer>

export const RuleHistory = ({ruleHistory}: { ruleHistory: RuleData['history'] }) => {
  const sortedHistory = ruleHistory.concat().sort((a, b) => a.revisionId > b.revisionId ? -1 : 1);
  return <RuleFormSection title="PUBLICATION HISTORY">
    <LineBreak/>
    <EuiFormRow><>
      {!sortedHistory.length && "This rule has not yet been published."}
      {sortedHistory.map((rule, index) => {
        const isFirstPublished = index === (sortedHistory.length - 1)
        return <Event>
          <EventTimeline isFirstPublished={isFirstPublished}>
            <EventTimelinePersonContainer>
              {rule.updatedBy.includes("Google Sheet") ? <SheetIcon/> : <Person/>}
            </EventTimelinePersonContainer>
          </EventTimeline>
          <EventDetails isFirstPublished={isFirstPublished}>
            <EventDetailsWho>
              <strong>{maybeGetNameFromEmail(rule.updatedBy)}</strong>, {format(new Date(rule.updatedAt), friendlyTimestampFormat)}
            </EventDetailsWho>
            <EventDetailsWhy>{rule.reason}</EventDetailsWhy>
          </EventDetails>

        </Event>
      })}
    </>

    </EuiFormRow>
  </RuleFormSection>
}
