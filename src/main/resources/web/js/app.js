/* ─────────────────────────────────────────
   COLLEGE PORTAL RBAC — FRONTEND APP
   ───────────────────────────────────────── */

const API = '';
let authToken = localStorage.getItem('rbac_token') || null;
let currentUser = null;
let userPermissions = [];
let statsData = {};

// ═══════════════════════════════════════
// UTILITIES
// ═══════════════════════════════════════

function $(id) { return document.getElementById(id); }

async function api(method, path, data) {
  const opts = {
    method,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {})
    }
  };
  if (data && method !== 'GET') {
    opts.body = Object.entries(data).map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&');
  }
  const res = await fetch(API + path, opts);
  return res.json();
}

function showPage(id) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  $(id).classList.add('active');
}

function toast(msg, type = 'info') {
  const tc = $('toastContainer');
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
  el.innerHTML = `<span>${icon}</span><span>${msg}</span>`;
  tc.appendChild(el);
  setTimeout(() => {
    el.style.animation = 'toastOut 0.3s ease forwards';
    setTimeout(() => el.remove(), 300);
  }, 3500);
}

function openModal(title, bodyHtml) {
  $('modalTitle').textContent = title;
  $('modalBody').innerHTML = bodyHtml;
  $('modal').classList.remove('hidden');
}

function closeModalDirect() { $('modal').classList.add('hidden'); }
function closeModal(e) { if (e.target === $('modal')) closeModalDirect(); }

function togglePwd() {
  const inp = $('password');
  inp.type = inp.type === 'password' ? 'text' : 'password';
}

function fillCreds(u, p) {
  $('username').value = u;
  $('password').value = p;
  document.querySelectorAll('.role-pill').forEach(b => b.classList.remove('active'));
  event.currentTarget.classList.add('active');
}

function toggleSidebar() {
  const sb = $('sidebar');
  const mc = $('mainContent');
  if (window.innerWidth <= 768) {
    sb.classList.toggle('mobile-open');
  } else {
    const isCollapsed = sb.classList.toggle('collapsed');
    if (isCollapsed) {
      mc.style.marginLeft = '68px';
      $('sidebarToggle').textContent = '▶';
    } else {
      mc.style.marginLeft = 'var(--sidebar-w)';
      $('sidebarToggle').textContent = '◀';
    }
  }
}

function gradeClass(g) {
  const map = { 'O': 'grade-O', 'A+': 'grade-A+', 'A': 'grade-A', 'B+': 'grade-B+', 'B': 'grade-B', 'C': 'grade-C', 'F': 'grade-F' };
  return map[g] || '';
}

function pct(v, max = 100) { return Math.min(100, Math.round((v / max) * 100)); }

// ═══════════════════════════════════════
// AUTH
// ═══════════════════════════════════════

async function handleLogin(e) {
  e.preventDefault();
  const btn = $('loginBtn');
  const txt = $('loginBtnText');
  const spin = $('loginSpinner');
  const errEl = $('loginError');
  errEl.classList.add('hidden');
  txt.textContent = 'Signing in...';
  spin.classList.remove('hidden');
  btn.disabled = true;

  try {
    const data = await api('POST', '/api/login', {
      username: $('username').value.trim(),
      password: $('password').value
    });
    if (data.success) {
      authToken = data.token;
      localStorage.setItem('rbac_token', authToken);
      currentUser = data.user;
      userPermissions = data.permissions || [];
      await initDashboard();
    } else {
      errEl.textContent = data.message || 'Login failed.';
      errEl.classList.remove('hidden');
    }
  } catch (err) {
    errEl.textContent = 'Cannot connect to server. Is the Java server running?';
    errEl.classList.remove('hidden');
  } finally {
    txt.textContent = 'Sign In';
    spin.classList.add('hidden');
    btn.disabled = false;
  }
}

async function handleLogout() {
  try { await api('POST', '/api/logout', {}); } catch (e) {}
  authToken = null;
  currentUser = null;
  userPermissions = [];
  localStorage.removeItem('rbac_token');
  showPage('loginPage');
  $('loginForm').reset();
}

// ═══════════════════════════════════════
// DASHBOARD INIT
// ═══════════════════════════════════════

