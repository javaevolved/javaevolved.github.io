#!/usr/bin/env python3
"""
Generate HTML detail pages from JSON snippet files and slug-template.html.
Python equivalent of generate.java — produces identical output.
"""

import argparse
import copy
import json
import yaml
import glob
import os
import html as html_mod
import re
from collections import OrderedDict
from urllib.parse import quote

BASE_URL = "https://javaevolved.github.io"
CONTENT_DIR = "content"
SITE_DIR = "site"
TRANSLATIONS_DIR = "translations"
CATEGORIES_FILE = "html-generators/categories.properties"
LOCALES_FILE = "html-generators/locales.properties"
GITHUB_ISSUES_URL = "https://github.com/javaevolved/javaevolved.github.io/issues/new"

TOKEN_RE = re.compile(r"\{\{([\w.]+)\}\}")

EXCLUDED_KEYS = {"_path", "prev", "next", "related", "proofCode"}

TRANSLATABLE_FIELDS = {
    "title", "summary", "explanation", "oldApproach", "modernApproach",
    "whyModernWins", "support",
}


# ---------------------------------------------------------------------------
# Properties / config loaders
# ---------------------------------------------------------------------------

def _load_properties(path):
    """Load a .properties file into an OrderedDict, preserving insertion order."""
    props = OrderedDict()
    with open(path) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            idx = line.find("=")
            if idx > 0:
                props[line[:idx].strip()] = line[idx + 1:].strip()
    return props


CATEGORY_DISPLAY = _load_properties(CATEGORIES_FILE)
LOCALES = _load_properties(LOCALES_FILE)


# ---------------------------------------------------------------------------
# File helpers (multi-format: .json, .yaml, .yml)
# ---------------------------------------------------------------------------

def _find_with_extensions(directory, base_name):
    """Return the first existing file matching base_name.{json,yaml,yml} in directory."""
    for ext in ("json", "yaml", "yml"):
        p = os.path.join(directory, f"{base_name}.{ext}")
        if os.path.isfile(p):
            return p
    return None


def _read_auto(path):
    """Read a JSON or YAML file based on its extension."""
    with open(path) as f:
        if path.endswith(".yaml") or path.endswith(".yml"):
            return yaml.safe_load(f)
        return json.load(f)


# ---------------------------------------------------------------------------
# UI strings (i18n)
# ---------------------------------------------------------------------------

def _flatten(obj, prefix=""):
    """Flatten a nested dict into dot-separated keys."""
    flat = {}
    for k, v in obj.items():
        key = f"{prefix}.{k}" if prefix else k
        if isinstance(v, dict):
            flat.update(_flatten(v, key))
        else:
            flat[key] = str(v)
    return flat


def load_strings(locale):
    """Load UI strings for a locale with English fallback for missing keys."""
    en_path = _find_with_extensions(os.path.join(TRANSLATIONS_DIR, "strings"), "en")
    if not en_path:
        raise FileNotFoundError("No English strings file found")
    en_strings = _flatten(_read_auto(en_path))

    if locale == "en":
        return en_strings

    locale_path = _find_with_extensions(os.path.join(TRANSLATIONS_DIR, "strings"), locale)
    if not locale_path:
        print(f"[WARN] strings/{locale}.{{json,yaml,yml}} not found — using all English strings")
        return dict(en_strings)

    locale_strings = _flatten(_read_auto(locale_path))
    merged = dict(en_strings)
    for key, value in locale_strings.items():
        if key in en_strings:
            merged[key] = value
    # Warn about missing keys
    locale_file = os.path.basename(locale_path)
    for key in en_strings:
        if key not in locale_strings:
            print(f'[WARN] {locale_file}: missing key "{key}" — using English fallback')
    return merged


# ---------------------------------------------------------------------------
# Snippet helpers
# ---------------------------------------------------------------------------

def _get(data, field):
    return data[field]


def _opt(data, field):
    v = data.get(field)
    return v if v is not None else None


def _key(data):
    return f"{data['category']}/{data['slug']}"


def _cat_display(data):
    return CATEGORY_DISPLAY[data["category"]]


# ---------------------------------------------------------------------------
# Escape helpers
# ---------------------------------------------------------------------------

def escape(text):
    """HTML-escape text for use in attributes and content."""
    if text is None:
        return ""
    return html_mod.escape(str(text), quote=True)


def json_escape(text):
    """Escape text for embedding in JSON strings inside ld+json blocks.
    Uses ASCII-only encoding with \\uXXXX escapes for non-ASCII characters.
    """
    return json.dumps(text, ensure_ascii=True)[1:-1]


