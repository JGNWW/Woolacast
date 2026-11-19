// Rendert de schermen naar renders/*.png: node render.mjs
import { createRequire } from 'node:module';
import { readdirSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const here = path.dirname(fileURLToPath(import.meta.url));
// Playwright staat vaak globaal; require kijkt ook in NODE_PATH.
const { chromium } = createRequire(import.meta.url)('playwright');
mkdirSync(path.join(here, 'renders'), { recursive: true });
const browser = await chromium.launch();
for (const file of readdirSync(here).filter(f => f.endsWith('.html'))) {
  const page = await browser.newPage({ viewport: { width: 400, height: 800 }, deviceScaleFactor: 2 });
  await page.goto('file://' + path.join(here, file));
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: path.join(here, 'renders', file.replace('.html', '.png')), fullPage: true });
  await page.close();
}
await browser.close();