async function initDashboard() {
  showPage('dashboardPage');
  buildSidebar();
  updateTopbar();
  await loadStats();
  showSection('overview');
}

function updateTopbar() {
  $('sidebarUserName').textContent = currentUser.fullName;
  $('sidebarAvatar').textContent = currentUser.fullName.charAt(0).toUpperCase();
  $('topbarUserName').textContent = currentUser.fullName;
  $('topbarAvatar').textContent = currentUser.fullName.charAt(0).toUpperCase();

  const badge = $('sidebarRoleBadge');
  badge.textContent = currentUser.roleDisplayName;
  badge.className = `role-badge badge-${currentUser.role}`;
}

function buildSidebar() {
  const role = currentUser.role;
  const nav = $('sidebarNav');

  const items = [
    { id: 'overview',     icon: '📊', label: 'Overview',      roles: ['ADMIN','FACULTY','STUDENT'] },
    { id: 'users',        icon: '👥', label: 'Manage Users',  roles: ['ADMIN'] },
    { id: 'marks',        icon: '📝', label: 'All Marks',     roles: ['ADMIN','FACULTY'] },
    { id: 'mymarks',      icon: '🎓', label: 'My Marks',      roles: ['STUDENT'] },
    { id: 'addmarks',     icon: '➕', label: 'Add Marks',     roles: ['FACULTY'] },
    { id: 'permissions',  icon: '🔐', label: 'Permissions',   roles: ['ADMIN','FACULTY','STUDENT'] },
    { id: 'audit',        icon: '📋', label: 'Audit Log',     roles: ['ADMIN'] },
  ];

  nav.innerHTML = items
    .filter(i => i.roles.includes(role))
    .map(i => `
      <button class="nav-item" id="nav-${i.id}" onclick="showSection('${i.id}')" data-label="${i.label}">
        <span class="nav-icon">${i.icon}</span>
        <span class="nav-label">${i.label}</span>
      </button>
    `).join('');
}

async function loadStats() {
  try {
    statsData = await api('GET', '/api/stats');
  } catch (e) {}
}

function setActiveNav(id) {
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  const el = $(`nav-${id}`);
  if (el) el.classList.add('active');
  const labels = {
    overview: 'Overview', users: 'Manage Users', marks: 'All Marks',
    mymarks: 'My Marks', addmarks: 'Add Marks', permissions: 'Role Permissions', audit: 'Audit Log'
  };
  $('topbarTitle').textContent = labels[id] || 'Dashboard';
}

// ═══════════════════════════════════════
// SECTIONS
// ═══════════════════════════════════════

async function showSection(id) {
  setActiveNav(id);
  $('contentArea').innerHTML = '<div style="text-align:center;padding:60px;color:var(--text-muted)">⏳ Loading...</div>';
  switch (id) {
    case 'overview':   await renderOverview(); break;
    case 'users':      await renderUsers(); break;
    case 'marks':      await renderAllMarks(); break;
    case 'mymarks':    await renderMyMarks(); break;
    case 'addmarks':   renderAddMarks(); break;
    case 'permissions': await renderPermissions(); break;
    case 'audit':      await renderAudit(); break;
    default: $('contentArea').innerHTML = '<p>Not found</p>';
  }
  // Close sidebar on mobile
  if (window.innerWidth <= 768) $('sidebar').classList.remove('mobile-open');
}

// ═══════════════════════════════════════
// OVERVIEW
// ═══════════════════════════════════════

