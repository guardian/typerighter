import { EuiBreadcrumbs } from '@elastic/eui';
import { useMatches } from 'react-router-dom';
import { getNameFromRouteMatch, RouteHandle, RouteNameFn } from './Page';

export const Breadcrumbs = () => {
	const matches = useMatches();
	const crumbs = matches.map((match) => ({
		text: getNameFromRouteMatch(match),
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
