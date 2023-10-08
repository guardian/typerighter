import { throttle, uniqBy } from 'lodash';
import { readJsonSeqStream } from './jsonSeq';

interface Category {
	id: string;
	name: string;
}

interface Suggestion {
	type: string;
	text: string;
}

interface CheckerRule {
	id: string;
	category: Category;
}

export interface RuleMatch {
	rule: CheckerRule;
	fromPos: number;
	toPos: number;
	precedingText: string;
	subsequentText: string;
	matchedText: string;
	message: string;
	shortMessage?: string;
	suggestions: Suggestion[];
	replacement?: Suggestion;
	markAsCorrect: boolean;
	matchContext: string;
}

interface CheckSingleRuleResult {
	matches: RuleMatch[];
	percentageRequestComplete: number | undefined;
}

export interface PaginatedCheckRuleResult {
	currentPage: number;
	maxPages: number;
	pageSize: number;
	result: CheckSingleRuleResult;
}

interface CAPICheck {
	queryStr: string;
	tags?: string[];
	sections?: string[];
	ruleId: string;
	onMatchesReceived: (result: PaginatedCheckRuleResult) => void;
	onRequestError: (msg: string) => void;
	onRequestComplete: () => void;
}

/**
 * An adapter for the Typerighter service that parses a chunked response returning
 * [`json-seq`](https://en.wikipedia.org/wiki/JSON_streaming#Record_separator-delimited_JSON).
 */
export class TyperighterChunkedAdapter {
	private responseBuffer: PaginatedCheckRuleResult[] = [];
	private responseThrottleMs = 100;
	private abortController: AbortController | undefined = undefined;

	public fetchMatches = async ({
		queryStr,
		tags,
		sections,
		ruleId,
		onMatchesReceived,
		onRequestError,
		onRequestComplete,
	}: CAPICheck) => {
		this.abortController?.abort();
		this.abortController = new AbortController();

		const onComplete = () => {
			this.abortController = undefined;
			onRequestComplete();
		};

		try {
			const response = await fetch(`/api/rules/${ruleId}/test-capi`, {
				method: 'POST',
				headers: new Headers({
					'Content-Type': 'application/json',
				}),
				body: JSON.stringify({
					queryStr,
					tags,
					sections,
				}),
				signal: this.abortController.signal,
			});

			if (response.status > 399) {
				onRequestError(`${response.status}: ${response.statusText}`);
				return onComplete();
			}

			const reader = response.body?.getReader();

			if (!reader) {
				onRequestError('Typerighter did not send a response body');
				return onComplete();
			}

			const streamReader = readJsonSeqStream(reader, (message) => {
				this.responseBuffer.push(message);
				this.throttledHandleResponse(onMatchesReceived);
			});

			return streamReader
				.catch((error) => {
					const isAbortRequest =
						error instanceof DOMException && error.name === 'AbortError';
					if (isAbortRequest) {
						// Necessary, or the stream continues to read
						reader.cancel();
					} else {
						onRequestError(error.message);
					}
					return onComplete();
				})
				.finally(() => {
					this.flushResponseBuffer(onMatchesReceived);
					onComplete();
				});
		} catch (e) {
			console.log(e);
		}
	};

	public abort = () => {
		this.abortController?.abort();
	};

	private flushResponseBuffer = (
		onMatchesReceived: (result: PaginatedCheckRuleResult) => void,
	) => {
		if (!this.responseBuffer.length) {
			return;
		}
		const matches = this.responseBuffer.flatMap((_) => _.result.matches);
		const latestResponse = this.responseBuffer[this.responseBuffer.length - 1];

		onMatchesReceived({
			currentPage: latestResponse.currentPage,
			maxPages: latestResponse.maxPages,
			pageSize: latestResponse.pageSize,
			result: {
				percentageRequestComplete:
					latestResponse.result.percentageRequestComplete,
				matches,
			},
		});

		// Clear the buffer
		this.responseBuffer = [];
	};

	protected throttledHandleResponse = throttle(
		this.flushResponseBuffer,
		this.responseThrottleMs,
	);
}