async function renderOverview() {
  await loadStats();
  const role = currentUser.role;
  const roleEmoji = role === 'ADMIN' ? '👑' : role === 'FACULTY' ? '🧑‍🏫' : '🎒';

  let statsHtml = '';
  if (role === 'ADMIN') {
    statsHtml = `
      <div class="stats-grid">
        <div class="stat-card blue">
          <div class="stat-icon">👥</div>
          <div class="stat-label">Total Users</div>
          <div class="stat-value">${statsData.totalUsers || 0}</div>
        </div>
        <div class="stat-card amber">
          <div class="stat-icon">👑</div>
          <div class="stat-label">Admins</div>
          <div class="stat-value">${statsData.adminCount || 0}</div>
        </div>
        <div class="stat-card teal">
          <div class="stat-icon">🧑‍🏫</div>
          <div class="stat-label">Faculty</div>
          <div class="stat-value">${statsData.facultyCount || 0}</div>
        </div>
        <div class="stat-card green">
          <div class="stat-icon">🎒</div>
          <div class="stat-label">Students</div>
          <div class="stat-value">${statsData.studentCount || 0}</div>
        </div>
        <div class="stat-card blue">
          <div class="stat-icon">📝</div>
          <div class="stat-label">Mark Records</div>
          <div class="stat-value">${statsData.totalMarks || 0}</div>
        </div>
        <div class="stat-card green">
          <div class="stat-icon">✅</div>
          <div class="stat-label">Access Granted</div>
          <div class="stat-value">${statsData.auditGranted || 0}</div>
        </div>
        <div class="stat-card amber">
          <div class="stat-icon">🚫</div>
          <div class="stat-label">Access Denied</div>
          <div class="stat-value">${statsData.auditDenied || 0}</div>
        </div>
      </div>`;
  } else if (role === 'FACULTY') {
    statsHtml = `
      <div class="stats-grid">
        <div class="stat-card blue">
          <div class="stat-icon">🎒</div>
          <div class="stat-label">Students</div>
          <div class="stat-value">${statsData.studentCount || 0}</div>
        </div>
        <div class="stat-card teal">
          <div class="stat-icon">📝</div>
          <div class="stat-label">Mark Records</div>
          <div class="stat-value">${statsData.totalMarks || 0}</div>
        </div>
      </div>`;
  } else {
    statsHtml = `
      <div class="stats-grid">
        <div class="stat-card blue">
          <div class="stat-icon">📚</div>
          <div class="stat-label">Your Subject Records</div>
          <div class="stat-value">—</div>
        </div>
      </div>`;
  }

  const perms = userPermissions.map(p => `
    <div style="display:flex;align-items:center;gap:8px;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.03)">
      <span style="color:var(--accent-green)">✔</span>
      <span style="font-size:0.84rem">${p.replace(/_/g,' ')}</span>
    </div>
  `).join('');

  $('contentArea').innerHTML = `
    <div class="welcome-panel">
      <div class="welcome-icon">${roleEmoji}</div>
      <div>
        <div class="welcome-title">Welcome back, ${currentUser.fullName}!</div>
        <div class="welcome-sub">${currentUser.roleDisplayName} · ${currentUser.email}</div>
        <div style="margin-top:10px">
          <span class="role-badge badge-${role}">${currentUser.roleDisplayName}</span>
        </div>
      </div>
    </div>
    ${statsHtml}
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:18px">
      <div class="table-wrap" style="padding:20px">
        <div class="section-title" style="margin-bottom:16px">🔐 Your Permissions</div>
        ${perms || '<div class="empty-state"><div class="empty-icon">🚫</div><p>No permissions assigned</p></div>'}
      </div>
      <div class="table-wrap" style="padding:20px">
        <div class="section-title" style="margin-bottom:16px">ℹ️ Role Info</div>
        <div style="font-size:0.85rem;line-height:1.7;color:var(--text-secondary)">
          <p><strong style="color:var(--text-primary)">Role:</strong> ${currentUser.roleDisplayName}</p>
          <p style="margin-top:8px"><strong style="color:var(--text-primary)">Description:</strong> ${currentUser.role === 'ADMIN' ? 'Manages users, assigns roles, and maintains the system.' : currentUser.role === 'FACULTY' ? 'Adds and updates student marks and academic data.' : 'Has limited access to view own academic records only.'}</p>
          <p style="margin-top:8px"><strong style="color:var(--text-primary)">Last Login:</strong> ${currentUser.lastLogin}</p>
          <p style="margin-top:8px"><strong style="color:var(--text-primary)">Member Since:</strong> ${currentUser.createdAt}</p>
        </div>
      </div>
    </div>
  `;
}

// ═══════════════════════════════════════
// USERS (ADMIN)
// ═══════════════════════════════════════

