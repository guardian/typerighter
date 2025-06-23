/*
When running in CI (GitHub Actions), use the GitHub Actions reporter to annotate the PR with any test failures.
Locally, use the default reporter.

See:
  - https://jestjs.io/docs/configuration#github-actions-reporter
  - https://docs.github.com/en/actions/learn-github-actions/variables#default-environment-variables
 */
const reporters = process.env.GITHUB_ACTIONS
	? [['github-actions', { silent: false }], 'summary']
	: ['default'];

// eslint-disable-next-line import/no-default-export -- TODO
export default {
	reporters,
	verbose: true,
	testEnvironment: 'node',
	projects: [
		{
			displayName: 'lambda',
			transform: {
				'^.+\\.tsx?$': 'ts-jest',
			},
			testMatch: ['*.test.ts'],
		},
	],
};
