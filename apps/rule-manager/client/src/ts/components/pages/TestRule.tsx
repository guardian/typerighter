import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
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
	WithEuiThemeProps,
	useEuiTheme,
	withEuiTheme,
	EuiText,
	EuiLink,
	EuiIconTip,
} from '@elastic/eui';
import { useDebouncedValue } from '../hooks/useDebounce';
import { Title } from '../form/Title';
import { SectionHeader } from '../form/SectionHeader';
import { LineBreak } from '../LineBreak';

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
		<EuiFlexItem style={{ height: '100%', paddingTop: '12px' }}>
			<SectionHeader>
				<Title>
					TEST RULE&nbsp;
					<EuiIconTip
						content="Test a rule against Guardian content, showing any matches found."
						position="right"
						type="iInCircle"
						size="s"
					/>
				</Title>
			</SectionHeader>
			<EuiSpacer size="m" />
			<EuiFlexGroup style={{ height: '100%' }} direction="column">
				<EuiFlexItem grow={0}>
					<EuiFlexGroup direction={'row'} gutterSize="s">
						<EuiFlexItem>
							<EuiFieldText
								placeholder={
									'Add a CAPI query to narrow down the content to check'
								}
								value={queryStr}
								onChange={(e) => setQueryStr(e.target.value)}
								fullWidth={true}
							/>
						</EuiFlexItem>
						<EuiFlexItem>
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
						</EuiFlexItem>
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
