#!/usr/bin/env python3
"""Generate sitemap.xml for MkDocs documentation."""

import os
import sys
from pathlib import Path
from datetime import datetime
from urllib.parse import urljoin

def generate_sitemap(site_dir: str, site_url: str) -> None:
    """Generate sitemap.xml from built HTML files."""

    site_path = Path(site_dir)
    if not site_path.exists():
        print(f"Error: {site_dir} does not exist")
        sys.exit(1)

    # Ensure site_url ends with /
    if not site_url.endswith('/'):
        site_url += '/'

    # Find all HTML files
    html_files = sorted(site_path.glob('**/*.html'))

    if not html_files:
        print(f"Warning: No HTML files found in {site_dir}")
        return

    # Build sitemap XML
    sitemap_lines = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ]

    for html_file in html_files:
        # Get relative path
        rel_path = html_file.relative_to(site_path)

        # Convert path to URL (convert index.html to /)
        url_path = str(rel_path).replace(os.sep, '/')
        if url_path == 'index.html':
            url_path = ''
        elif url_path.endswith('/index.html'):
            url_path = url_path[:-10]  # Remove /index.html

        full_url = urljoin(site_url, url_path)

        # Get last modified time
        mtime = html_file.stat().st_mtime
        lastmod = datetime.fromtimestamp(mtime).isoformat()

        sitemap_lines.append('  <url>')
        sitemap_lines.append(f'    <loc>{full_url}</loc>')
        sitemap_lines.append(f'    <lastmod>{lastmod}</lastmod>')
        sitemap_lines.append('    <changefreq>weekly</changefreq>')
        sitemap_lines.append('    <priority>0.8</priority>')
        sitemap_lines.append('  </url>')

    sitemap_lines.append('</urlset>')

    # Write sitemap
    sitemap_path = site_path / 'sitemap.xml'
    sitemap_path.write_text('\n'.join(sitemap_lines))

    print(f"✅ Generated sitemap.xml with {len(html_files)} URLs")
    print(f"   Location: {sitemap_path}")

if __name__ == '__main__':
    site_dir = sys.argv[1] if len(sys.argv) > 1 else 'site'
    site_url = sys.argv[2] if len(sys.argv) > 2 else 'https://akshay2211.github.io/DrawBox'

    generate_sitemap(site_dir, site_url)