import { EuiBreadcrumbs } from '@elastic/eui';
import { useMatches } from 'react-router-dom';

export const Breadcrumbs = () => {
	const matches = useMatches();
	const crumbs = matches.map((match) => ({
		text: (match.handle as { name: string })?.name || '',
		href: match.pathname,
	}));

	return (
		<EuiBreadcrumbs
			breadcrumbs={crumbs}
			truncate={false}
			aria-label="Page breadcrumbs"
		/>
	);
};
