#!/usr/bin/env python3
"""Generate HTML detail pages from JSON snippet files and slug-template.html."""

import json
import glob
import os
import html
import re
from urllib.parse import quote

BASE_URL = "https://javaevolved.github.io"
TEMPLATE_FILE = "slug-template.html"

CATEGORY_DISPLAY = {
    "language": "Language",
    "collections": "Collections",
    "strings": "Strings",
    "streams": "Streams",
    "concurrency": "Concurrency",
    "io": "I/O",
    "errors": "Errors",
    "datetime": "Date/Time",
    "security": "Security",
    "tooling": "Tooling",
}


def escape(text):
    """HTML-escape text for use in attributes and content."""
    return html.escape(text, quote=True)


def json_escape(text):
    """Escape text for embedding in JSON strings inside ld+json blocks.

    Uses ASCII-only encoding to match the original HTML files which use
    \\uXXXX escapes for non-ASCII characters in ld+json blocks.
    """
    return json.dumps(text, ensure_ascii=True)[1:-1]


def load_template():
    """Load the external HTML template."""
    with open(TEMPLATE_FILE) as f:
        return f.read()


def load_all_snippets():
    """Load all JSON snippet files, keyed by category/slug."""
    snippets = {}
    categories = [
        "language", "collections", "strings", "streams", "concurrency",
        "io", "errors", "datetime", "security", "tooling",
    ]
    json_files = []
    for cat in categories:
        json_files.extend(sorted(glob.glob(f"{cat}/*.json")))
    for path in json_files:
        with open(path) as f:
            data = json.load(f)
        key = f"{data['category']}/{data['slug']}"
        data["_path"] = key
        snippets[key] = data
    return snippets


def render_nav_arrows(data):
    """Render prev/next navigation arrows."""
    parts = []
    if data.get("prev"):
        parts.append(
            f'<a href="/{data["prev"]}.html" aria-label="Previous pattern">←</a>'
        )
    else:
        parts.append('<span class="nav-arrow-disabled">←</span>')
    if data.get("next"):
        parts.append(
            f'<a href="/{data["next"]}.html" aria-label="Next pattern">→</a>'
        )
    else:
        parts.append("")
    return "\n          ".join(parts)


def render_why_cards(why_list):
    """Render the 3 why-modern-wins cards."""
    cards = []
    for w in why_list:
        cards.append(
            f"""        <div class="why-card">
          <div class="why-icon">{w['icon']}</div>
          <h3>{escape(w['title'])}</h3>
          <p>{escape(w['desc'])}</p>
        </div>"""
        )
    return "\n".join(cards)


def render_related_card(related_data):
    """Render a single related pattern tip-card."""
    cat = related_data["category"]
    slug = related_data["slug"]
    cat_display = CATEGORY_DISPLAY[cat]
    path = f"{cat}/{slug}"

    return f"""        <a href="/{path}.html" class="tip-card">
          <div class="tip-card-body">
            <div class="tip-card-header">
              <div class="tip-badges">
                <span class="badge {cat}">{cat_display}</span>
                <span class="badge {related_data['difficulty']}">{related_data['difficulty']}</span>
              </div>
            </div>
            <h3>{escape(related_data['title'])}</h3>
          </div>
          <div class="card-code">
            <div class="card-code-layer old-layer">
              <div class="mini-label">{escape(related_data['oldLabel'])}</div>
              <pre class="code-text">{escape(related_data['oldCode'])}</pre>
            </div>
            <div class="card-code-layer modern-layer">
              <div class="mini-label">{escape(related_data['modernLabel'])}</div>
              <pre class="code-text">{escape(related_data['modernCode'])}</pre>
            </div>
            <span class="hover-hint">Hover to see modern ➜</span>
          </div>
          <div class="tip-card-footer">
            <span class="browser-support"><span class="dot"></span>JDK {related_data['jdkVersion']}+</span>
            <span class="arrow-link">→</span>
          </div>
        </a>"""


def render_related_section(related_paths, all_snippets):
    """Render all related pattern cards."""
    cards = []
    for path in related_paths:
        if path in all_snippets:
            cards.append(render_related_card(all_snippets[path]))
    return "\n".join(cards)


