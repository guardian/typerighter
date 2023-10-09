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
	EuiSelect,
} from '@elastic/eui';
import { useDebouncedValue } from '../hooks/useDebounce';
import { Title } from '../form/Title';
import { SectionHeader } from '../form/SectionHeader';

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

const maxMatchOptions = [
	{
		value: 10,
		text: 'Search for max. 10 matches',
	},
	{
		value: 20,
		text: 'Search for max. 20 matches',
	},
	{
		value: 50,
		text: 'Search for max. 50 matches',
	},
];

const maxPageOptions = [
	{
		value: 50,
		text: 'Across 50 pages of content',
	},
	{
		value: 100,
		text: 'Across 100 pages of content',
	},
	{
		value: 200,
		text: 'Across 200 pages of content',
	},
];

export const TestRule = ({ pattern }: { pattern?: string }) => {
	const { id: ruleId } = useParams();
	const [matches, setMatches] = useState<RuleMatch[]>([]);
	const [maxMatches, setMaxMatches] = useState<number>(10);
	const [maxPages, setMaxPages] = useState<number>(50);
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
        maxMatches,
        maxPages,
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
					TEST RULE AGAINST GUARDIAN CONTENT&nbsp;
					<EuiIconTip
						content="Test this rule by running it against lots of Guardian content. Any matches found are shown below."
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
							<EuiSelect
								options={maxMatchOptions}
								value={maxMatches}
								onChange={(e) => setMaxMatches(parseInt(e.target.value))}
							/>
						</EuiFlexItem>
						<EuiFlexItem>
							<EuiSelect options={maxPageOptions}value={maxPages}
								onChange={(e) => setMaxPages(parseInt(e.target.value))} />
						</EuiFlexItem>
						<EuiFlexItem grow={2}>
							<EuiFieldText
								placeholder={'Narrow down the search with a CAPI query'}
								value={queryStr}
								onChange={(e) => setQueryStr(e.target.value)}
								fullWidth={true}
							/>
						</EuiFlexItem>
					</EuiFlexGroup>
					<EuiSpacer size="s" />
					<EuiFlexItem grow={2}>
						{result ? (
							<EuiFlexGroup>
								<ResultText>
									<span>
										Page {result.currentPage}/{result.maxPages} checked (
										{result.currentPage * result.pageSize} articles),{' '}
										{matches.length}/{maxMatches} matches found&nbsp;&nbsp;
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
