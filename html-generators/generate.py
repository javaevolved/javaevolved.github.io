#!/usr/bin/env python3
"""Generate HTML detail pages from JSON snippet files and slug-template.html."""

import json
import yaml
import glob
import os
import html
import re
from urllib.parse import quote

BASE_URL = "https://javaevolved.github.io"
TEMPLATE_FILE = "templates/slug-template.html"
WHY_CARD_TEMPLATE = "templates/why-card.html"
RELATED_CARD_TEMPLATE = "templates/related-card.html"
SOCIAL_SHARE_TEMPLATE = "templates/social-share.html"
DOC_LINK_TEMPLATE = "templates/doc-link.html"
INDEX_TEMPLATE = "templates/index.html"
INDEX_CARD_TEMPLATE = "templates/index-card.html"
CONTENT_DIR = "content"
SITE_DIR = "site"

CATEGORIES_FILE = "html-generators/categories.properties"


def load_category_display():
    """Load category display names from the properties file."""
    categories = {}
    with open(CATEGORIES_FILE) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            key, sep, value = line.partition('=')
            key, value = key.strip(), value.strip()
            if sep and key:
                categories[key] = value
    return categories


CATEGORY_DISPLAY = load_category_display()


def escape(text):
    """HTML-escape text for use in attributes and content."""
    return html.escape(text, quote=True)


def replace_tokens(template, replacements):
    """Replace {{token}} placeholders in a template string."""
    def replacer(m):
        key = m.group(1)
        return replacements.get(key, m.group(0))
    return re.sub(r"\{\{(\w+)\}\}", replacer, template)


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
    json_files = []
    for cat in CATEGORY_DISPLAY:
        json_files.extend(glob.glob(f"{CONTENT_DIR}/{cat}/*.json"))
        json_files.extend(glob.glob(f"{CONTENT_DIR}/{cat}/*.yaml"))
        json_files.extend(glob.glob(f"{CONTENT_DIR}/{cat}/*.yml"))
    json_files.sort()
    for path in json_files:
        with open(path) as f:
            if path.endswith(".yaml") or path.endswith(".yml"):
                data = yaml.safe_load(f)
            else:
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


def render_why_cards(why_card_template, why_list):
    """Render the 3 why-modern-wins cards."""
    cards = []
    for w in why_list:
        cards.append(
            replace_tokens(why_card_template, {
                "icon": w["icon"],
                "title": escape(w["title"]),
                "desc": escape(w["desc"]),
            })
        )
    return "\n".join(cards)


def render_related_card(related_card_template, related_data):
    """Render a single related pattern tip-card."""
    cat = related_data["category"]
    cat_display = CATEGORY_DISPLAY[cat]

    return replace_tokens(related_card_template, {
        "category": cat,
        "slug": related_data["slug"],
        "catDisplay": cat_display,
        "difficulty": related_data["difficulty"],
        "title": escape(related_data["title"]),
        "oldLabel": escape(related_data["oldLabel"]),
        "oldCode": escape(related_data["oldCode"]),
        "modernLabel": escape(related_data["modernLabel"]),
        "modernCode": escape(related_data["modernCode"]),
        "jdkVersion": related_data["jdkVersion"],
    })


def render_related_section(related_card_template, related_paths, all_snippets):
    """Render all related pattern cards."""
    cards = []
    for path in related_paths:
        if path in all_snippets:
            cards.append(render_related_card(related_card_template, all_snippets[path]))
    return "\n".join(cards)


def render_social_share(social_share_template, slug, title):
    """Render social share URLs using the old flat URL format."""
    page_url = f"{BASE_URL}/{slug}.html"
    share_text = f"{title} \u2013 java.evolved"

    encoded_url = quote(page_url, safe="")
    encoded_text = quote(share_text, safe="")

    return replace_tokens(social_share_template, {
        "encodedUrl": encoded_url,
        "encodedText": encoded_text,
    })


