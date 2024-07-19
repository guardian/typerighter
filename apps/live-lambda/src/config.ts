import * as process from 'process';

export interface Config {
	stack: string;
	stage: string;
	app: string;
}

function getEnvOrThrow(key: string): string {
	const value = process.env[key];
	if (value === undefined) {
		throw new Error(`Environment variable ${key} is not set`);
	}
	return value;
}

export function getConfig(): Config {
	return {
		stack: getEnvOrThrow('STACK'),
		stage: getEnvOrThrow('STAGE'),
		app: getEnvOrThrow('APP'),
	};
}