def render_social_share(slug, title):
    """Render social share URLs using the old flat URL format."""
    page_url = f"{BASE_URL}/{slug}.html"
    share_text = f"{title} \u2013 java.evolved"

    encoded_url = quote(page_url, safe="")
    encoded_text = quote(share_text, safe="")

    x_url = f"https://x.com/intent/tweet?url={encoded_url}&text={encoded_text}"
    bsky_url = f"https://bsky.app/intent/compose?text={encoded_text}%20{encoded_url}"
    li_url = f"https://www.linkedin.com/sharing/share-offsite/?url={encoded_url}"
    reddit_url = (
        f"https://www.reddit.com/submit?url={encoded_url}&title={encoded_text}"
    )

    return f"""  <div class="social-share">
    <span class="share-label">Share</span>
    <a href="{x_url}" target="_blank" rel="noopener" class="share-btn share-x" aria-label="Share on X">𝕏</a>
    <a href="{bsky_url}" target="_blank" rel="noopener" class="share-btn share-bsky" aria-label="Share on Bluesky">🦋</a>
    <a href="{li_url}" target="_blank" rel="noopener" class="share-btn share-li" aria-label="Share on LinkedIn">in</a>
    <a href="{reddit_url}" target="_blank" rel="noopener" class="share-btn share-reddit" aria-label="Share on Reddit">⬡</a>
  </div>"""


def generate_html(template, data, all_snippets):
    """Generate the full HTML page for a snippet by rendering the template."""
    cat = data["category"]
    slug = data["slug"]
    cat_display = CATEGORY_DISPLAY[cat]

    # Build the substitution map
    replacements = {
        "title": escape(data["title"]),
        "summary": escape(data["summary"]),
        "slug": slug,
        "category": cat,
        "categoryDisplay": cat_display,
        "difficulty": data["difficulty"],
        "jdkVersion": data["jdkVersion"],
        "oldLabel": escape(data["oldLabel"]),
        "modernLabel": escape(data["modernLabel"]),
        "oldCode": escape(data["oldCode"]),
        "modernCode": escape(data["modernCode"]),
        "oldApproach": escape(data["oldApproach"]),
        "modernApproach": escape(data["modernApproach"]),
        "explanation": escape(data["explanation"]),
        "support": escape(data["support"]),
        "canonicalUrl": f"{BASE_URL}/{cat}/{slug}.html",
        "flatUrl": f"{BASE_URL}/{slug}.html",
        "titleJson": json_escape(data["title"]),
        "summaryJson": json_escape(data["summary"]),
        "categoryDisplayJson": json_escape(cat_display),
        "navArrows": render_nav_arrows(data),
        "whyCards": render_why_cards(data["whyModernWins"]),
        "relatedCards": render_related_section(data.get("related", []), all_snippets),
        "socialShare": render_social_share(slug, data["title"]),
    }

    # Replace all {{placeholder}} tokens
    def replace_token(match):
        key = match.group(1)
        if key in replacements:
            return str(replacements[key])
        return match.group(0)

    return re.sub(r"\{\{(\w+)\}\}", replace_token, template)


def main():
    template = load_template()
    all_snippets = load_all_snippets()
    print(f"Loaded {len(all_snippets)} snippets")

    for key, data in all_snippets.items():
        html_content = generate_html(template, data, all_snippets).strip()
        out_path = f"{data['category']}/{data['slug']}.html"
        with open(out_path, "w", newline="") as f:
            f.write(html_content)

    print(f"Generated {len(all_snippets)} HTML files")

    # Rebuild data/snippets.json from individual JSON files
    # This file is used at runtime by app.js for search
    snippets_list = []
    for key, data in all_snippets.items():
        entry = {k: v for k, v in data.items() if k not in ("_path", "prev", "next", "related")}
        snippets_list.append(entry)

    os.makedirs("data", exist_ok=True)
    with open("data/snippets.json", "w") as f:
        json.dump(snippets_list, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"Rebuilt data/snippets.json with {len(snippets_list)} entries")


if __name__ == "__main__":
    main()