def render_doc_links(doc_link_template, docs):
    """Render documentation links."""
    return "\n".join(
        replace_tokens(doc_link_template, {
            "docTitle": escape(d["title"]),
            "docHref": d["href"],
        })
        for d in docs
    )


def _support_badge(state):
    return {"preview": "Preview", "experimental": "Experimental"}.get(state, "Available")


def _support_badge_class(state):
    return {"preview": "preview", "experimental": "experimental"}.get(state, "widely")


def render_index_card(index_card_template, data):
    """Render a single index page preview card."""
    cat = data["category"]
    return replace_tokens(index_card_template, {
        "category": cat,
        "slug": data["slug"],
        "catDisplay": CATEGORY_DISPLAY[cat],
        "title": escape(data["title"]),
        "oldCode": escape(data["oldCode"]),
        "modernCode": escape(data["modernCode"]),
        "jdkVersion": data["jdkVersion"],
    })


def generate_html(template, why_card_template, related_card_template,
                  social_share_template, doc_link_template, data, all_snippets):
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
        "supportDescription": escape(data["support"]["description"]),
        "supportBadge": _support_badge(data["support"]["state"]),
        "supportBadgeClass": _support_badge_class(data["support"]["state"]),
        "canonicalUrl": f"{BASE_URL}/{cat}/{slug}.html",
        "flatUrl": f"{BASE_URL}/{slug}.html",
        "titleJson": json_escape(data["title"]),
        "summaryJson": json_escape(data["summary"]),
        "categoryDisplayJson": json_escape(cat_display),
        "navArrows": render_nav_arrows(data),
        "whyCards": render_why_cards(why_card_template, data["whyModernWins"]),
        "docLinks": render_doc_links(doc_link_template, data.get("docs", [])),
        "relatedCards": render_related_section(related_card_template, data.get("related", []), all_snippets),
        "socialShare": render_social_share(social_share_template, slug, data["title"]),
    }

    return replace_tokens(template, replacements)


def main():
    template = load_template()
    why_card_template = open(WHY_CARD_TEMPLATE).read()
    related_card_template = open(RELATED_CARD_TEMPLATE).read()
    social_share_template = open(SOCIAL_SHARE_TEMPLATE).read()
    doc_link_template = open(DOC_LINK_TEMPLATE).read()
    index_template = open(INDEX_TEMPLATE).read()
    index_card_template = open(INDEX_CARD_TEMPLATE).read()
    all_snippets = load_all_snippets()
    print(f"Loaded {len(all_snippets)} snippets")

    for key, data in all_snippets.items():
        html_content = generate_html(
            template, why_card_template, related_card_template,
            social_share_template, doc_link_template, data, all_snippets
        ).strip()
        out_dir = os.path.join(SITE_DIR, data['category'])
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, f"{data['slug']}.html")
        with open(out_path, "w", newline="") as f:
            f.write(html_content)

    print(f"Generated {len(all_snippets)} HTML files")

    # Rebuild data/snippets.json from individual JSON files
    # This file is used at runtime by app.js for search
    snippets_list = []
    for key, data in all_snippets.items():
        entry = {k: v for k, v in data.items() if k not in ("_path", "prev", "next", "related")}
        snippets_list.append(entry)

    os.makedirs(os.path.join(SITE_DIR, "data"), exist_ok=True)
    with open(os.path.join(SITE_DIR, "data", "snippets.json"), "w") as f:
        json.dump(snippets_list, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"Rebuilt data/snippets.json with {len(snippets_list)} entries")

    # Generate index.html from template
    tip_cards = "\n".join(
        render_index_card(index_card_template, data)
        for data in all_snippets.values()
    )
    count = len(all_snippets)
    index_html = replace_tokens(index_template, {
        "tipCards": tip_cards,
        "snippetCount": str(count),
    })
    with open(os.path.join(SITE_DIR, "index.html"), "w") as f:
        f.write(index_html)

    print(f"Generated index.html with {count} cards")


if __name__ == "__main__":
    main()
