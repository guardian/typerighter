import styled from '@emotion/styled';

export const subdued = '#F7F8FC';
export const lightShade = '#D3DAE6';

export const Event = styled.div`
	display: flex;
`;

export const EventTimelineContainer = styled.div<{ isFirst: boolean }>`
	position: relative;
	${({ isFirst }) => !isFirst && `border-left: 2px solid ${subdued};`}
	margin-left: 16px;
	margin-right: 27px;
`;

export const EventTimelinePersonContainer = styled.div`
	position: absolute;
	width: 32px;
	height: 32px;
	left: -16px;
	background-color: ${subdued};
	border-radius: 50%;
`;

export const EventDetails = styled.div<{ isFirst: boolean }>`
	border: 1px solid ${subdued};
	border-radius: 6px;
	flex-grow: 1;
	${({ isFirst }) => !isFirst && `margin-bottom: 8px;`}
`;

export const EventDetailsHeader = styled.div`
	background-color: ${subdued};
	border-bottom: 1px solid ${lightShade};
	padding: 8px;
`;

export const EventDetailsBody = styled.div`
	background-color: #fff;
	padding: 8px;
`;