async function renderUsers() {
  if (!userPermissions.includes('VIEW_ALL_USERS')) {
    $('contentArea').innerHTML = accessDeniedHtml('Manage Users', 'Only Admin can view and manage users.');
    return;
  }
  const data = await api('GET', '/api/users');
  if (!data.success) {
    $('contentArea').innerHTML = accessDeniedHtml('Manage Users', data.message);
    return;
  }
  const users = data.users || [];
  const rows = users.map(u => `
    <tr>
      <td><code style="font-size:0.75rem;color:var(--text-muted)">#${u.id}</code></td>
      <td>
        <div style="display:flex;align-items:center;gap:10px">
          <div class="avatar sm">${u.fullName.charAt(0)}</div>
          <div>
            <div style="font-weight:600">${u.fullName}</div>
            <div style="font-size:0.75rem;color:var(--text-muted)">@${u.username}</div>
          </div>
        </div>
      </td>
      <td><span style="color:var(--text-secondary);font-size:0.82rem">${u.email}</span></td>
      <td><span class="role-badge badge-${u.role}">${u.roleDisplayName}</span></td>
      <td><span class="chip ${u.active ? 'chip-active' : 'chip-inactive'}">${u.active ? '● Active' : '● Inactive'}</span></td>
      <td style="font-size:0.78rem;color:var(--text-muted)">${u.lastLogin}</td>
      <td>
        <div class="flex gap-8">
          <button class="btn btn-sm btn-secondary" onclick="openRoleModal('${u.id}','${u.fullName}','${u.role}')">🔄 Role</button>
          <button class="btn btn-sm btn-danger" onclick="deleteUser('${u.id}','${u.fullName}')">🗑</button>
        </div>
      </td>
    </tr>
  `).join('');

  $('contentArea').innerHTML = `
    <div class="section-header">
      <div class="section-title">👥 User Management</div>
      <div class="section-actions">
        <button class="btn btn-primary btn-sm" onclick="openCreateUserModal()">➕ Create User</button>
      </div>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th><th>User</th><th>Email</th><th>Role</th><th>Status</th><th>Last Login</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>${rows || '<tr><td colspan="7"><div class="empty-state"><div class="empty-icon">👤</div><p>No users found.</p></div></td></tr>'}</tbody>
      </table>
    </div>
  `;
}

function openCreateUserModal() {
  openModal('➕ Create New User', `
    <form class="modal-form" onsubmit="submitCreateUser(event)">
      <div class="form-group">
        <label>Full Name</label>
        <input type="text" id="newFullName" placeholder="Dr. John Doe" required />
      </div>
      <div class="form-group">
        <label>Username</label>
        <input type="text" id="newUsername" placeholder="johndoe" required />
      </div>
      <div class="form-group">
        <label>Email</label>
        <input type="email" id="newEmail" placeholder="john@college.edu" required />
      </div>
      <div class="form-group">
        <label>Password</label>
        <input type="text" id="newPassword" placeholder="Default: pass123" value="pass123" required />
      </div>
      <div class="form-group">
        <label>Role</label>
        <select id="newRole">
          <option value="ADMIN">👑 Admin</option>
          <option value="FACULTY">🧑‍🏫 Faculty</option>
          <option value="STUDENT" selected>🎒 Student</option>
        </select>
      </div>
      <div class="modal-actions">
        <button type="button" class="btn btn-secondary" onclick="closeModalDirect()">Cancel</button>
        <button type="submit" class="btn btn-primary">Create User</button>
      </div>
    </form>
  `);
}

async function submitCreateUser(e) {
  e.preventDefault();
  const res = await api('POST', '/api/users', {
    fullName: $('newFullName').value,
    username: $('newUsername').value,
    email: $('newEmail').value,
    password: $('newPassword').value,
    role: $('newRole').value
  });
  closeModalDirect();
  toast(res.message, res.success ? 'success' : 'error');
  if (res.success) renderUsers();
}

function openRoleModal(userId, name, currentRole) {
  openModal(`🔄 Change Role — ${name}`, `
    <form class="modal-form" onsubmit="submitChangeRole(event,'${userId}')">
      <div class="form-group">
        <label>New Role</label>
        <select id="newRoleAssign">
          <option value="ADMIN" ${currentRole==='ADMIN'?'selected':''}>👑 Admin</option>
          <option value="FACULTY" ${currentRole==='FACULTY'?'selected':''}>🧑‍🏫 Faculty</option>
          <option value="STUDENT" ${currentRole==='STUDENT'?'selected':''}>🎒 Student</option>
        </select>
      </div>
      <div class="modal-actions">
        <button type="button" class="btn btn-secondary" onclick="closeModalDirect()">Cancel</button>
        <button type="submit" class="btn btn-primary">Update Role</button>
      </div>
    </form>
  `);
}

