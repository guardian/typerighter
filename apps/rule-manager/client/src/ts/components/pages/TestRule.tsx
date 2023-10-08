import { useEffect, useMemo, useState } from 'react';
import { useOutletContext, useParams } from 'react-router-dom';
import {
	TyperighterChunkedAdapter,
	PaginatedCheckRuleResult,
	RuleMatch,
} from '../../utils/TyperighterChunkedAdapter';
import styled from '@emotion/styled';
import {
	EuiFieldText,
	EuiFlexGroup,
	EuiFlexItem,
	EuiLoadingSpinner,
	EuiSpacer,
	EuiTitle,
	WithEuiThemeProps,
	useEuiTheme,
	withEuiTheme,
	EuiText,
	EuiButtonEmpty,
	EuiLink,
} from '@elastic/eui';
import { RuleForm } from '../RuleForm';
import { noop } from 'lodash';
import { useDebouncedValue } from '../hooks/useDebounce';

const MatchContainer = withEuiTheme(styled.div<WithEuiThemeProps>`
	font-family: 'Guardian Egyptian Text';
	background-color: white;
	padding: ${({ theme }) => theme.euiTheme.base}px;
`);

const ResultText = styled.div`
	display: flex;
	align-items: center;
`;

const ResultActions = styled.div`
	margin-left: auto;
`;

const chunkedAdapter = new TyperighterChunkedAdapter();

export const TestRule = ({ pattern }: { pattern?: string }) => {
	const { id: ruleId } = useParams();
	const [matches, setMatches] = useState<RuleMatch[]>([]);
	const [isLoading, setIsLoading] = useState(false);
	const [result, setResult] = useState<PaginatedCheckRuleResult | undefined>(
		undefined,
	);
	const [queryStr, setQueryStr] = useState('');
	const gap = `${useEuiTheme().euiTheme.base}px`;
	const debouncedQueryStr = useDebouncedValue(queryStr, 1000);

	const fetchMatches = useMemo(
		() => () => {
			if (!pattern || !ruleId) {
				return;
			}

			setMatches([]);
			setIsLoading(true);

			chunkedAdapter.fetchMatches({
				queryStr: debouncedQueryStr,
				ruleId,
				onMatchesReceived: (result) => {
					setMatches((cur) => cur.concat(result.result.matches));
					setResult(result);
				},
				onRequestError: (err) => console.warn(err),
				onRequestComplete: () => setIsLoading(false),
			});
		},
		[debouncedQueryStr, pattern, ruleId],
	);

	useEffect(() => {
		fetchMatches();
		return () => chunkedAdapter.abort();
	}, [debouncedQueryStr, pattern]);

	return (
		<EuiFlexItem style={{ height: '100%' }}>
			<EuiFlexGroup style={{ height: '100%' }} direction="column">
				<EuiFlexItem grow={0}>
					<EuiFlexGroup direction={'column'} gutterSize="s">
						<EuiFieldText
							placeholder={
								'Add a CAPI query to narrow down the content to check'
							}
							value={queryStr}
							onChange={(e) => setQueryStr(e.target.value)}
							fullWidth={true}
						/>
						{result ? (
							<EuiFlexGroup>
								<ResultText>
									<span>
										Page {result.currentPage} of {result.maxPages} checked (
										{result.currentPage * result.pageSize} articles),{' '}
										{matches.length} matches found&nbsp;&nbsp;
									</span>
									{isLoading && <EuiLoadingSpinner size="s" />}
								</ResultText>
								{isLoading && (
									<ResultActions>
										<EuiLink href="#" onClick={() => chunkedAdapter.abort()}>
											Cancel
										</EuiLink>
									</ResultActions>
								)}
							</EuiFlexGroup>
						) : (
							''
						)}
					</EuiFlexGroup>
				</EuiFlexItem>
				<EuiFlexItem style={{ overflowY: 'scroll', gap }}>
					{!matches.length && (
						<>
							<EuiSpacer size="xxl" />
							<EuiText
								color="subdued"
								size="s"
								textAlign="center"
							>{`No matches found for this search${
								isLoading ? ' yet.' : '.'
							}`}</EuiText>
						</>
					)}
					{matches.map(
						({
							fromPos,
							toPos,
							precedingText,
							matchedText,
							subsequentText,
						}) => {
							return (
								<MatchContainer key={`${fromPos}-${toPos}-${matchedText}`}>
									<p>
										…{precedingText}
										<strong>{matchedText}</strong>
										{subsequentText}…
									</p>
								</MatchContainer>
							);
						},
					)}
				</EuiFlexItem>
			</EuiFlexGroup>
		</EuiFlexItem>
	);
};
