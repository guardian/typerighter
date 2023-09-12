import { ReactNode } from 'react';
import styled from '@emotion/styled';

const Container = styled.div`
	display: flex;
	flex-direction: column;
	height: 100%;
	width: 100%;
	min-width: 0;
	min-height: 0;
`;

const Header = styled.div`
	flex-grow: 0;
`;
const Content = styled.div`
	flex-grow: 2;
	overflow: hidden;
`;

/**
 * +---------------------+
 * |       Header        |
 * +---------------------+
 * |         ^           |
 * |         |           |
 * | Full-height content |
 * |         |           |
 * |         v           |
 * +---------------------+
 */
export const FullHeightContentWithFixedHeader = ({
	header,
	content,
}: {
	header: ReactNode;
	content: ReactNode;
}) => (
	<Container>
		<Header>{header}</Header>
		<Content>{content}</Content>
	</Container>
);
