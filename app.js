/* ===========================
   Modern Java — app.js
   Vanilla JS for search, filters, syntax highlighting, and interactions
   =========================== */

(() => {
  'use strict';

  /* ---------- Snippets Data ---------- */
  let snippets = [];

  const loadSnippets = async () => {
    try {
      const res = await fetch('/data/snippets.json');
      snippets = await res.json();
    } catch (e) {
      console.warn('Could not load snippets.json:', e);
    }
  };

  /* ==========================================================
     1. Search Overlay (⌘K / Ctrl+K)
     ========================================================== */
  const initSearch = () => {
    const overlay = document.querySelector('.search-overlay');
    const cmdBar = document.querySelector('.cmd-bar');
    if (!overlay) return;

    const input = overlay.querySelector('.search-input');
    const resultsContainer = overlay.querySelector('.search-results');
    let selectedIndex = -1;
    let visibleResults = [];

    const openSearch = () => {
      overlay.classList.add('active');
      if (input) {
        input.value = '';
        input.focus();
      }
      renderResults('');
    };

    const closeSearch = () => {
      overlay.classList.remove('active');
      selectedIndex = -1;
    };

    // Fuzzy match: check if query words appear in target string
    const fuzzyMatch = (query, text) => {
      const lower = text.toLowerCase();
      return query.toLowerCase().split(/\s+/).filter(Boolean)
        .every(word => lower.includes(word));
    };

    const renderResults = (query) => {
      if (!resultsContainer) return;

      if (!query.trim()) {
        visibleResults = snippets.slice(0, 12);
      } else {
        visibleResults = snippets.filter(s =>
          fuzzyMatch(query, s.title) ||
          fuzzyMatch(query, s.category) ||
          fuzzyMatch(query, s.summary)
        );
      }

      selectedIndex = visibleResults.length > 0 ? 0 : -1;

      resultsContainer.innerHTML = visibleResults.map((s, i) => `
        <div class="search-result${i === 0 ? ' selected' : ''}" data-slug="${s.slug}">
          <div>
            <div class="title">${escapeHtml(s.title)}</div>
            <div class="desc">${escapeHtml(s.summary)}</div>
          </div>
          <span class="badge ${s.category}">${s.category}</span>
        </div>
      `).join('');

      // Click handlers on results
      resultsContainer.querySelectorAll('.search-result').forEach(el => {
        el.addEventListener('click', () => {
          window.location.href = '/' + el.dataset.slug + '.html';
        });
      });
    };

    const updateSelection = () => {
      const items = resultsContainer.querySelectorAll('.search-result');
      items.forEach((el, i) => {
        el.classList.toggle('selected', i === selectedIndex);
      });
      // Scroll selected into view
      if (items[selectedIndex]) {
        items[selectedIndex].scrollIntoView({ block: 'nearest' });
      }
    };

    // Keyboard shortcut: ⌘K / Ctrl+K
    document.addEventListener('keydown', (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        openSearch();
      }
      if (e.key === 'Escape') {
        closeSearch();
      }
    });

    // Cmd-bar click
    if (cmdBar) {
      cmdBar.addEventListener('click', openSearch);
    }

    // Click backdrop to close
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) closeSearch();
    });

    // Search input events
    if (input) {
      input.addEventListener('input', () => {
        renderResults(input.value);
      });

      input.addEventListener('keydown', (e) => {
        if (e.key === 'ArrowDown') {
          e.preventDefault();
          if (visibleResults.length > 0) {
            selectedIndex = (selectedIndex + 1) % visibleResults.length;
            updateSelection();
          }
        } else if (e.key === 'ArrowUp') {
          e.preventDefault();
          if (visibleResults.length > 0) {
            selectedIndex = (selectedIndex - 1 + visibleResults.length) % visibleResults.length;
            updateSelection();
          }
        } else if (e.key === 'Enter') {
          e.preventDefault();
          if (selectedIndex >= 0 && visibleResults[selectedIndex]) {
            window.location.href = '/' + visibleResults[selectedIndex].slug + '.html';
          }
        }
      });
    }
  };

  /* ==========================================================
     2. Category Filter Pills (homepage)
     ========================================================== */
  const initFilters = () => {
    const pills = document.querySelectorAll('.filter-pill');
    const cards = document.querySelectorAll('.tip-card');
    if (!pills.length || !cards.length) return;

    pills.forEach(pill => {
      pill.addEventListener('click', () => {
        const category = pill.dataset.filter || 'all';

        // Update active pill
        pills.forEach(p => p.classList.remove('active'));
        pill.classList.add('active');

        // Filter cards
        cards.forEach(card => {
          if (category === 'all' || card.dataset.category === category) {
            card.classList.remove('filter-hidden');
          } else {
            card.classList.add('filter-hidden');
          }
        });
      });
    });
  };

  /* ==========================================================
     3. Card Hover / Touch Toggle (homepage)
     ========================================================== */
  const initCardToggle = () => {
    const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    if (!isTouchDevice) return;

    document.querySelectorAll('.tip-card').forEach(card => {
      card.addEventListener('click', (e) => {
        // Don't toggle if clicking a link inside the card
        if (e.target.closest('a')) return;
        card.classList.toggle('toggled');
      });
    });
  };

  /* ==========================================================
     4. Copy-to-Clipboard (article pages)
     ========================================================== */
  const initCopyButtons = () => {
    document.querySelectorAll('.copy-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        // Find adjacent code block
        const codeBlock = btn.closest('.code-header')?.nextElementSibling
          || btn.closest('.compare-panel-header')?.nextElementSibling?.querySelector('pre, code, .code-text')
          || btn.parentElement?.querySelector('pre, code, .code-text');
        if (!codeBlock) return;

        const text = codeBlock.textContent;
        navigator.clipboard.writeText(text).then(() => {
          btn.classList.add('copied');
          const original = btn.textContent;
          btn.textContent = 'Copied!';
          setTimeout(() => {
            btn.classList.remove('copied');
            btn.textContent = original;
          }, 2000);
        }).catch(() => {
          // Fallback for older browsers
          const textarea = document.createElement('textarea');
          textarea.value = text;
          textarea.style.position = 'fixed';
          textarea.style.opacity = '0';
          document.body.appendChild(textarea);
          textarea.select();
          document.execCommand('copy');
          document.body.removeChild(textarea);
          btn.classList.add('copied');
          const original = btn.textContent;
          btn.textContent = 'Copied!';
          setTimeout(() => {
            btn.classList.remove('copied');
            btn.textContent = original;
          }, 2000);
        });
      });
    });
  };

  /* ==========================================================
     5. Syntax Highlighting (Java)
     ========================================================== */
  const JAVA_KEYWORDS = new Set([
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch',
    'char', 'class', 'const', 'continue', 'default', 'do', 'double',
    'else', 'enum', 'extends', 'final', 'finally', 'float', 'for',
    'goto', 'if', 'implements', 'import', 'instanceof', 'int',
    'interface', 'long', 'module', 'native', 'new', 'null', 'package',
    'permits', 'private', 'protected', 'public', 'record', 'return',
    'sealed', 'short', 'static', 'strictfp', 'super', 'switch',
    'synchronized', 'this', 'throw', 'throws', 'transient', 'try',
    'var', 'void', 'volatile', 'when', 'while', 'yield'
  ]);

  const highlightJava = (code) => {
    const tokens = [];
    let i = 0;
    const len = code.length;

    while (i < len) {
      // Block comments: /* ... */
      if (code[i] === '/' && code[i + 1] === '*') {
        let end = code.indexOf('*/', i + 2);
        if (end === -1) end = len - 2;
        const text = code.slice(i, end + 2);
        tokens.push(`<span class="cmt">${escapeHtml(text)}</span>`);
        i = end + 2;
        continue;
      }

      // Line comments: // ...
      if (code[i] === '/' && code[i + 1] === '/') {
        let end = code.indexOf('\n', i);
        if (end === -1) end = len;
        const text = code.slice(i, end);
        tokens.push(`<span class="cmt">${escapeHtml(text)}</span>`);
        i = end;
        continue;
      }

      // Text blocks: """ ... """
      if (code[i] === '"' && code[i + 1] === '"' && code[i + 2] === '"') {
        let end = code.indexOf('"""', i + 3);
        if (end === -1) end = len - 3;
        const text = code.slice(i, end + 3);
        tokens.push(`<span class="str">${escapeHtml(text)}</span>`);
        i = end + 3;
        continue;
      }

      // String literals: "..."
      if (code[i] === '"') {
        let j = i + 1;
        while (j < len && code[j] !== '"') {
          if (code[j] === '\\') j++; // skip escaped char
          j++;
        }
        const text = code.slice(i, j + 1);
        tokens.push(`<span class="str">${escapeHtml(text)}</span>`);
        i = j + 1;
        continue;
      }

      // Char literals: '...'
      if (code[i] === "'") {
        let j = i + 1;
        while (j < len && code[j] !== "'") {
          if (code[j] === '\\') j++;
          j++;
        }
        const text = code.slice(i, j + 1);
        tokens.push(`<span class="str">${escapeHtml(text)}</span>`);
        i = j + 1;
        continue;
      }

      // Annotations: @Word
      if (code[i] === '@' && i + 1 < len && /[A-Za-z_]/.test(code[i + 1])) {
        let j = i + 1;
        while (j < len && /[\w]/.test(code[j])) j++;
        const text = code.slice(i, j);
        tokens.push(`<span class="ann">${escapeHtml(text)}</span>`);
        i = j;
        continue;
      }

      // Numbers: digits (including hex, binary, underscores, suffixes)
      if (/[0-9]/.test(code[i]) && (i === 0 || !/[\w]/.test(code[i - 1]))) {
        let j = i;
        // Hex/binary prefix
        if (code[j] === '0' && (code[j + 1] === 'x' || code[j + 1] === 'X' ||
            code[j + 1] === 'b' || code[j + 1] === 'B')) {
          j += 2;
        }
        while (j < len && /[0-9a-fA-F_]/.test(code[j])) j++;
        // Decimal part
        if (code[j] === '.' && /[0-9]/.test(code[j + 1])) {
          j++;
          while (j < len && /[0-9_]/.test(code[j])) j++;
        }
        // Exponent
        if (code[j] === 'e' || code[j] === 'E') {
          j++;
          if (code[j] === '+' || code[j] === '-') j++;
          while (j < len && /[0-9_]/.test(code[j])) j++;
        }
        // Type suffix (L, f, d)
        if (/[LlFfDd]/.test(code[j])) j++;
        const text = code.slice(i, j);
        tokens.push(`<span class="num">${escapeHtml(text)}</span>`);
        i = j;
        continue;
      }

      // Words: keywords, types, method calls
      if (/[A-Za-z_$]/.test(code[i])) {
        let j = i;
        while (j < len && /[\w$]/.test(code[j])) j++;
        const word = code.slice(i, j);

        // Look ahead for method call: word(
        let k = j;
        while (k < len && code[k] === ' ') k++;

        if (JAVA_KEYWORDS.has(word)) {
          tokens.push(`<span class="kw">${escapeHtml(word)}</span>`);
        } else if (code[k] === '(' && !/^[A-Z]/.test(word)) {
          tokens.push(`<span class="fn">${escapeHtml(word)}</span>`);
        } else if (/^[A-Z]/.test(word)) {
          tokens.push(`<span class="typ">${escapeHtml(word)}</span>`);
        } else {
          tokens.push(escapeHtml(word));
        }
        i = j;
        continue;
      }

      // Everything else: operators, punctuation, whitespace
      tokens.push(escapeHtml(code[i]));
      i++;
    }

    return tokens.join('');
  };

  const initSyntaxHighlighting = () => {
    document.querySelectorAll('.code-text').forEach(el => {
      // Skip if already highlighted
      if (el.dataset.highlighted) return;
      el.dataset.highlighted = 'true';

      const raw = el.textContent;
      el.innerHTML = highlightJava(raw);
    });
  };

  /* ==========================================================
     6. Newsletter Form
     ========================================================== */
  const initNewsletter = () => {
    const form = document.querySelector('.newsletter-form');
    if (!form) return;

    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const box = form.closest('.newsletter-box');
      if (box) {
        box.innerHTML = '<p style="color: var(--accent); font-weight: 600;">Thanks! 🎉 You\'re on the list.</p>';
      } else {
        form.innerHTML = '<p style="color: var(--accent); font-weight: 600;">Thanks!</p>';
      }
    });
  };

  /* ==========================================================
     Utilities
     ========================================================== */
  const escapeHtml = (str) => {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  };

  /* ==========================================================
     Init
     ========================================================== */
  document.addEventListener('DOMContentLoaded', () => {
    loadSnippets().then(() => {
      initSearch();
    });
    initFilters();
    initCardToggle();
    initCopyButtons();
    initSyntaxHighlighting();
    initNewsletter();
  });
})();
