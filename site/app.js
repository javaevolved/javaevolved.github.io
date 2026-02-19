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
        // Double requestAnimationFrame ensures focus after visibility transition
        requestAnimationFrame(() => {
          requestAnimationFrame(() => {
            input.focus();
          });
        });
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
        <div class="search-result${i === 0 ? ' selected' : ''}" data-slug="${s.slug}" data-category="${s.category}">
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
          window.location.href = '/' + el.dataset.category + '/' + el.dataset.slug + '.html';
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
            window.location.href = '/' + visibleResults[selectedIndex].category + '/' + visibleResults[selectedIndex].slug + '.html';
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
        const wasActive = pill.classList.contains('active');

        // Update active pill (toggle off if re-clicked)
        pills.forEach(p => p.classList.remove('active'));
        if (!wasActive) pill.classList.add('active');

        const showAll = (!wasActive && category === 'all');
        const showCategory = (!wasActive && category !== 'all') ? category : null;

        // Filter cards
        cards.forEach(card => {
          if (showAll || card.dataset.category === showCategory) {
            card.classList.remove('filter-hidden');
          } else {
            card.classList.add('filter-hidden');
          }
        });

        // Update view toggle button state
        if (window.updateViewToggleState) {
          window.updateViewToggleState();
        }
      });
    });

    // Auto-click "All" button on page load to show all cards
    const allButton = document.querySelector('.filter-pill[data-filter="all"]');
    if (allButton) {
      allButton.click();
    }
  };

  /* ==========================================================
     3. Card Hover / Touch Toggle (homepage)
     ========================================================== */
  const initCardToggle = () => {
    const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    if (!isTouchDevice) return;

    // Update hover hints for touch devices
    document.querySelectorAll('.hover-hint').forEach(hint => {
      hint.textContent = '👆 tap or swipe →';
    });

    document.querySelectorAll('.tip-card').forEach(card => {
      let touchStartX = 0;
      let touchStartY = 0;
      let touchEndX = 0;
      let touchEndY = 0;

      // Track touch start
      card.addEventListener('touchstart', (e) => {
        // Only track touches on the card-code area
        if (!e.target.closest('.card-code')) return;
        
        touchStartX = e.changedTouches[0].clientX;
        touchStartY = e.changedTouches[0].clientY;
      }, { passive: true });

      // Handle touch end for swipe or tap
      // Note: passive:false allows us to preventDefault on tap/swipe while still allowing vertical scrolling
      card.addEventListener('touchend', (e) => {
        // Only handle touches on the card-code area
        if (!e.target.closest('.card-code')) return;

        // Don't handle touch events when in expanded mode
        const tipsGrid = document.getElementById('tipsGrid');
        if (tipsGrid && tipsGrid.classList.contains('expanded')) {
          return;
        }

        touchEndX = e.changedTouches[0].clientX;
        touchEndY = e.changedTouches[0].clientY;

        const deltaX = touchEndX - touchStartX;
        const deltaY = touchEndY - touchStartY;
        const absDeltaX = Math.abs(deltaX);
        const absDeltaY = Math.abs(deltaY);

        // Determine if it's a swipe (horizontal movement > 50px and more horizontal than vertical)
        const isHorizontalSwipe = absDeltaX > 50 && absDeltaX > absDeltaY;
        
        if (isHorizontalSwipe) {
          // Prevent default navigation for horizontal swipes
          e.preventDefault();
          // Swipe left = show modern, swipe right = show old
          if (deltaX < 0) {
            // Swipe left - show modern
            card.classList.add('toggled');
          } else {
            // Swipe right - show old
            card.classList.remove('toggled');
          }
        } else if (absDeltaX < 10 && absDeltaY < 10) {
          // It's a tap (movement under 10px threshold)
          e.preventDefault();
          card.classList.toggle('toggled');
        }
        // Note: Vertical scrolling (large deltaY, small deltaX) doesn't call preventDefault
      }, { passive: false });

      // Prevent click events on card-code from navigating (touch devices only)
      // This is a safety net in case touch events trigger click as fallback
      card.addEventListener('click', (e) => {
        if (e.target.closest('.card-code')) {
          // Don't prevent navigation when in expanded mode
          const tipsGrid = document.getElementById('tipsGrid');
          if (tipsGrid && tipsGrid.classList.contains('expanded')) {
            return;
          }
          e.preventDefault();
          e.stopPropagation();
        }
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
     6. View Toggle (Expand/Collapse All Cards)
     ========================================================== */
  const initViewToggle = () => {
    const toggleBtn = document.getElementById('viewToggle');
    const tipsGrid = document.getElementById('tipsGrid');
    if (!toggleBtn || !tipsGrid) return;

    let isExpanded = false;

    const updateButtonState = () => {
      const visibleCards = document.querySelectorAll('.tip-card:not(.filter-hidden)');
      const hasVisibleCards = visibleCards.length > 0;
      
      toggleBtn.disabled = !hasVisibleCards;
      if (!hasVisibleCards) {
        toggleBtn.style.opacity = '0.5';
        toggleBtn.style.cursor = 'not-allowed';
      } else {
        toggleBtn.style.opacity = '1';
        toggleBtn.style.cursor = 'pointer';
      }
    };

    toggleBtn.addEventListener('click', () => {
      isExpanded = !isExpanded;
      
      if (isExpanded) {
        tipsGrid.classList.add('expanded');
        toggleBtn.querySelector('.view-toggle-icon').textContent = '⊟';
        toggleBtn.querySelector('.view-toggle-text').textContent = 'Collapse All';
        
        // Remove toggled class from all cards when expanding
        document.querySelectorAll('.tip-card').forEach(card => {
          card.classList.remove('toggled');
        });
      } else {
        tipsGrid.classList.remove('expanded');
        toggleBtn.querySelector('.view-toggle-icon').textContent = '⊞';
        toggleBtn.querySelector('.view-toggle-text').textContent = 'Expand All';
      }
    });

    // Check initial state
    updateButtonState();

    // Make updateButtonState available for filter to call
    window.updateViewToggleState = updateButtonState;
  };

  /* ==========================================================
     7. Theme Toggle
     ========================================================== */
  const initThemeToggle = () => {
    const btn = document.getElementById('themeToggle');
    if (!btn) return;

    const updateButton = (theme) => {
      btn.textContent = theme === 'dark' ? '☀️' : '🌙';
      btn.setAttribute('aria-label', theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme');
    };

    // The anti-FOUC inline script already applied the theme; just sync the button state
    updateButton(document.documentElement.getAttribute('data-theme') || 'dark');

    btn.addEventListener('click', () => {
      const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('theme', next);
      updateButton(next);
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
    initViewToggle();
    initCopyButtons();
    initSyntaxHighlighting();
    initNewsletter();
    initThemeToggle();
  });
})();
