import { EuiFormRow } from "@elastic/eui";
import React from "react"
import { RuleFormSection } from "./RuleFormSection"
import {RuleData} from "./hooks/useRule";
import {maybeGetNameFromEmail} from "../utils/user";
import styled from "@emotion/styled";
import {LineBreak} from "./LineBreak";
import {formatTimestampTZ} from "../utils/date";
import {Person} from "./icons/person";

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

export const RuleHistory = ({ruleHistory}: {ruleHistory: RuleData['history']}) => {

    return <RuleFormSection title="PUBLICATION HISTORY">
      <LineBreak/>
        <EuiFormRow
        ><>
          {!ruleHistory.length && "This rule has not yet been published."}
          {ruleHistory.map((rule, index) => {
            const isFirstPublished = index === (ruleHistory.length - 1)
            return <Event>
              <EventTimeline isFirstPublished={isFirstPublished}>
  <EventTimelinePersonContainer>

                <Person />
  </EventTimelinePersonContainer>
              </EventTimeline>
              <EventDetails isFirstPublished={isFirstPublished}>
                <EventDetailsWho>
                  <strong>{maybeGetNameFromEmail(rule.updatedBy)}</strong>, {formatTimestampTZ(rule.updatedAt)}
                </EventDetailsWho>
                <EventDetailsWhy>{rule.reason}</EventDetailsWhy>
              </EventDetails>

            </Event>
          })}
        </>

        </EuiFormRow>
    </RuleFormSection>
}