async function submitChangeRole(e, userId) {
  e.preventDefault();
  const res = await api('PUT', `/api/users/${userId}/role`, { role: $('newRoleAssign').value });
  closeModalDirect();
  toast(res.message, res.success ? 'success' : 'error');
  if (res.success) renderUsers();
}

async function deleteUser(userId, name) {
  if (!confirm(`Delete user "${name}"? This cannot be undone.`)) return;
  const res = await api('DELETE', `/api/users/${userId}`);
  toast(res.message, res.success ? 'success' : 'error');
  if (res.success) renderUsers();
}

// ═══════════════════════════════════════
// ALL MARKS (ADMIN/FACULTY)
// ═══════════════════════════════════════

async function renderAllMarks() {
  if (!userPermissions.includes('VIEW_ALL_MARKS')) {
    $('contentArea').innerHTML = accessDeniedHtml('All Marks', 'Only Admin and Faculty can view all student marks.');
    return;
  }
  const data = await api('GET', '/api/marks');
  if (!data.success) { $('contentArea').innerHTML = accessDeniedHtml('All Marks', data.message); return; }
  const marks = data.marks || [];
  renderMarksTable(marks, true);
}

function renderMarksTable(marks, showEdit = false) {
  const rows = marks.map(m => `
    <tr>
      <td><code style="font-size:0.73rem;color:var(--text-muted)">#${m.id}</code></td>
      <td>
        <div style="font-weight:600">${m.studentName}</div>
        <div style="font-size:0.73rem;color:var(--text-muted)">${m.semester}</div>
      </td>
      <td style="font-weight:500">${m.subject}</td>
      <td>
        <div style="font-weight:700">${m.marks}<span style="color:var(--text-muted);font-weight:400">/${m.maxMarks}</span></div>
        <div class="progress-bar-wrap" style="width:80px;margin-top:4px">
          <div class="progress-bar ${m.percentage>=75?'progress-green':m.percentage>=50?'progress-blue':'progress-amber'}" style="width:${m.percentage}%"></div>
        </div>
      </td>
      <td><span class="grade ${gradeClass(m.grade)}">${m.grade}</span></td>
      <td style="color:var(--text-secondary);font-size:0.82rem">${m.addedBy}</td>
      <td style="font-size:0.75rem;color:var(--text-muted)">${m.updatedAt}</td>
      ${showEdit && userPermissions.includes('UPDATE_MARKS') ? `
        <td><button class="btn btn-sm btn-secondary" onclick="openUpdateMarkModal('${m.id}','${m.studentName}','${m.subject}',${m.marks},${m.maxMarks})">✏️ Edit</button></td>
      ` : (showEdit ? '<td></td>' : '')}
    </tr>
  `).join('') || '<tr><td colspan="8"><div class="empty-state"><div class="empty-icon">📋</div><p>No marks found.</p></div></td></tr>';

  $('contentArea').innerHTML = `
    <div class="section-header">
      <div class="section-title">📝 Student Marks</div>
      ${userPermissions.includes('ADD_MARKS') ? `<button class="btn btn-primary btn-sm" onclick="showSection('addmarks')">➕ Add Marks</button>` : ''}
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th><th>Student</th><th>Subject</th><th>Marks</th><th>Grade</th>
            <th>Added By</th><th>Updated</th>${showEdit ? '<th>Actions</th>' : ''}
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
  `;
}

function openUpdateMarkModal(markId, studentName, subject, marks, maxMarks) {
  openModal(`✏️ Update Marks — ${studentName}`, `
    <form class="modal-form" onsubmit="submitUpdateMarks(event,'${markId}')">
      <p style="color:var(--text-secondary);font-size:0.85rem;margin-bottom:16px">
        Subject: <strong>${subject}</strong> · Current: <strong>${marks}/${maxMarks}</strong>
      </p>
      <div class="form-group">
        <label>New Marks (out of ${maxMarks})</label>
        <input type="number" id="newMarksVal" value="${marks}" min="0" max="${maxMarks}" required />
      </div>
      <div class="modal-actions">
        <button type="button" class="btn btn-secondary" onclick="closeModalDirect()">Cancel</button>
        <button type="submit" class="btn btn-primary">Update</button>
      </div>
    </form>
  `);
}

