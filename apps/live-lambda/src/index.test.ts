import { main } from './index';

describe('The lambda', () => {
	beforeAll(() => {
		process.env.STACK = 'playground';
		process.env.STAGE = 'TEST';
		process.env.APP = 'example-typescript-lambda';
	});

	it('should return a greeting', async () => {
		const response = await main();
		expect(response).toContain(
			'Hello from example-typescript-lambda in TEST! The time is ',
		);
	});
});
