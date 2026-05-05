/**
 * Shared admin sidebar — drop <aside class="sidebar" id="sidebar"></aside>
 * on any admin page, include this script, and the sidebar is injected automatically.
 */
(function () {

    const LINKS = [
        { group: 'Main' },
        { href: 'admin-dashboard.html',       icon: '⊞', label: 'Dashboard'    },
        { href: 'projects.html',              icon: '📋', label: 'Projects'     },
        { href: 'admin-quotations.html',      icon: '📄', label: 'Quotations'   },
        { href: 'calendar.html',              icon: '📅', label: 'Calendar'     },
        { href: 'admin-previous-projects.html', icon: '🏆', label: 'Past Projects' },
        { group: 'Manage' },
        { href: 'services.html',              icon: '🔧', label: 'Services'     },
        { href: 'clients.html',               icon: '👥', label: 'Clients'      },
        { href: 'team.html',                  icon: '👷', label: 'Team'         },
        { href: 'reports.html',               icon: '📊', label: 'Reports'      },
        { group: 'Account' },
        { href: 'settings.html',              icon: '⚙️', label: 'Settings'     },
        { logout: true,                        icon: '↩', label: 'Logout'       },
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
    const SIDEBAR_CSS = `
        :root { --sb-w: 252px; }
        .app { display: flex; min-height: 100vh; }
        .sidebar {
            width: var(--sb-w); flex-shrink: 0;
            background: #0d1b2a;
            position: fixed; top: 0; left: 0; height: 100vh;
            display: flex; flex-direction: column;
            z-index: 200; overflow-y: auto;
            transition: transform 0.28s ease;
        }
        .sidebar::before {
            content: ''; position: absolute; inset: 0;
            background: linear-gradient(180deg,rgba(26,95,173,.08) 0%,transparent 60%);
            pointer-events: none;
        }
        .sb-logo { display: flex; align-items: center; gap: 10px; padding: 22px 18px 14px; flex-shrink: 0; }
        .sb-logo-icons { display: flex; gap: 4px; }
        .sb-logo-icon { width: 22px; height: 22px; border-radius: 5px; display: flex; align-items: center; justify-content: center; font-size: 11px; }
        .sb-logo-icon.b { background: #1a5fad; }
        .sb-logo-icon.o { background: #e8762a; }
        .sb-logo-icon.g { background: #3cb54a; }
        .sb-brand { font-family: 'Oswald', sans-serif; font-size: 14px; font-weight: 700; letter-spacing: 2px; color: #fff; text-transform: uppercase; }
        .sb-brand em { color: #e8762a; font-style: normal; }
        .sb-sub { font-size: 9px; color: rgba(255,255,255,.3); letter-spacing: 2.5px; text-transform: uppercase; }
        .sb-nav { flex: 1; padding: 8px 12px 16px; display: flex; flex-direction: column; gap: 2px; }
        .sb-section-label { font-size: 9px; letter-spacing: 2px; text-transform: uppercase; color: rgba(255,255,255,.25); padding: 14px 8px 4px; display: block; }
        .sb-item {
            display: flex; align-items: center; gap: 10px;
            padding: 9px 12px; border-radius: 8px;
            font-size: 13px; font-weight: 500; color: rgba(255,255,255,.55);
            background: none; border: none; cursor: pointer; width: 100%;
            text-decoration: none; transition: all .18s;
        }
        .sb-item:hover { background: rgba(255,255,255,.06); color: rgba(255,255,255,.85); }
        .sb-item.active { background: rgba(26,95,173,.25); color: #fff; font-weight: 600; }
        .sb-item.active::before { display: none; }
        .sb-icon { font-size: 15px; width: 22px; text-align: center; flex-shrink: 0; }
        .sb-footer { display: flex; align-items: center; gap: 10px; padding: 16px 18px; border-top: 1px solid rgba(255,255,255,.07); flex-shrink: 0; }
        .sb-avatar { width: 34px; height: 34px; border-radius: 50%; background: #1a5fad; color: #fff; font-weight: 700; font-size: 13px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
        .sb-user-name { display: block; font-size: 13px; font-weight: 600; color: #fff; }
        .sb-user-role { font-size: 10px; color: rgba(255,255,255,.3); }
        .main { flex: 1; margin-left: var(--sb-w); min-height: 100vh; }
        .overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 199; }
        .overlay.show { display: block; }
        .hamburger { display: none; flex-direction: column; gap: 4px; background: none; border: none; cursor: pointer; padding: 6px; }
        .hamburger span { display: block; width: 22px; height: 2px; background: currentColor; border-radius: 2px; }
        @media (max-width: 768px) {
            .sidebar { transform: translateX(-100%); }
            .sidebar.open { transform: translateX(0); }
            .main { margin-left: 0; }
            .hamburger { display: flex; }
        }`;

    /* ── INJECT ────────────────────────────────────────────────────── */
    function inject() {
        // CSS
        if (!document.getElementById('admin-sb-css')) {
            const style = document.createElement('style');
            style.id = 'admin-sb-css';
            style.textContent = SIDEBAR_CSS;
            document.head.appendChild(style);
        }

        // Sidebar HTML
        const aside = document.getElementById('sidebar');
        if (aside) aside.innerHTML = SIDEBAR_HTML;

        // Overlay (create if missing)
        if (!document.getElementById('overlay')) {
            const ov = document.createElement('div');
            ov.id = 'overlay'; ov.className = 'overlay';
            aside && aside.parentNode.insertBefore(ov, aside.nextSibling);
        }
    }

    /* ── BEHAVIOR ──────────────────────────────────────────────────── */
    function setupBehavior() {
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('overlay');
        const hamburger = document.getElementById('hamburger');
        const logoutBtn = document.getElementById('sb-logout-btn');

        if (hamburger && sidebar) {
            hamburger.addEventListener('click', () => {
                sidebar.classList.toggle('open');
                overlay && overlay.classList.toggle('show');
            });
        }
        if (overlay && sidebar) {
            overlay.addEventListener('click', () => {
                sidebar.classList.remove('open');
                overlay.classList.remove('show');
            });
        }
        if (logoutBtn) {
            logoutBtn.addEventListener('click', async () => {
                try { await fetch('/api/admin/clear-session', { method: 'POST', credentials: 'include' }); } catch (_) {}
                sessionStorage.clear();
                localStorage.removeItem('admin');
                window.location.href = 'login.html';
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
            const el = document.getElementById('sb-avatar');
            const nm = document.getElementById('sb-name');
            if (el) el.textContent = initials;
            if (nm) nm.textContent = `${admin.name} ${admin.surname || ''}`.trim();
        } catch (_) {}
    }

    /* ── BOOT ──────────────────────────────────────────────────────── */
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => { inject(); setupBehavior(); loadAdminName(); });
    } else {
        inject(); setupBehavior(); loadAdminName();
    }

})();
