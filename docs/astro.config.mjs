// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
	site: 'https://gradleup.com',
	base: '/librarian',
	integrations: [
		starlight({
			title: 'Librarian',
			editLink: {
				baseUrl: 'https://github.com/GradleUp/librarian/edit/main/docs/',
			},
			logo: {
				src: './src/assets/logo.svg'
			},
			social: {
				github: 'https://github.com/GradleUp/librarian',
			},
			sidebar: [
				{ label: 'Welcome', link: '/', },
				{ label: 'CLI', link: '/cli' },
				// {
				// 	label: 'Guides',
				// 	items: [
				// 		// Each item here is one entry in the navigation menu.
				// 		{ label: 'Example Guide', slug: 'guides/example' },
				// 	],
				// },
				// {
				// 	label: 'Reference',
				// 	autogenerate: { directory: 'reference' },
				// },
			],
		}),
	],
});