def js_escape(s):
    """Escape a string for embedding inside a JS double-quoted string."""
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")


def url_encode(s):
    return quote(s, safe="")


# ---------------------------------------------------------------------------
# Token replacement (multi-pass, supports dotted keys)
# ---------------------------------------------------------------------------

def replace_tokens(template, replacements):
    """Replace {{token}} placeholders; up to 3 passes for nested tokens."""
    result = template
    for _ in range(3):
        found = False
        def replacer(m):
            nonlocal found
            key = m.group(1)
            val = replacements.get(key)
            if val is not None:
                found = True
                return val
            return m.group(0)
        result = TOKEN_RE.sub(replacer, result)
        if not found:
            break
    return result


# ---------------------------------------------------------------------------
# Load all English snippets
# ---------------------------------------------------------------------------

def load_all_snippets():
    """Load all content snippet files, keyed by category/slug, in sorted order."""
    snippets = OrderedDict()
    for cat in CATEGORY_DISPLAY:
        cat_dir = os.path.join(CONTENT_DIR, cat)
        if not os.path.isdir(cat_dir):
            continue
        files = []
        for ext in ("json", "yaml", "yml"):
            files.extend(glob.glob(os.path.join(cat_dir, f"*.{ext}")))
        files.sort()
        for path in files:
            data = _read_auto(path)
            data["_path"] = _key(data)
            snippets[_key(data)] = data
    return snippets


# ---------------------------------------------------------------------------
# Translation merging
# ---------------------------------------------------------------------------

def resolve_snippet(english_snippet, locale):
    """Overlay translated content onto the English base for a given locale."""
    if locale == "en":
        return english_snippet

    translated_dir = os.path.join(TRANSLATIONS_DIR, "content", locale, english_snippet["category"])
    translated_path = _find_with_extensions(translated_dir, english_snippet["slug"])
    if not translated_path:
        return english_snippet

    try:
        translated = _read_auto(translated_path)
        merged = copy.deepcopy(english_snippet)
        for field in TRANSLATABLE_FIELDS:
            if field in translated:
                if field == "support" and isinstance(translated["support"], dict):
                    if "description" in translated["support"]:
                        merged["support"]["description"] = translated["support"]["description"]
                else:
                    merged[field] = translated[field]
        return merged
    except Exception:
        print(f"[WARN] Failed to load {translated_path} — using English")
        return english_snippet


# ---------------------------------------------------------------------------
# Badge / display helpers
# ---------------------------------------------------------------------------

def support_badge(state, strings):
    return {
        "preview": strings.get("support.preview", "Preview"),
        "experimental": strings.get("support.experimental", "Experimental"),
    }.get(state, strings.get("support.available", "Available"))


def support_badge_class(state):
    return {"preview": "preview", "experimental": "experimental"}.get(state, "widely")


def difficulty_display(difficulty, strings):
    return strings.get(f"difficulty.{difficulty}", difficulty)


# ---------------------------------------------------------------------------
# Render helpers
# ---------------------------------------------------------------------------

def render_nav_arrows(data, locale):
    """Render prev/next navigation arrows with locale-aware paths."""
    prefix = "" if locale == "en" else f"/{locale}"
    prev_val = _opt(data, "prev")
    next_val = _opt(data, "next")
    prev_html = (
        f'<a href="{prefix}/{prev_val}.html" aria-label="Previous pattern">←</a>'
        if prev_val
        else '<span class="nav-arrow-disabled">←</span>'
    )
    next_html = (
        f'<a href="{prefix}/{next_val}.html" aria-label="Next pattern">→</a>'
        if next_val
        else ""
    )
    return prev_html + "\n          " + next_html


def render_why_cards(tpl, why_list):
    """Render the 3 why-modern-wins cards."""
    cards = []
    for w in why_list:
        cards.append(replace_tokens(tpl, {
            "icon": w["icon"],
            "title": escape(w["title"]),
            "desc": escape(w["desc"]),
        }))
    return "\n".join(cards)


def render_doc_links(tpl, docs):
    """Render documentation links."""
    return "\n".join(
        replace_tokens(tpl, {
            "docTitle": escape(d["title"]),
            "docHref": d["href"],
        })
        for d in docs
    )