async function submitUpdateMarks(e, markId) {
  e.preventDefault();
  const res = await api('PUT', `/api/marks/${markId}`, { marks: $('newMarksVal').value });
  closeModalDirect();
  toast(res.message, res.success ? 'success' : 'error');
  if (res.success) renderAllMarks();
}

// ═══════════════════════════════════════
// MY MARKS (STUDENT)
// ═══════════════════════════════════════

async function renderMyMarks() {
  if (!userPermissions.includes('VIEW_OWN_MARKS')) {
    $('contentArea').innerHTML = accessDeniedHtml('My Marks', 'Access denied.');
    return;
  }
  const data = await api('GET', '/api/marks/own');
  const marks = data.marks || [];

  if (!marks.length) {
    $('contentArea').innerHTML = `
      <div class="section-title" style="margin-bottom:20px">🎓 My Academic Record</div>
      <div class="table-wrap"><div class="empty-state"><div class="empty-icon">📋</div><p>No marks have been added for you yet.</p></div></div>`;
    return;
  }

  const totalPct = marks.reduce((a, m) => a + m.percentage, 0) / marks.length;
  const overallGrade = totalPct >= 90 ? 'O' : totalPct >= 80 ? 'A+' : totalPct >= 70 ? 'A' : totalPct >= 60 ? 'B+' : totalPct >= 50 ? 'B' : totalPct >= 40 ? 'C' : 'F';

  const rows = marks.map(m => `
    <tr>
      <td style="font-weight:600">${m.subject}</td>
      <td style="font-size:0.82rem;color:var(--text-secondary)">${m.semester}</td>
      <td>
        <div style="font-weight:700">${m.marks}<span style="color:var(--text-muted);font-weight:400">/${m.maxMarks}</span></div>
        <div class="progress-bar-wrap" style="width:100px;margin-top:4px">
          <div class="progress-bar ${m.percentage>=75?'progress-green':m.percentage>=50?'progress-blue':'progress-amber'}" style="width:${m.percentage}%"></div>
        </div>
      </td>
      <td style="font-weight:600">${m.percentage}%</td>
      <td><span class="grade ${gradeClass(m.grade)}">${m.grade}</span></td>
      <td style="font-size:0.78rem;color:var(--text-muted)">${m.addedBy}</td>
    </tr>
  `).join('');

  $('contentArea').innerHTML = `
    <div class="section-title" style="margin-bottom:20px">🎓 My Academic Record</div>
    <div class="stats-grid" style="margin-bottom:24px">
      <div class="stat-card blue">
        <div class="stat-icon">📚</div>
        <div class="stat-label">Subjects</div>
        <div class="stat-value">${marks.length}</div>
      </div>
      <div class="stat-card green">
        <div class="stat-icon">📊</div>
        <div class="stat-label">Avg Percentage</div>
        <div class="stat-value">${totalPct.toFixed(1)}%</div>
      </div>
      <div class="stat-card teal">
        <div class="stat-icon">🏆</div>
        <div class="stat-label">Overall Grade</div>
        <div class="stat-value"><span class="grade ${gradeClass(overallGrade)}" style="font-size:1.5rem">${overallGrade}</span></div>
      </div>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>Subject</th><th>Semester</th><th>Marks</th><th>%</th><th>Grade</th><th>Faculty</th></tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
  `;
}

// ═══════════════════════════════════════
// ADD MARKS (FACULTY)
// ═══════════════════════════════════════

