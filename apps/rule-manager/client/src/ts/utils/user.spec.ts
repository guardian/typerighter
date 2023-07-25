import { maybeGetNameFromEmail } from './user';

describe('user utils', () => {
	describe('maybeGetNameFromEmail', () => {
		it('should turn a Guardian e-mail into a name', () => {
			const email = 'eva.smith@guardian.co.uk';
			const formattedEmail = 'Eva Smith';
			expect(maybeGetNameFromEmail(email)).toBe(formattedEmail);
		});
		it("should not include 'casual'", () => {
			const email = 'eva.smith.casual@guardian.co.uk';
			const formattedEmail = 'Eva Smith';
			expect(maybeGetNameFromEmail(email)).toBe(formattedEmail);
		});
	});
});