def render_related_card(tpl, rel, locale, strings):
    """Render a single related pattern tip-card."""
    related_href = (
        f"/{rel['category']}/{rel['slug']}.html"
        if locale == "en"
        else f"/{locale}/{rel['category']}/{rel['slug']}.html"
    )
    return replace_tokens(tpl, {
        "category": rel["category"],
        "slug": rel["slug"],
        "catDisplay": _cat_display(rel),
        "difficulty": rel["difficulty"],
        "difficultyDisplay": difficulty_display(rel["difficulty"], strings),
        "title": escape(rel["title"]),
        "oldLabel": escape(rel["oldLabel"]),
        "oldCode": escape(rel["oldCode"]),
        "modernLabel": escape(rel["modernLabel"]),
        "modernCode": escape(rel["modernCode"]),
        "jdkVersion": rel["jdkVersion"],
        "relatedHref": related_href,
        "cards.hoverHintRelated": strings.get("cards.hoverHintRelated", "Hover to see modern ➜"),
    })


def render_related_section(tpl, data, all_snippets, locale, strings):
    """Render all related pattern cards."""
    related = data.get("related", [])
    cards = []
    for path in related:
        if path in all_snippets:
            cards.append(render_related_card(tpl, all_snippets[path], locale, strings))
    return "\n".join(cards)


def render_social_share(tpl, slug, title, strings):
    """Render social share URLs."""
    encoded_url = url_encode(f"{BASE_URL}/{slug}.html")
    encoded_text = url_encode(f"{title} \u2013 java.evolved")
    return replace_tokens(tpl, {
        "encodedUrl": encoded_url,
        "encodedText": encoded_text,
        "share.label": strings.get("share.label", "Share"),
    })


def render_index_card(tpl, data, locale, strings):
    """Render a single index page preview card."""
    card_href = (
        f"/{data['category']}/{data['slug']}.html"
        if locale == "en"
        else f"/{locale}/{data['category']}/{data['slug']}.html"
    )
    return replace_tokens(tpl, {
        "category": data["category"],
        "slug": data["slug"],
        "catDisplay": _cat_display(data),
        "title": escape(data["title"]),
        "oldCode": escape(data["oldCode"]),
        "modernCode": escape(data["modernCode"]),
        "jdkVersion": data["jdkVersion"],
        "cardHref": card_href,
        "cards.old": strings.get("cards.old", "Old"),
        "cards.modern": strings.get("cards.modern", "Modern"),
        "cards.hoverHint": strings.get("cards.hoverHint", "hover to see modern →"),
        "cards.learnMore": strings.get("cards.learnMore", "learn more"),
    })


# ---------------------------------------------------------------------------
# Locale picker, hreflang, i18n script
# ---------------------------------------------------------------------------

def render_locale_picker(current_locale):
    """Render the locale picker dropdown HTML."""
    lines = []
    lines.append('        <div class="locale-picker" id="localePicker">')
    lines.append('          <button type="button" class="locale-toggle" aria-haspopup="listbox" aria-expanded="false"')
    lines.append('                  aria-label="Select language">🌐</button>')
    lines.append('          <ul role="listbox" aria-label="Language">')
    for loc, name in LOCALES.items():
        selected = loc == current_locale
        cls = ' class="active"' if selected else ""
        lines.append(f'            <li role="option" data-locale="{loc}" aria-selected="{str(selected).lower()}"{cls}>{name}</li>')
    lines.append("          </ul>")
    lines.append("        </div>")
    return "\n".join(lines)


def render_hreflang_links(path_part, slug):
    """Render hreflang <link> tags for all locales."""
    lines = []
    for loc in LOCALES:
        if slug == "index":
            href = f"{BASE_URL}/" if loc == "en" else f"{BASE_URL}/{loc}/"
        else:
            href = (
                f"{BASE_URL}/{path_part}{slug}.html"
                if loc == "en"
                else f"{BASE_URL}/{loc}/{path_part}{slug}.html"
            )
        lines.append(f'  <link rel="alternate" hreflang="{loc}" href="{href}">')
    # x-default points to English
    default_href = (
        f"{BASE_URL}/" if slug == "index"
        else f"{BASE_URL}/{path_part}{slug}.html"
    )
    lines.append(f'  <link rel="alternate" hreflang="x-default" href="{default_href}">')
    return "\n".join(lines)


def render_i18n_script(strings, locale):
    """Render the i18n script block for client-side JS."""
    locale_array = ", ".join(f'"{loc}"' for loc in LOCALES)
    return (
        "<script>\n"
        "  window.i18n = {\n"
        f'    locale: "{locale}",\n'
        f"    availableLocales: [{locale_array}],\n"
        f'    searchPlaceholder: "{js_escape(strings.get("search.placeholder", "Search snippets…"))}",\n'
        f'    noResults: "{js_escape(strings.get("search.noResults", "No results found."))}",\n'
        f'    copied: "{js_escape(strings.get("copy.copied", "Copied!"))}",\n'
        f'    expandAll: "{js_escape(strings.get("view.expandAll", "Expand All"))}",\n'
        f'    collapseAll: "{js_escape(strings.get("view.collapseAll", "Collapse All"))}",\n'
        f'    hoverHint: "{js_escape(strings.get("cards.hoverHint", "hover to see modern →"))}",\n'
        f'    touchHint: "{js_escape(strings.get("cards.touchHint", "👆 tap or swipe →"))}"\n'
        "  };\n"
        "</script>"
    )