function renderAddMarks() {
  if (!userPermissions.includes('ADD_MARKS')) {
    $('contentArea').innerHTML = accessDeniedHtml('Add Marks', 'Only Faculty can add student marks.');
    return;
  }
  const students = (statsData.students || []).map(s =>
    `<option value="${s.id}" data-name="${s.fullName}">${s.fullName} (@${s.username})</option>`
  ).join('');

  $('contentArea').innerHTML = `
    <div class="section-title" style="margin-bottom:24px">➕ Add Student Marks</div>
    <div class="table-wrap" style="max-width:560px;padding:28px">
      <form class="modal-form" onsubmit="submitAddMarks(event)" id="addMarksForm">
        <div class="form-group">
          <label>Student</label>
          <select id="amStudent" required onchange="updateStudentName(this)">
            <option value="">-- Select Student --</option>
            ${students}
          </select>
          <input type="hidden" id="amStudentName" />
        </div>
        <div class="form-group">
          <label>Subject</label>
          <select id="amSubject" required>
            <option value="">-- Select Subject --</option>
            <option>Mathematics</option>
            <option>Physics</option>
            <option>Chemistry</option>
            <option>Computer Science</option>
            <option>English</option>
            <option>Biology</option>
            <option>Economics</option>
          </select>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
          <div class="form-group">
            <label>Marks Obtained</label>
            <input type="number" id="amMarks" min="0" max="100" placeholder="e.g. 78" required />
          </div>
          <div class="form-group">
            <label>Maximum Marks</label>
            <input type="number" id="amMaxMarks" value="100" min="1" placeholder="e.g. 100" required />
          </div>
        </div>
        <div class="form-group">
          <label>Semester</label>
          <select id="amSemester" required>
            <option value="Semester 1">Semester 1</option>
            <option value="Semester 2">Semester 2</option>
            <option value="Semester 3">Semester 3</option>
            <option value="Semester 4">Semester 4</option>
            <option value="Semester 5">Semester 5</option>
            <option value="Semester 6">Semester 6</option>
          </select>
        </div>
        <div id="addMarksResult" class="hidden" style="margin-bottom:12px"></div>
        <button type="submit" class="btn btn-primary btn-full">➕ Add Marks</button>
      </form>
    </div>
  `;
}

function updateStudentName(sel) {
  const opt = sel.selectedOptions[0];
  $('amStudentName').value = opt ? opt.getAttribute('data-name') : '';
}

async function submitAddMarks(e) {
  e.preventDefault();
  const res = await api('POST', '/api/marks', {
    studentId: $('amStudent').value,
    studentName: $('amStudentName').value,
    subject: $('amSubject').value,
    marks: $('amMarks').value,
    maxMarks: $('amMaxMarks').value,
    semester: $('amSemester').value
  });
  const el = $('addMarksResult');
  el.className = res.success ? 'error-msg' : 'error-msg';
  el.style.background = res.success ? 'rgba(34,197,94,0.12)' : 'rgba(244,63,94,0.12)';
  el.style.borderColor = res.success ? 'rgba(34,197,94,0.3)' : 'rgba(244,63,94,0.3)';
  el.style.color = res.success ? 'var(--accent-green)' : 'var(--accent-rose)';
  el.textContent = res.message;
  el.classList.remove('hidden');
  if (res.success) {
    toast(res.message, 'success');
    $('addMarksForm').reset();
  } else {
    toast(res.message, 'error');
  }
}

// ═══════════════════════════════════════
// PERMISSIONS
// ═══════════════════════════════════════

async function renderPermissions() {
  const data = await api('GET', '/api/permissions');
  const roles = data.roles || [];
  const roleColors = { ADMIN: 'amber', FACULTY: 'blue', STUDENT: 'green' };
  const roleEmoji = { ADMIN: '👑', FACULTY: '🧑‍🏫', STUDENT: '🎒' };

  const cards = roles.map(r => {
    const perms = r.permissions.map(p => `
      <div class="perm-item">
        <div class="perm-check">✔</div>
        <div>
          <div class="perm-name">${p.displayName}</div>
          <div class="perm-desc">${p.description}</div>
        </div>
      </div>
    `).join('');
    return `
      <div class="perm-card">
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px">
          <span style="font-size:1.5rem">${roleEmoji[r.role]}</span>
          <div>
            <div class="perm-card-title">${r.displayName}</div>
            <span class="role-badge badge-${r.role}">${r.displayName}</span>
          </div>
        </div>
        <div class="perm-card-desc">${r.description}</div>
        <div class="perm-list">${perms}</div>
      </div>
    `;
  }).join('');

  $('contentArea').innerHTML = `
    <div class="section-header">
      <div class="section-title">🔐 Role Permissions Matrix</div>
    </div>
    <div class="perm-grid">${cards}</div>
    <div class="table-wrap" style="margin-top:24px">
      <table>
        <thead>
          <tr>
            <th>Permission</th>
            <th style="text-align:center">👑 Admin</th>
            <th style="text-align:center">🧑‍🏫 Faculty</th>
            <th style="text-align:center">🎒 Student</th>
          </tr>
        </thead>
        <tbody id="permMatrixBody"></tbody>
      </table>
    </div>
  `;
  buildPermMatrix(roles);
}

