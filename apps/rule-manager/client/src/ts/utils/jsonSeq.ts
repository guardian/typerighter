export const RECORD_SEPARATOR = String.fromCharCode(31);

export const readJsonSeqStream = async (
	reader: ReadableStreamDefaultReader,
	onMessage: (message: any) => void,
): Promise<void> => {
	// window.TextDecoder will not be defined in a NodeJS context (for our tests)..
	const textDecoder = new window.TextDecoder();
	const initialChunk = await reader.read();
	// Chunks do not correspond directly with lines of JSON, so we must buffer
	// partial lines.
	let buffer = '';

	const parseAndPushMessage = (json: string) => {
		const message = JSON.parse(json.trim());
		onMessage(message);
	};

	const processStringChunk = (chunk: string) => {
		const textChunks = (buffer + chunk).split(RECORD_SEPARATOR);

		// Take everything but the tail of the array. This will either be an empty
		// string, as the preceding line will have been terminated by a newline
		// character, or a partial line.
		for (const rawJson of textChunks.slice(0, -1)) {
			const json = rawJson.trim();
			if (!json.length) {
				break;
			}

			parseAndPushMessage(json);
		}

		// Add anything that remains to the buffer.
		buffer = (textChunks[textChunks.length - 1] ?? '').trim();
	};

	const streamIterator = ({
		done,
		value,
	}: ReadableStreamDefaultReadResult<Uint8Array>): Promise<void> => {
		if (done) {
			if (buffer.length) {
				// Flush anything that remains in the buffer
				parseAndPushMessage(buffer);
			}
			return Promise.resolve();
		}

		const textChunks = textDecoder.decode(value);
		processStringChunk(textChunks);

		return reader.read().then(streamIterator);
	};

	return streamIterator(initialChunk);
};
