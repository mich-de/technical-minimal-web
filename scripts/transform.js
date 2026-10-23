#!/usr/bin/env node
/**
 * Technical Minimal Web Transformer
 * Transforms basic pages or JSON configs into the Teenage Engineering / Nothing inspired
 * mich-de.github.io style web showcase.
 */

const fs = require('fs');
const path = require('path');
const { renderDotMatrix } = require('./dot-matrix');

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function generateSite(data) {
  const templatePath = path.join(__dirname, '..', 'templates', 'web', 'template.html');
  let tpl = fs.readFileSync(templatePath, 'utf8');

  const title = data.title || 'Technical Minimal Showcase';
  const description = data.description || 'Curated projects and highlights.';
  const entityName = data.entityName || data.user || 'SYSTEM';
  const handleLink = data.handleLink || (data.user ? `https://github.com/${data.user}` : '#');
  const heroTitle = data.heroTitle || 'Engineered with';
  const heroHot = data.heroHotTitle || 'precision.';
  const heroLead = data.heroLead || 'A curated showcase built with technical rigor, strict typography, and zero fluff.';
  const splashText = data.splashText || 'SYSTEM.';

  const sections = data.sections || [];
  let totalItems = 0;
  sections.forEach(s => {
    totalItems += (s.items || []).length;
  });

  const statsLabel = `${sections.length} sections · ${totalItems} items`;
  const footerLeft = data.footerLeft || `Created for ${escapeHtml(entityName)}`;

  // Build Splash
  const splashSvg = renderDotMatrix(splashText);
  const splashSteps = sections.map(s => s.title || s.id);
  if (splashSteps.length === 0) splashSteps.push('SYSTEM');
  
  const splashBars = sections.map((s, i) => {
    const hue = s.hue !== undefined ? s.hue : (i * 45) % 360;
    return `<i style="--h:${hue};--i:${i}"></i>`;
  }).join('');

  const splashTotal = String(splashSteps.length).padStart(2, '0');

  // Build Index nav
  const indexNavItems = sections.map((s, i) => {
    const hue = s.hue !== undefined ? s.hue : (i * 45) % 360;
    const count = (s.items || []).length;
    return `      <li style="--h:${hue}"><a href="#${escapeHtml(s.id)}"><i class="sw" aria-hidden="true"></i>${escapeHtml(s.title)} <span class="n mono">${count}</span></a></li>`;
  }).join('\n');

  // Build Section blocks
  const sectionBlocks = sections.map((s, i) => {
    const hue = s.hue !== undefined ? s.hue : (i * 45) % 360;
    const icon = s.icon || '#i-cube';
    const rows = (s.items || []).map(item => {
      const href = item.url || item.link || '#';
      const slug = item.slug || item.name || '';
      const name = item.title || item.name || '';
      const desc = item.description || '';
      const tag = item.tag || item.kind || 'VIEW';
      const isPlay = tag.toLowerCase() === 'play' || tag.toLowerCase() === 'live' || tag.toLowerCase() === 'hot';
      const playClass = isPlay ? ' highlight' : '';

      return `      <li>
        <a class="row" href="${escapeHtml(href)}">
          <span class="slug mono">${escapeHtml(slug)}</span>
          <span class="info">
            <span class="t">${escapeHtml(name)}</span>
            ${desc ? `<span class="d">${escapeHtml(desc)}</span>` : ''}
          </span>
          <span class="k mono${playClass}">${escapeHtml(tag)}</span>
          <svg class="arr" viewBox="0 0 16 16" aria-hidden="true"><use href="#i-arrow"/></svg>
        </a>
      </li>`;
    }).join('\n');

    return `<section class="section-block" id="${escapeHtml(s.id)}" style="--h:${hue}">
  <div class="section-head">
    <div class="stick">
      <div class="top">
        <svg class="ico" aria-hidden="true"><use href="${escapeHtml(icon)}"/></svg>
        <span class="mono">${String(i + 1).padStart(2, '0')} // ${escapeHtml(s.id.toUpperCase())}</span>
      </div>
      <h2>${escapeHtml(s.title)}</h2>
      ${s.description ? `<p>${escapeHtml(s.description)}</p>` : ''}
    </div>
  </div>
  <ul class="rows">
${rows}
  </ul>
</section>`;
  }).join('\n\n');

  // Replace placeholders
  tpl = tpl
    .replace(/\{\{PAGE_TITLE\}\}/g, escapeHtml(title))
    .replace(/\{\{PAGE_DESCRIPTION\}\}/g, escapeHtml(description))
    .replace(/\{\{ENTITY_NAME\}\}/g, escapeHtml(entityName))
    .replace(/\{\{HANDLE_LINK\}\}/g, escapeHtml(handleLink))
    .replace(/\{\{STATS_LABEL\}\}/g, escapeHtml(statsLabel))
    .replace(/\{\{HERO_TITLE\}\}/g, escapeHtml(heroTitle))
    .replace(/\{\{HERO_HOT_TITLE\}\}/g, escapeHtml(heroHot))
    .replace(/\{\{HERO_LEAD\}\}/g, escapeHtml(heroLead))
    .replace(/\{\{SPLASH_SVG\}\}/g, splashSvg)
    .replace(/\{\{SPLASH_BARS\}\}/g, splashBars)
    .replace(/\{\{SPLASH_TOTAL\}\}/g, splashTotal)
    .replace(/\{\{SPLASH_STEPS_JSON\}\}/g, JSON.stringify(splashSteps))
    .replace(/\{\{INDEX_NAV_ITEMS\}\}/g, indexNavItems)
    .replace(/\{\{SECTION_BLOCKS\}\}/g, sectionBlocks)
    .replace(/\{\{FOOTER_LEFT\}\}/g, escapeHtml(footerLeft));

  return tpl;
}

module.exports = { generateSite };

if (require.main === module) {
  const args = process.argv.slice(2);
  if (args.length === 0) {
    console.log('Usage: node transform.js <config.json> [output.html]');
    process.exit(1);
  }

  const configPath = path.resolve(args[0]);
  const outPath = args[1] ? path.resolve(args[1]) : path.join(path.dirname(configPath), 'index.html');
  const data = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  const html = generateSite(data);
  fs.writeFileSync(outPath, html, 'utf8');
  console.log(`Generated: ${outPath}`);
}