function buildPermMatrix(roles) {
  const allPerms = new Set();
  roles.forEach(r => r.permissions.forEach(p => allPerms.add(JSON.stringify({name:p.name,displayName:p.displayName}))));
  const adminPerms = new Set(roles.find(r=>r.role==='ADMIN')?.permissions.map(p=>p.name)||[]);
  const facPerms   = new Set(roles.find(r=>r.role==='FACULTY')?.permissions.map(p=>p.name)||[]);
  const stuPerms   = new Set(roles.find(r=>r.role==='STUDENT')?.permissions.map(p=>p.name)||[]);
  const rows = [...allPerms].map(raw => {
    const p = JSON.parse(raw);
    const check = `<span style="color:var(--accent-green);font-size:1.1rem">✔</span>`;
    const cross  = `<span style="color:var(--text-muted);font-size:1.1rem">✕</span>`;
    return `<tr>
      <td style="font-weight:500">${p.displayName}</td>
      <td style="text-align:center">${adminPerms.has(p.name)?check:cross}</td>
      <td style="text-align:center">${facPerms.has(p.name)?check:cross}</td>
      <td style="text-align:center">${stuPerms.has(p.name)?check:cross}</td>
    </tr>`;
  }).join('');
  $('permMatrixBody').innerHTML = rows;
}

// ═══════════════════════════════════════
// AUDIT LOG (ADMIN)
// ═══════════════════════════════════════

async function renderAudit() {
  if (currentUser.role !== 'ADMIN') {
    $('contentArea').innerHTML = accessDeniedHtml('Audit Log', 'Only Admin can view system audit logs.');
    return;
  }
  const data = await api('GET', '/api/audit');
  const logs = data.logs || [];

  const items = logs.map(l => `
    <div class="audit-item">
      <div class="audit-dot ${l.success ? 'success' : 'denied'}"></div>
      <div style="flex:1">
        <div class="audit-action">${l.action.replace(/_/g,' ')}</div>
        <div class="audit-meta">
          <strong>${l.actor}</strong> <span class="role-badge badge-${l.role?.toUpperCase()}" style="font-size:0.68rem;padding:1px 8px">${l.role}</span>
          · <span style="color:${l.success?'var(--accent-green)':'var(--accent-rose)'};font-weight:600">${l.result}</span>
          · ${l.details}
        </div>
      </div>
      <div class="audit-time">${l.timestamp}</div>
    </div>
  `).join('') || '<div class="empty-state"><div class="empty-icon">📋</div><p>No audit entries yet.</p></div>';

  $('contentArea').innerHTML = `
    <div class="section-header">
      <div class="section-title">📋 System Audit Log</div>
      <button class="btn btn-sm btn-secondary" onclick="renderAudit()">🔄 Refresh</button>
    </div>
    <div class="table-wrap">${items}</div>
  `;
}

// ═══════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════

function accessDeniedHtml(section, msg) {
  return `
    <div class="access-denied">
      <div class="access-icon">🚫</div>
      <h2>Access Denied</h2>
      <p>${msg || 'You do not have permission to access this section.'}</p>
      <p style="font-size:0.8rem;color:var(--text-muted);margin-top:8px">Role: <strong>${currentUser?.roleDisplayName}</strong></p>
    </div>
  `;
}

// ═══════════════════════════════════════
// STARTUP
// ═══════════════════════════════════════

async function startup() {
  if (authToken) {
    try {
      const data = await api('GET', '/api/me');
      if (data.user) {
        currentUser = data.user;
        userPermissions = data.permissions || [];
        await initDashboard();
        return;
      }
    } catch (e) {}
    authToken = null;
    localStorage.removeItem('rbac_token');
  }
  showPage('loginPage');
}

startup();
