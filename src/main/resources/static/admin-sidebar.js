/**
 * Shared admin layout — sidebar + topbar CSS
 * Drop <aside class="sidebar" id="sidebar"></aside> on any admin page,
 * include this script, and the sidebar is injected automatically.
 * The topbar CSS is also injected so every page shares the same styling.
 */
(function () {

    const LINKS = [
        { group: 'Main' },
        { href: 'admin-dashboard.html',         icon: '⊞', label: 'Dashboard'     },
        { href: 'projects.html',                icon: '📋', label: 'Projects'      },
        { href: 'admin-quotations.html',        icon: '📄', label: 'Quotations'    },
        { href: 'calendar.html',                icon: '📅', label: 'Calendar'      },
        { href: 'admin-previous-projects.html', icon: '🏆', label: 'Past Projects' },
        { group: 'Manage' },
        { href: 'services.html',                icon: '🔧', label: 'Services'      },
        { href: 'clients.html',                 icon: '👥', label: 'Clients'       },
        { href: 'team.html',                    icon: '👷', label: 'Team'          },
        { href: 'reports.html',                 icon: '📊', label: 'Reports'       },
        { group: 'Account' },
        { href: 'settings.html',                icon: '⚙️', label: 'Settings'      },
        { logout: true,                          icon: '↩', label: 'Logout'        },
    ];

    const currentPage = window.location.pathname.split('/').pop() || 'admin-dashboard.html';

    /* ── HTML ──────────────────────────────────────────────────────── */
    function buildNav() {
        return LINKS.map(l => {
            if (l.group) return `<span class="sb-section-label">${l.group}</span>`;
            if (l.logout) return `<button class="sb-item" id="sb-logout-btn"><span class="sb-icon">${l.icon}</span> ${l.label}</button>`;
            const active = currentPage === l.href ? ' active' : '';
            return `<a class="sb-item${active}" href="${l.href}"><span class="sb-icon">${l.icon}</span> ${l.label}</a>`;
        }).join('');
    }

    const SIDEBAR_HTML = `
        <div class="sb-logo">
            <div class="sb-logo-icons">
                <div class="sb-logo-icon b">🔨</div>
                <div class="sb-logo-icon o">🧱</div>
                <div class="sb-logo-icon g">🖌️</div>
            </div>
            <div class="sb-brand-text">
                <span class="sb-brand">MPHO <em>YAN</em>GA</span>
                <span class="sb-sub">Admin Portal</span>
            </div>
        </div>
        <nav class="sb-nav">${buildNav()}</nav>
        <div class="sb-footer">
            <div class="sb-avatar" id="sb-avatar">A</div>
            <div>
                <span class="sb-user-name" id="sb-name">Admin</span>
                <span class="sb-user-role">Administrator</span>
            </div>
        </div>`;

    /* ── CSS ───────────────────────────────────────────────────────── */
    const LAYOUT_CSS = `
        :root {
            --blue:        #1a5fad;
            --blue-light:  #2471c8;
            --orange:      #e8762a;
            --green:       #3cb54a;
            --red:         #e53e3e;
            --bg:          #f4f6fb;
            --sidebar-bg:  #0d1b2a;
            --card-bg:     #ffffff;
            --border:      #e2e8f0;
            --text:        #1a202c;
            --text-light:  #718096;
            --text-xlight: #a0aec0;
            --shadow:      0 2px 12px rgba(0,0,0,0.08);
            --shadow-lg:   0 8px 32px rgba(0,0,0,0.15);
            --radius:      12px;
            --topbar-h:    64px;
            --sidebar-w:   240px;
        }

        /* ── APP SHELL ── */
        .app { display: flex; min-height: 100vh; }

        /* ── SIDEBAR ── */
        .sidebar {
            width: var(--sidebar-w);
            background: var(--sidebar-bg);
            display: flex;
            flex-direction: column;
            position: fixed;
            top: 0; left: 0; bottom: 0;
            z-index: 200;
            transition: transform 0.3s ease;
        }
        .sb-logo {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 20px 16px 18px;
            border-bottom: 1px solid rgba(255,255,255,0.07);
        }
        .sb-logo-icons { display: flex; gap: 4px; }
        .sb-logo-icon {
            width: 26px; height: 26px; border-radius: 6px;
            display: flex; align-items: center; justify-content: center;
            font-size: 13px;
        }
        .sb-logo-icon.b { background: #1a5fad; }
        .sb-logo-icon.o { background: #e8762a; }
        .sb-logo-icon.g { background: #3cb54a; }
        .sb-brand-text { line-height: 1.25; }
        .sb-brand {
            display: block;
            font-family: 'Oswald', sans-serif;
            font-size: 15px; font-weight: 700;
            color: #fff; letter-spacing: 0.5px;
        }
        .sb-brand em { color: var(--orange); font-style: italic; }
        .sb-sub {
            display: block;
            font-size: 10px; letter-spacing: 2px;
            text-transform: uppercase;
            color: rgba(255,255,255,0.35);
        }
        .sb-nav { flex: 1; padding: 14px 10px; overflow-y: auto; }
        .sb-section-label {
            display: block;
            font-size: 9px; letter-spacing: 2px;
            text-transform: uppercase;
            color: rgba(255,255,255,0.25);
            padding: 14px 8px 6px;
        }
        .sb-item {
            display: flex; align-items: center; gap: 10px;
            width: 100%; padding: 9px 12px;
            border: none; border-left: 3px solid transparent;
            background: none;
            color: rgba(255,255,255,0.6);
            font-family: 'Barlow', sans-serif;
            font-size: 14px; border-radius: 8px;
            cursor: pointer; text-decoration: none;
            transition: all 0.2s; text-align: left;
        }
        .sb-item:hover { background: rgba(255,255,255,0.08); color: #fff; }
        .sb-item.active {
            background: rgba(26,95,173,0.25);
            color: #fff;
            border-left: 3px solid var(--blue);
        }
        .sb-icon { font-size: 15px; width: 20px; text-align: center; flex-shrink: 0; }
        .sb-footer {
            display: flex; align-items: center; gap: 10px;
            padding: 14px 16px;
            border-top: 1px solid rgba(255,255,255,0.07);
            flex-shrink: 0;
        }
        .sb-avatar {
            width: 34px; height: 34px; border-radius: 50%;
            background: var(--blue); color: #fff;
            display: flex; align-items: center; justify-content: center;
            font-size: 12px; font-weight: 700; flex-shrink: 0;
        }
        .sb-user-name { display: block; font-size: 13px; color: #fff; font-weight: 600; }
        .sb-user-role { display: block; font-size: 10px; color: rgba(255,255,255,0.4); }

        /* ── OVERLAY (mobile) ── */
        .overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 190; }
        .overlay.active { display: block; }

        /* ── MAIN ── */
        .main { flex: 1; margin-left: var(--sidebar-w); min-height: 100vh; display: flex; flex-direction: column; }

        /* ── TOPBAR ── */
        .topbar {
            height: var(--topbar-h);
            background: var(--card-bg);
            border-bottom: 1px solid var(--border);
            display: flex; align-items: center;
            justify-content: space-between;
            padding: 0 24px;
            position: sticky; top: 0; z-index: 100;
            box-shadow: 0 1px 4px rgba(0,0,0,0.06);
        }
        .tb-left  { display: flex; align-items: center; gap: 14px; }
        .tb-right { display: flex; align-items: center; gap: 12px; }
        .hamburger {
            display: none; flex-direction: column; gap: 4px;
            background: none; border: none; cursor: pointer; padding: 4px;
        }
        .hamburger span { display: block; width: 22px; height: 2px; background: var(--text); border-radius: 2px; }
        .page-heading h1 {
            font-family: 'Oswald', sans-serif;
            font-size: 20px; font-weight: 600;
            color: var(--text);
        }
        .page-date { font-size: 12px; color: var(--text-light); display: block; }
        .tb-search {
            display: flex; align-items: center; gap: 8px;
            background: var(--bg);
            border: 1px solid var(--border);
            border-radius: 8px; padding: 7px 12px;
        }
        .tb-search input {
            border: none; background: none; outline: none;
            font-family: 'Barlow', sans-serif;
            font-size: 14px; color: var(--text); width: 180px;
        }
        .tb-btn {
            display: flex; align-items: center; gap: 6px;
            padding: 8px 16px; border: none; border-radius: 8px;
            font-family: 'Barlow', sans-serif;
            font-size: 14px; font-weight: 600;
            cursor: pointer; transition: opacity 0.2s, transform 0.15s;
        }
        .tb-btn:hover { opacity: 0.88; transform: translateY(-1px); }
        .tb-btn.primary { background: var(--blue); color: #fff; }
        .tb-btn.secondary { background: var(--bg); color: var(--text); border: 1px solid var(--border); }
        .tb-btn.danger { background: var(--red); color: #fff; }
        .tb-avatar {
            width: 34px; height: 34px; border-radius: 50%;
            background: var(--blue); color: #fff;
            display: flex; align-items: center; justify-content: center;
            font-size: 12px; font-weight: 700;
            cursor: default;
        }

        /* ── CONTENT AREA ── */
        .content { padding: 24px; flex: 1; }

        /* ── MOBILE ── */
        @media (max-width: 768px) {
            .sidebar { transform: translateX(-100%); }
            .sidebar.open { transform: translateX(0); }
            .main { margin-left: 0; }
            .hamburger { display: flex; }
            .tb-search { display: none; }
        }`;

    /* ── INJECT ────────────────────────────────────────────────────── */
    function inject() {
        if (!document.getElementById('admin-layout-css')) {
            const style = document.createElement('style');
            style.id = 'admin-layout-css';
            style.textContent = LAYOUT_CSS;
            document.head.appendChild(style);
        }

        const aside = document.getElementById('sidebar');
        if (aside) aside.innerHTML = SIDEBAR_HTML;

        if (!document.getElementById('overlay')) {
            const ov = document.createElement('div');
            ov.id = 'overlay';
            ov.className = 'overlay';
            aside && aside.parentNode.insertBefore(ov, aside.nextSibling);
        }
    }

    /* ── BEHAVIOR ──────────────────────────────────────────────────── */
    function setupBehavior() {
        const sidebar   = document.getElementById('sidebar');
        const overlay   = document.getElementById('overlay');
        const hamburger = document.getElementById('hamburger');
        const logoutBtn = document.getElementById('sb-logout-btn');

        if (hamburger && sidebar) {
            hamburger.addEventListener('click', () => {
                sidebar.classList.toggle('open');
                overlay && overlay.classList.toggle('active');
            });
        }
        if (overlay && sidebar) {
            overlay.addEventListener('click', () => {
                sidebar.classList.remove('open');
                overlay.classList.remove('active');
            });
        }
        if (logoutBtn) {
            logoutBtn.addEventListener('click', async () => {
                try {
                    await fetch('/api/admin/clear-session', { method: 'POST', credentials: 'include' });
                } catch (_) {}
                sessionStorage.clear();
                localStorage.removeItem('admin');
                window.location.href = 'login.html';
            });
        }

        // Set date on any element with id="page-date"
        const dateEl = document.getElementById('page-date');
        if (dateEl && !dateEl.textContent.trim()) {
            dateEl.textContent = new Date().toLocaleDateString('en-ZW', {
                weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
            });
        }
    }

    async function loadAdminName() {
        try {
            const res = await fetch('/api/admin/current-session', { credentials: 'include' });
            if (!res.ok) return;
            const data = await res.json();
            const admin = data && data.data;
            if (!admin) return;
            const initials = (admin.name[0] + (admin.surname ? admin.surname[0] : '')).toUpperCase();
            const sbAvatar = document.getElementById('sb-avatar');
            const tbAvatar = document.getElementById('tb-avatar');
            const sbName   = document.getElementById('sb-name');
            if (sbAvatar) sbAvatar.textContent = initials;
            if (tbAvatar) tbAvatar.textContent = initials;
            if (sbName)   sbName.textContent = `${admin.name} ${admin.surname || ''}`.trim();
        } catch (_) {}
    }

    /* ── BOOT ──────────────────────────────────────────────────────── */
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => { inject(); setupBehavior(); loadAdminName(); });
    } else {
        inject(); setupBehavior(); loadAdminName();
    }

})();