# ---------------------------------------------------------------------------
# Contribute URLs
# ---------------------------------------------------------------------------

def build_contribute_urls(data, locale, locale_name):
    """Build GitHub issue template URLs for contribute links."""
    title = data["title"]
    category = data["category"]
    slug = data["slug"]

    code_url = (
        f"{GITHUB_ISSUES_URL}?template=code-issue.yml"
        f"&title={url_encode(f'[Code Issue] {title}')}"
        f"&category={url_encode(category)}"
        f"&slug={url_encode(slug)}"
    )

    clean_locale_name = re.sub(r"^[^\w]", "", locale_name, flags=re.UNICODE)
    # Strip leading non-letter chars (match Java's ^[^\p{L}]+)
    clean_locale_name = re.sub(r"^[^a-zA-Z\u00C0-\u024F\u0400-\u04FF\u0600-\u06FF\u3000-\u9FFF\uAC00-\uD7AF]+", "", locale_name)

    trans_url = (
        f"{GITHUB_ISSUES_URL}?template=translation-issue.yml"
        f"&title={url_encode(f'[Translation] {title} ({clean_locale_name})')}"
        f"&locale={url_encode(locale)}"
        f"&pattern={url_encode(slug)}"
        f"&area={url_encode('Pattern content')}"
    )

    suggest_url = f"{GITHUB_ISSUES_URL}?template=new-pattern.yml"

    return {
        "contributeCodeIssueUrl": code_url,
        "contributeTranslationIssueUrl": trans_url,
        "contributeSuggestUrl": suggest_url,
    }


# ---------------------------------------------------------------------------
# HTML generation
# ---------------------------------------------------------------------------

def generate_html(templates, data, all_snippets, extra_tokens, locale):
    """Generate the full HTML page for a snippet by rendering the template."""
    is_english = locale == "en"
    cat = data["category"]
    slug = data["slug"]
    cat_display = _cat_display(data)

    canonical_url = (
        f"{BASE_URL}/{cat}/{slug}.html"
        if is_english
        else f"{BASE_URL}/{locale}/{cat}/{slug}.html"
    )

    tokens = dict(extra_tokens)
    tokens.update({
        "title": escape(data["title"]),
        "summary": escape(data["summary"]),
        "slug": slug,
        "category": cat,
        "categoryDisplay": cat_display,
        "difficulty": data["difficulty"],
        "difficultyDisplay": difficulty_display(data["difficulty"], extra_tokens),
        "jdkVersion": data["jdkVersion"],
        "oldLabel": escape(data["oldLabel"]),
        "modernLabel": escape(data["modernLabel"]),
        "oldCode": escape(data["oldCode"]),
        "modernCode": escape(data["modernCode"]),
        "oldApproach": escape(data["oldApproach"]),
        "modernApproach": escape(data["modernApproach"]),
        "explanation": escape(data["explanation"]),
        "supportDescription": escape(data["support"]["description"]),
        "supportBadge": support_badge(data["support"]["state"], extra_tokens),
        "supportBadgeClass": support_badge_class(data["support"]["state"]),
        "canonicalUrl": canonical_url,
        "flatUrl": f"{BASE_URL}/{slug}.html",
        "titleJson": json_escape(data["title"]),
        "summaryJson": json_escape(data["summary"]),
        "categoryDisplayJson": json_escape(cat_display),
        "navArrows": render_nav_arrows(data, locale),
        "whyCards": render_why_cards(templates["why_card"], data["whyModernWins"]),
        "docLinks": render_doc_links(templates["doc_link"], data.get("docs", [])),
        "relatedCards": render_related_section(
            templates["related_card"], data, all_snippets, locale, extra_tokens
        ),
        "socialShare": render_social_share(
            templates["social_share"], slug, data["title"], extra_tokens
        ),
    })

    locale_name = LOCALES.get(locale, locale)
    tokens.update(build_contribute_urls(data, locale, locale_name))

    return replace_tokens(templates["page"], tokens)


# ---------------------------------------------------------------------------
# Build a single locale
# ---------------------------------------------------------------------------

