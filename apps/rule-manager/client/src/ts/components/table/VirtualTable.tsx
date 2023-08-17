/** Context for cross component communication */
import { FixedSizeList, FixedSizeListProps } from 'react-window';
import React, { useContext, useRef, useState } from 'react';
import { EuiTable, EuiTableBody } from '@elastic/eui';

export const VirtualTableContext = React.createContext<{
	top: number;
	setTop: (top: number) => void;
	header: React.ReactNode;
	footer: React.ReactNode;
}>({
	top: 0,
	setTop: (value: number) => {},
	header: <></>,
	footer: <></>,
});

const VirtualTableInner = React.forwardRef<
	HTMLDivElement,
	React.HTMLProps<HTMLDivElement>
>(({ children, ...rest }) => {
	const { header, footer } = useContext(VirtualTableContext);
	return (
		<EuiTable {...rest}>
			{header}
			<EuiTableBody>{children}</EuiTableBody>
			{footer}
		</EuiTable>
	);
});

/**
 * An EUI table with virtual rows. We must use this approach when working with EUI table components,
 * because react-window requires an inner and outer component to render its virtualised list,
 * and the table structure does not play nicely with that by default. Draws heavily on
 * https://codesandbox.io/s/react-window-with-table-elements-d861o?file=/src/index.tsx.
 */
export const EuiVirtualTable = ({
	row,
	header,
	footer,
	innerElementType,
	itemSize,
	...rest
}: {
	header?: React.ReactNode;
	footer?: React.ReactNode;
	row: FixedSizeListProps['children'];
} & Omit<FixedSizeListProps, 'children'>) => {
	const listRef = useRef<FixedSizeList | null>();
	const [top, setTop] = useState(0);

	return (
		<VirtualTableContext.Provider value={{ top, setTop, header, footer }}>
			<FixedSizeList
				{...rest}
				itemSize={itemSize}
				innerElementType={VirtualTableInner}
				onItemsRendered={(props) => {
					const style =
						listRef.current &&
						// @ts-ignore private method access
						listRef.current._getItemStyle(props.overscanStartIndex);
					setTop((style && style.top) || 0);

					// Call the original callback
					rest.onItemsRendered && rest.onItemsRendered(props);
				}}
				ref={(el) => (listRef.current = el)}
			>
				{row}
			</FixedSizeList>
		</VirtualTableContext.Provider>
	);
};
