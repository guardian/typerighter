import { getConfig } from './config';

export async function main() {
	const { stage, app } = getConfig();
	const msg = `Hello from ${app} in ${stage}! The time is ${new Date().toString()}`;
	console.log(msg);
	return Promise.resolve(msg);
}