def build_locale(locale, templates, all_snippets):
    """Build all HTML files for a single locale."""
    is_english = locale == "en"
    strings = load_strings(locale)
    locale_name = LOCALES.get(locale, locale)
    base_prefix = "../" if is_english else "../../"
    home_url = "/" if is_english else f"/{locale}/"

    print(f"Building locale: {locale} ({locale_name})")

    locale_picker_html = render_locale_picker(locale)
    index_hreflang = render_hreflang_links("", "index")
    i18n_script = render_i18n_script(strings, locale)

    for snippet in all_snippets.values():
        resolved = resolve_snippet(snippet, locale)
        detail_hreflang = render_hreflang_links(f"{snippet['category']}/", snippet["slug"])

        extra_tokens = dict(strings)
        extra_tokens.update({
            "locale": locale,
            "htmlDir": "rtl" if locale == "ar" else "ltr",
            "ogLocale": locale.replace("-", "_"),
            "basePrefix": base_prefix,
            "homeUrl": home_url,
            "localePicker": locale_picker_html,
            "hreflangLinks": detail_hreflang,
            "i18nScript": i18n_script,
        })

        html_content = generate_html(templates, resolved, all_snippets, extra_tokens, locale).strip()

        if is_english:
            out_dir = os.path.join(SITE_DIR, snippet["category"])
        else:
            out_dir = os.path.join(SITE_DIR, locale, snippet["category"])
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, f"{snippet['slug']}.html")
        with open(out_path, "w", newline="") as f:
            f.write(html_content)

    print(f"Generated {len(all_snippets)} HTML files for {locale}")

    # Rebuild data/snippets.json
    snippets_list = []
    for s in all_snippets.values():
        resolved = resolve_snippet(s, locale)
        entry = {k: v for k, v in resolved.items() if k not in EXCLUDED_KEYS}
        snippets_list.append(entry)

    data_dir = (
        os.path.join(SITE_DIR, "data")
        if is_english
        else os.path.join(SITE_DIR, locale, "data")
    )
    os.makedirs(data_dir, exist_ok=True)
    with open(os.path.join(data_dir, "snippets.json"), "w") as f:
        json.dump(snippets_list, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"Rebuilt data/snippets.json for {locale} with {len(snippets_list)} entries")

    # Generate index.html from template
    tip_cards = "\n".join(
        render_index_card(templates["index_card"], resolve_snippet(s, locale), locale, strings)
        for s in all_snippets.values()
    )

    index_tokens = dict(strings)
    index_tokens.update({
        "tipCards": tip_cards,
        "snippetCount": str(len(all_snippets)),
        "locale": locale,
        "htmlDir": "rtl" if locale == "ar" else "ltr",
        "ogLocale": locale.replace("-", "_"),
        "canonicalUrl": BASE_URL if is_english else f"{BASE_URL}/{locale}",
        "homeUrl": home_url,
        "indexBasePrefix": "" if is_english else "../",
        "localePicker": locale_picker_html,
        "hreflangLinks": index_hreflang,
        "i18nScript": i18n_script,
    })

    index_html = replace_tokens(templates["index"], index_tokens)
    if is_english:
        index_path = os.path.join(SITE_DIR, "index.html")
    else:
        index_dir = os.path.join(SITE_DIR, locale)
        os.makedirs(index_dir, exist_ok=True)
        index_path = os.path.join(index_dir, "index.html")
    with open(index_path, "w") as f:
        f.write(index_html)
    print(f"Generated index.html for {locale} with {len(all_snippets)} cards")


# ---------------------------------------------------------------------------
# Templates loader
# ---------------------------------------------------------------------------

def load_templates():
    """Load all HTML templates."""
    def _read(path):
        with open(path) as f:
            return f.read()
    return {
        "page": _read("templates/slug-template.html"),
        "why_card": _read("templates/why-card.html"),
        "related_card": _read("templates/related-card.html"),
        "social_share": _read("templates/social-share.html"),
        "index": _read("templates/index.html"),
        "index_card": _read("templates/index-card.html"),
        "doc_link": _read("templates/doc-link.html"),
    }


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Generate java.evolved site HTML")
    parser.add_argument("--all-locales", action="store_true", help="Build all locales")
    parser.add_argument("--locale", type=str, help="Build a single locale")
    args = parser.parse_args()

    templates = load_templates()
    all_snippets = load_all_snippets()
    print(f"Loaded {len(all_snippets)} snippets")

    if args.locale:
        locales_to_build = [args.locale]
    else:
        locales_to_build = list(LOCALES.keys())

    for locale in locales_to_build:
        build_locale(locale, templates, all_snippets)


if __name__ == "__main__":
    main()
