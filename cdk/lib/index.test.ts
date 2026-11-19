import { describe, it } from 'node:test';
import { TrackingTag as MockedTrackingTag } from '@guardian/cdk/lib/constants/__mocks__/tracking-tag.js';
import { TrackingTag } from '@guardian/cdk/lib/constants/tracking-tag.js';
import { format as prettyFormat } from '@vitest/pretty-format';
import { Template } from 'aws-cdk-lib/assertions';
import { App } from 'aws-cdk-lib';
import { Typerighter } from './index.ts';

// Override properties of TrackingTag with the mocked version, so snapshots generated are more stable
Object.assign(TrackingTag, MockedTrackingTag);

// eslint-disable-next-line @typescript-eslint/no-explicit-any -- Matches typing of prettyFormat()
const jsonSerializer = (value: any) =>
	prettyFormat(value, { printBasicPrototype: false, escapeString: false });

describe('The typerighter stack', () => {
	it('matches the snapshot', (t) => {
		const app = new App();
		const stack = new Typerighter(app, 'typerighter', {
			env: {
				region: 'eu-west-1',
			},
			stack: 'flexible',
			stage: 'TEST',
			instanceCount: 1,
			domainSuffix: 'test.dev-gutools.co.uk',
		});

		const template = Template.fromStack(stack);
		t.assert.snapshot(template.toJSON(), { serializers: [jsonSerializer] });
	});
});
