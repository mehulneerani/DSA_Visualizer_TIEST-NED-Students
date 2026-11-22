let MAX = 0;
const $ = s => document.querySelector(s);
const sleep = ms => new Promise(r => setTimeout(r, ms));

const SID = Math.random().toString(36).slice(2) + Date.now().toString(36);

const FILE_MODE = location.protocol === 'file:';
const SERVER_URL = 'http://localhost:8765';

function srvBanner(show, wrongPage) {
  const b = $('#srv');
  if (!b) return;
  if (!show) { b.hidden = true; return; }
  b.hidden = false;
  b.innerHTML = (wrongPage ? 'This page is not being served by the Java server (for example a VS Code or Live Server preview, or a double-clicked file). While the Java server is running, open <b>' + SERVER_URL + '</b> in Chrome or Edge.' : FILE_MODE
    ? 'This page was opened as a file, so it cannot reach the Java server. Start the server, then open <b>' + SERVER_URL + '</b> in your browser.'
    : 'The Java server is not answering. Start it, keep its window open, then press Try again.')
    + ' <button id="retry">' + (FILE_MODE || wrongPage ? 'Open ' + SERVER_URL.replace('http://', '') : 'Try again') + '</button>';
  $('#retry').onclick = () => { if (FILE_MODE || wrongPage) { location.href = SERVER_URL + '/'; return; } if (!busy) start(sci); };
}

async function api(action, params) {
  let res;
  try {
    res = await fetch('/api/' + action, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams(Object.assign({ sid: SID }, params)) });
  } catch (e) {
    srvBanner(true);
    throw new Error(FILE_MODE ? 'This page was opened as a file. Open ' + SERVER_URL + ' instead.' : 'I cannot reach the Java server. Start it, then press Try again.');
  }
  srvBanner(false);
  let data;
  try { data = await res.json(); } catch (e) { srvBanner(true, true); throw new Error('This page is not talking to the Java server. Open ' + SERVER_URL + ' in Chrome or Edge.'); }
  if (!res.ok || !data.ok) throw new Error(data.error || 'Something went wrong on the server.');
  return data;
}

const OPS = {
  stack: [['push', 'Push', 'Put one on top'], ['pop', 'Pop', 'Take the top one off'], ['peek', 'Peek', 'Look at the top only'], ['empty', 'isEmpty', 'Check if nothing is there'], ['full', 'isFull', 'Check if no room is left']],
  queue: [['enq', 'Enqueue', 'Join at the back'], ['deq', 'Dequeue', 'Serve the one at the front'], ['front', 'Front', 'See who is first'], ['empty', 'isEmpty', 'Check if nobody waits']],
  tree: [['ins', 'Insert', 'Save a new contact'], ['search', 'Search', 'Find a contact'], ['min', 'Smallest', 'First name, A to Z'], ['order', 'A to Z list', 'Read every name in order'], ['del', 'Delete', 'Remove a contact (3 types)']],
  list: [['first', 'Add first', 'Attach at the very front'], ['last', 'Add last', 'Walk to the end, then attach'], ['at', 'Add at position', 'Insert at the position you type'], ['rem', 'Remove first', 'Detach the front one'], ['del', 'Delete a coach', 'Remove the coach you typed'], ['find', 'Find', 'Check one by one from the head']],
  btree: [['add', 'Add person', 'Take the first free spot'], ['pre', 'Pre-order', 'Parent, left, right'], ['in', 'In-order', 'Left, parent, right'], ['post', 'Post-order', 'Left, right, parent'], ['level', 'Level order', 'Row by row (queue)'], ['height', 'Height', 'Count the levels']]
};
const DSN = { stack: 'Stack', queue: 'Queue', list: 'Linked list', tree: 'Binary search tree', btree: 'Binary tree' };
const NEEDS_NAME = ['push', 'enq', 'first', 'last', 'at', 'find', 'ins', 'search', 'del', 'add'];

const SC = [
  {
    id: 'print',
    t: 'Print shop',
    em: '🖨️',
    sub: 'People waiting to print',
    ds: 'queue',
    wait: 2000,
    why: 'People are served in the order they arrived. First in, first out.',
    intro: 'Customers wait in line to print. First choose how many people the line can hold.',
    intro2: 'Type a customer’s name and press Enqueue. Then press Dequeue to let the printer serve the first one.',
    tell: (e, v, a) => ({ enq: v + ' joins the line.', deq: 'The printer takes ' + e.x + '’s paper first, because ' + e.x + ' came first.', front: e.x + ' is first in line. Their paper prints next.', empty: a.length ? 'Someone is still waiting.' : 'Nobody is waiting. The printer rests.' })[e.op],
    draw: (a, e, s) => {
      const ppl = a.map((x, i) => `<div class="ppl ${e.op === 'enq' && e.idx === i ? 'in' : ''}">🧍<b>${esc(x)}</b><span>📄</span></div>`).join('') || '<i>nobody</i>';
      const sheet = e.op === 'deq' && e.x ? `<div class="sheet"><b>${esc(e.x)}</b></div>` : '';
      const done = s.printed.map((x, i) => `<div class="pp ${i === s.printed.length - 1 && e.op === 'deq' ? 'late' : ''}">${esc(x)}</div>`).join('') || '<i>nothing yet</i>';
      return `<div class="shop"><div><h4>Waiting in line</h4><div class="people">${ppl}</div></div><div class="printer">${sheet}<div class="intray"></div><div class="pbody"><i class="led ${sheet ? 'busy' : ''}"></i><i class="slot"></i></div><div class="outtray"></div></div><div><h4>Collected</h4><div class="pile">${done}</div></div></div>`;
    }
  },
  {
    id: 'library',
    t: 'Library desk',
    em: '📚',
    sub: 'Books piled on a desk',
    ds: 'stack',
    why: 'You can only reach the top book. The last one you put down is the first to go back.',
    intro: 'A student piles books on a desk. First choose how many books fit on the desk.',
    intro2: 'Type a book name and press Push to put it on the desk. Pop returns the top book. isFull tells you if the desk has no room left.',
    tell: (e, v, a) => ({ push: v + ' goes on top of the pile.', pop: e.x + ' is on top, so it goes back first.', peek: e.x + ' is the easiest to grab because it is on top.', empty: a.length ? 'There are still books on the desk.' : 'The desk is clear.', full: e.full ? 'The desk is full (' + a.length + ' of ' + MAX + '). The next Push would overflow.' : 'The desk is not full. There is room for ' + (MAX - a.length) + ' more.' })[e.op],
    draw: (a, e, s) => `<div class="lib"><div><h4>Returned to shelf</h4><div class="shelf">${s.returned.map(x => `<span class="spine ${cls(x)}">${esc(x)}</span>`).join('') || '<i>none yet</i>'}</div></div><div><h4>Your desk</h4><div class="desk">${a.slice().reverse().map(x => `<div class="book ${cls(x)}">${esc(x)}</div>`).join('')}</div></div></div>`
  },
  {
    id: 'train',
    t: 'Train coaches',
    em: '🚂',
    sub: 'Coaches hooked in a chain',
    ds: 'list',
    pos: true,
    why: 'Each coach only knows the one behind it. To reach one, you walk from the engine.',
    intro: 'Coaches are hooked one behind another. First choose how many coaches the train can have.',
    intro2: 'Type a coach name and press Add last to hook it at the end. Add first attaches at the front. Type a position (1 = front) to Add at position, or type a name and press Delete a coach.',
    tell: (e, v, a) => ({ first: v + ' is attached at the very front. Only the head changed.', last: v + ' is hooked at the end after walking past ' + (a.length - 1) + ' coach' + (a.length === 2 ? '' : 'es') + '.', at: v + ' is inserted at position ' + e.pos + '. Only two links changed: the coach before it and the new coach.', rem: e.x + ' is detached from the front.', del: e.gone ? e.x + ' is removed from the train. The coach before it is now linked to the coach after it.' : v + ' is not on this train, so nothing was deleted.', find: e.hit ? 'Found ' + v + ' by checking coach by coach.' : v + ' is not on this train.' })[e.op],
    draw: (a, e) => `<div class="trn">` + (a.length ? a.map((x, i) => `<div class="cch ${cls(x)} ${['first', 'last', 'at'].includes(e.op) && e.idx === i ? 'in' : ''}"><span>${i === 0 ? '🚂' : '🚃'}</span>${esc(x)}</div>`).join('<span class="cpl">═</span>') : '<i>no train yet</i>') + `</div>`
  },
  {
    id: 'family',
    t: 'Family tree',
    em: '🌳',
    sub: 'Parents and children, top to bottom',
    ds: 'btree',
    sizeQ: 'How many family members can the tree hold?',
    why: 'Every node has at most two children and there is no sorting rule. Four walks (pre, in, post and level order) read the same tree in four different orders.',
    intro: 'A family tree has one person at the top and up to two children under each person. First choose how many people it can hold.',
    intro2: 'Add a few names. Each new person takes the first free spot, row by row. Then press the four walks and watch the numbers show the visiting order.',
    hint: 'Names must be different. A binary tree has no sorting rule, so the order of names does not matter.',
    draw: (a, e, s) => {
      const P = s.nodes;
      if (!P.length) return '<i>No family members yet</i>';
      const ord = {};
      (e.order || []).forEach((c, k) => ord[c] = k + 1);
      const lv = Math.max(...P.map(n => n.d)) + 1;
      let h = '<div class="fam">';
      for (let d = 0; d < lv; d++) {
        h += `<div class="gen"><small>Level ${d}${d === 0 ? ' (root)' : ''}</small><div class="gl">` + P.filter(n => n.d === d).map(n => `<span class="ppl2 ${cls(n.v)} ${e.x === n.v ? 'in' : ''}">👤 ${esc(n.v)}${ord[n.v] ? `<i class="vno">${ord[n.v]}</i>` : ''}</span>`).join('') + '</div></div>';
      }
      return h + '</div>' + (e.order ? `<p class="cap" style="margin-top:8px">Visiting order: ${e.order.map(esc).join(' → ')}</p>` : '');
    }
  },
  {
    id: 'contacts',
    t: 'Phone contacts',
    em: '📱',
    sub: 'Saved names sorted A to Z',
    ds: 'tree',
    why: 'Names that come earlier go left and later names go right, so each search skips a whole side.',
    intro: 'A phone keeps contacts sorted so search is fast. First choose how many contacts it can hold.',
    intro2: 'Type a contact name and press Insert. Search shows how few contacts it checks. Delete shows the three types of deletion: type 1 leaf (no child), type 2 one child, type 3 two children.',
    draw: (a, e, s) => {
      const l = s.sorted;
      return `<div class="win"><div class="bar"><span class="bk">🔍</span><span class="url">${e.op === 'search' && e.x ? esc(e.x) : 'Search contacts'}</span></div><div class="pg" style="display:block;font-size:16px;padding:6px 12px;text-align:left"><div style="width:100%;text-align:left">${l.length ? l.map(x => `<div style="padding:5px 0;border-bottom:2px dashed var(--muted);${e.op === 'search' && e.hit && e.x.toLowerCase() === x.toLowerCase() ? 'background:var(--yellow);color:var(--on);font-weight:700' : ''}">👤 ${esc(x)}</div>`).join('') : '<i>No contacts yet</i>'}</div></div></div>`;
    }
  }
];

const blank = () => ({ printed: [], returned: [], nodes: [], sorted: [], n: 0 });
let sc, sci = 0, D = [], st = blank(), busy = false;
const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const cls = v => 'c' + ([...String(v)].reduce((s, c) => s + c.charCodeAt(0), 0) % 5 % 4);

function apply(r) { D = r.items; st = { printed: r.served, returned: r.returned, nodes: r.nodes, sorted: r.sorted, n: r.size }; MAX = r.capacity; }

function say(t, m = 'happy') {
  $('#say').textContent = t;
  $('#mouth').setAttribute('d', { happy: 'M28 52 Q40 64 52 52', sad: 'M28 58 Q40 46 52 58', think: 'M30 55 H50' }[m]);
}

function blk(v, i, o, tag) {
  return `<div class="blk ${cls(v)} ${o.add === i ? 'in' : ''} ${o.out === i ? 'out' : ''} ${o.hl === i ? 'hl' : ''}">${esc(v)}${tag ? `<em class="tag">${tag}</em>` : ''}</div>`;
}

function render(o = {}) {
  const a = D, k = sc.ds;
  let h = '';
  if (k === 'tree' || k === 'btree') { $('#stage').innerHTML = treeHTML(o); $('#size').textContent = 'Size: ' + st.n + ' of ' + (MAX || '?'); return; }
  if (!a.length) h = '<p class="empty">Nothing here yet.</p>';
  else if (k === 'stack') h = '<div class="stack">' + a.map((v, i) => blk(v, i, o, i === a.length - 1 ? 'top' : '')).reverse().join('') + '</div>';
  else if (k === 'queue') h = '<div class="row queue">' + a.map((v, i) => blk(v, i, o, [i === 0 ? 'front' : '', i === a.length - 1 ? 'back' : ''].filter(Boolean).join(' / '))).join('') + '</div>';
  else h = '<div class="row">' + a.map((v, i) => blk(v, i, o, i === 0 ? 'head' : '') + '<span class="arr">→</span>').join('') + '<span class="nul">null</span></div>';
  $('#stage').innerHTML = h;
  $('#size').textContent = 'Size: ' + a.length + ' of ' + (MAX || '?');
}

const btns = d => document.querySelectorAll('#ops button').forEach(b => b.disabled = d);
function scene(e) { $('#scene').innerHTML = sc.draw(D, e, st); }

function treeHTML(o) {
  const P = st.nodes;
  if (!P.length) return '<p class="empty">Nothing here yet.</p>';
  const W = (P.length - 1) * 66 + 70, H = Math.max(...P.map(n => n.d)) * 74 + 70;
  const X = n => n.x * 66 + 35, Y = n => n.d * 74 + 35, hl = o.hl || [];
  let g = '';
  P.forEach(n => { [n.l, n.r].forEach(c => { if (c >= 0) g += `<line x1="${X(n)}" y1="${Y(n)}" x2="${X(P[c])}" y2="${Y(P[c])}" stroke="var(--ink)" stroke-width="3"/>`; }); });
  P.forEach(n => {
    const f = ['--blue', '--mint', '--yellow', '--coral'][+cls(n.v).slice(1)], lab = n.v.length > 7 ? n.v.slice(0, 6) + '…' : n.v;
    g += `<g class="${hl.includes(n.v) ? 'tnh' : ''} ${o.add === n.v ? 'in' : ''}"><circle cx="${X(n)}" cy="${Y(n)}" r="27" style="fill:var(${f})" stroke="var(--ink)" stroke-width="3"/><text x="${X(n)}" y="${Y(n) + 5}" text-anchor="middle" font-size="13" font-weight="700" fill="var(--on)">${esc(lab)}</text></g>`;
  });
  return `<svg width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="${DSN[sc.ds]}">${g}</svg>`;
}

async function runTree(o, v, r) {
  const e = { op: o };
  let n = 0, m = 'happy', t = '', story = '';
  const pl = k => k === 1 ? '' : 's';
  const walk = async nodes => { for (let k = 1; k <= nodes.length; k++) { n = k; render({ hl: nodes.slice(0, k) }); await sleep(450); } };
  if (o === 'ins') {
    if (r.status === 'full') { t = 'Full! There is no room for ' + v + '. This is called overflow.'; m = 'sad'; }
    else {
      await walk(r.path);
      if (r.status === 'duplicate') { t = v + ' is already saved.'; m = 'think'; }
      else {
        apply(r); e.x = v; render({ add: v });
        t = v + ' was placed after ' + n + ' comparison' + pl(n) + '. Names that come earlier go left, later names go right.';
        story = v + ' is saved. The list stays sorted A to Z by itself.';
      }
    }
  } else if (o === 'search') {
    await walk(r.path);
    e.hit = r.status === 'found'; e.x = v;
    t = r.wasEmpty ? 'The tree is empty.' : e.hit ? 'Found ' + v + ' after ' + n + ' step' + pl(n) + '. Each step skipped a whole side.' : v + ' is not saved. I checked ' + n + ' node' + pl(n) + '.';
    m = e.hit ? 'happy' : 'think';
    story = e.hit ? 'The app finds ' + v + ' without reading every contact.' : v + ' is not in the contacts.';
  } else if (o === 'min') {
    if (r.status === 'underflow') { t = 'The tree is empty.'; m = 'sad'; }
    else {
      await walk(r.path);
      e.x = r.x;
      t = 'Keep going left. ' + r.x + ' is the smallest after ' + n + ' step' + pl(n) + '.';
      story = r.x + ' comes first in A to Z.';
    }
  } else if (o === 'order') {
    if (r.status === 'underflow') { t = 'The tree is empty.'; m = 'sad'; }
    else {
      for (const x of r.order) { n++; render({ hl: [x] }); await sleep(450); }
      t = 'Left side, then the node, then the right side gives sorted order.';
      story = 'A to Z: ' + r.order.join(', ');
    }
  } else if (o === 'del') {
    if (r.wasEmpty) { t = 'The tree is empty, so there is nothing to delete.'; m = 'sad'; }
    else if (r.status === 'notfound') { await walk(r.path); t = v + ' is not saved, so there is nothing to delete.'; m = 'think'; story = t; }
    else {
      await walk(r.path);
      if (r.case === 'two') { for (let k = 0; k < r.spath.length; k++) { n++; render({ hl: r.path.concat(r.spath.slice(0, k + 1)) }); await sleep(450); } }
      apply(r); e.x = v; render();
      t = r.case === 'leaf' ? 'Deletion type 1 (leaf node): ' + v + ' has no children, so it is simply cut off. Nothing else moves.'
        : r.case === 'one' ? 'Deletion type 2 (one child): ' + v + ' has one child, so that child moves up into its place.'
        : 'Deletion type 3 (two children): the smallest name on ' + v + '’s right side is ' + r.succ + ' (its in-order successor), so ' + r.succ + ' takes ' + v + '’s place and its old spot is closed up.';
      story = v + ' is deleted. The list stays sorted A to Z.';
    }
  }
  apply(r); render();
  say(t, m);
  if (story || m === 'sad') $('#story').textContent = m === 'sad' ? t : story;
  scene(e);
  await sleep(250);
}

async function runLinear(o, v, r) {
  const a = D, e = { op: o };
  let n = 0, m = 'happy', t = '';
  const full = r.status === 'full', none = r.status === 'underflow';
  const fullMsg = 'Full! There is no room for ' + v + '. This is called overflow.';
  const walkTo = async k => { for (let i = 0; i < k; i++) { render({ hl: i }); n++; await sleep(380); } };
  if (o === 'empty') { t = r.empty ? 'Yes, it is empty.' : 'No, it holds ' + a.length + '.'; }
  else if (o === 'full') { e.full = r.full; t = r.full ? 'Yes, it is full. No more can be added.' : 'No, it is not full. There is room for ' + (MAX - a.length) + ' more.'; }
  else if (o === 'push' || o === 'enq' || o === 'first') {
    if (full) { t = fullMsg; m = 'sad'; }
    else {
      apply(r); e.idx = o === 'first' ? 0 : D.length - 1; render({ add: e.idx }); e.x = v;
      t = o === 'push' ? v + ' went on top. One step, nothing else moved.' : o === 'enq' ? v + ' joined the back. One step, nobody else moved.' : v + ' is the new head. Only one pointer changed.';
    }
  } else if (o === 'last') {
    if (full) { t = fullMsg; m = 'sad'; }
    else {
      await walkTo(a.length);
      apply(r); e.idx = D.length - 1; e.x = v; render({ add: e.idx });
      t = 'I walked past ' + n + ' coach' + (n === 1 ? '' : 'es') + ' to reach the end, then attached ' + v + '.'; m = 'think';
    }
  } else if (o === 'at') {
    if (r.status === 'badpos') { t = 'Position must be a whole number from 1 to ' + r.max + '.'; m = 'sad'; }
    else if (full) { t = fullMsg; m = 'sad'; }
    else {
      await walkTo(r.pos - 1);
      apply(r); e.idx = r.pos - 1; e.x = v; e.pos = r.pos; render({ add: e.idx });
      t = r.pos === 1 ? v + ' is the new head. Position 1 is the front, so only the head changed.' : 'I walked past ' + (r.pos - 1) + ' coach' + (r.pos === 2 ? '' : 'es') + ' and linked ' + v + ' in at position ' + r.pos + '.'; m = 'think';
    }
  } else if (o === 'del') {
    if (r.wasEmpty) { t = 'The list is empty, so there is nothing to delete.'; m = 'sad'; }
    else if (r.status === 'notfound') { await walkTo(a.length); t = v + ' is not on this train. I checked all ' + n + ' to be sure.'; m = 'think'; }
    else {
      await walkTo(r.index);
      render({ out: r.index }); await sleep(320);
      n = r.index + 1; e.x = v; e.gone = true; apply(r); render();
      t = v + ' was found after ' + n + ' coach' + (n === 1 ? '' : 'es') + ' and unhooked. The neighbours are linked together.';
    }
  } else if (o === 'pop' || o === 'deq' || o === 'rem') {
    if (none) { t = 'Nothing to take out. This is called underflow.'; m = 'sad'; }
    else {
      const i = o === 'pop' ? a.length - 1 : 0;
      render({ out: i }); await sleep(320);
      e.x = r.x; apply(r); render();
      t = e.x + ' left from the ' + (o === 'pop' ? 'top' : 'front') + '. One step.';
    }
  } else if (o === 'peek' || o === 'front') {
    if (none) { t = 'Nothing to look at.'; m = 'sad'; }
    else {
      const i = o === 'peek' ? a.length - 1 : 0;
      render({ hl: i }); e.x = r.x;
      t = 'It is ' + r.x + '. I only looked, nothing was removed.'; await sleep(800); render();
    }
  } else if (o === 'find') {
    await walkTo(r.status === 'found' ? r.index + 1 : a.length);
    const hit = r.status === 'found';
    t = r.wasEmpty ? 'The list is empty.' : hit ? 'Found ' + v + ' after checking ' + n + ' coach' + (n === 1 ? '' : 'es') + '.' : 'No ' + v + ' here. I checked all ' + n + ' to be sure.';
    m = hit ? 'happy' : 'think'; e.hit = hit; render();
  }
  apply(r);
  say(t, m);
  $('#story').textContent = m === 'sad' ? t : sc.tell(e, v, D);
  scene(e);
  await sleep(e.op === 'deq' && e.x && sc.wait ? sc.wait : 250);
}

async function runBTree(o, v, r) {
  const e = { op: o };
  let m = 'happy', t = '', story = '';
  const pl = k => k === 1 ? '' : 's';
  const walk = async nodes => { for (let k = 1; k <= nodes.length; k++) { render({ hl: nodes.slice(0, k) }); await sleep(450); } };
  if (o === 'add') {
    if (r.status === 'full') { t = 'Full! There is no room for ' + v + '. This is called overflow.'; m = 'sad'; story = t; }
    else if (r.status === 'duplicate') { t = v + ' is already in the tree. Every name must be different.'; m = 'think'; story = t; }
    else {
      await walk(r.path); apply(r); e.x = v; render({ add: v });
      t = r.path.length ? v + ' takes the first free spot, found by going down from the root past ' + r.path.length + ' node' + pl(r.path.length) + '. A binary tree has no sorting rule, it just fills each row from left to right.' : v + ' becomes the root, the very top of the family.';
      story = v + ' joins the family tree.';
    }
  } else if (o === 'height') {
    if (r.status === 'underflow') { t = 'The tree is empty.'; m = 'sad'; story = t; }
    else { t = 'The tree has ' + r.height + ' level' + pl(r.height) + '. Height tells you the longest walk from the top to the bottom.'; story = 'The family spans ' + r.height + ' generation' + pl(r.height) + '.'; }
  } else {
    if (r.status === 'underflow') { t = 'The tree is empty.'; m = 'sad'; story = t; }
    else {
      for (let k = 1; k <= r.order.length; k++) { render({ hl: r.order.slice(0, k) }); await sleep(500); }
      e.order = r.order;
      t = { pre: 'Pre-order: the parent first, then the left side, then the right side.', in: 'In-order: the left side, then the parent, then the right side.', post: 'Post-order: both sides first and the parent last, like finishing the children’s work before the parent.', level: 'Level order: row by row from the top, using a queue of waiting people.' }[o];
      story = { pre: 'Pre-order', in: 'In-order', post: 'Post-order', level: 'Level order' }[o] + ': ' + r.order.join(', ');
    }
  }
  if (o !== 'add') apply(r);
  if (o === 'add' || r.status !== 'ok' || o === 'height') render({}); else render({ hl: r.order });
  say(t, m);
  $('#story').textContent = story || t;
  scene(e);
  await sleep(250);
}

async function run(o) {
  if (busy) return;
  if (!MAX) { say('Set the size first, then try an operation.', 'think'); return; }
  const v = $('#val').value.trim();
  const pos = $('#pos').value.trim();
  if (NEEDS_NAME.includes(o) && !v) { say('Type a name first, then press the operation.', 'think'); return; }
  if (o === 'at' && !pos) { say('Type a position number first (1 = front), then press Add at position.', 'think'); return; }
  busy = true; btns(true); $('#val').value = ''; $('#pos').value = '';
  document.querySelectorAll('#ops button').forEach(b => b.classList.toggle('now', b.dataset.op === o));
  try {
    const r = await api('op', { scenario: sc.id, op: o, value: v, pos: pos });
    if (sc.ds === 'tree') await runTree(o, v, r); else if (sc.ds === 'btree') await runBTree(o, v, r); else await runLinear(o, v, r);
  } catch (err) { say(err.message, 'sad'); }
  finally { busy = false; btns(false); }
}

function start(i) {
  sc = SC[i]; sci = i; D = []; st = blank(); busy = true; MAX = 0;
  document.querySelectorAll('#menu button').forEach((b, j) => b.setAttribute('aria-current', j === i));
  $('#stt').textContent = sc.em + ' ' + sc.t; $('#dsn').textContent = 'Uses: ' + DSN[sc.ds]; $('#why').textContent = sc.why;
  $('#story').textContent = sc.intro; $('#vt').textContent = 'Behind the scenes: ' + DSN[sc.ds];
  $('#sz').value = ''; $('#val').value = ''; $('#pos').value = ''; $('#szmsg').textContent = '';
  $('#szlabel').textContent = '1. ' + (sc.sizeQ || 'How many can it hold?');
  $('#posrow').hidden = !sc.pos;
  const hn = $('#hint'); hn.textContent = sc.hint || ''; hn.hidden = !sc.hint;
  $('#ops').innerHTML = OPS[sc.ds].map(o => `<button data-op="${o[0]}"><b>${o[1]}</b><small>${o[2]}</small></button>`).join('');
  $('#ops').querySelectorAll('button').forEach(b => b.onclick = () => run(b.dataset.op));
  render(); scene({});
  say('Set the size first. Then add something and watch the ' + DSN[sc.ds].toLowerCase() + ' work.', 'think');
  api('reset', { scenario: sc.id }).catch(err => say(err.message, 'sad')).then(() => { busy = false; });
}

async function setSize() {
  if (busy) return;
  const n = parseInt($('#sz').value);
  if (!(n >= 1 && n <= 10)) { $('#szmsg').textContent = 'Please enter a number from 1 to 10.'; return; }
  busy = true;
  try {
    apply(await api('size', { scenario: sc.id, size: n }));
    $('#szmsg').textContent = 'Size is ' + n + '. The ' + DSN[sc.ds].toLowerCase() + ' is empty and ready.';
    document.querySelectorAll('#ops button').forEach(b => b.classList.remove('now'));
    render(); scene({}); $('#story').textContent = sc.intro2;
    say('Size set to ' + n + '. Now type a name and press an operation.');
  } catch (err) { say(err.message, 'sad'); }
  finally { busy = false; }
}

$('#setsz').onclick = setSize;
$('#sz').onkeydown = e => { if (e.key === 'Enter') setSize(); };
$('#rs').onclick = () => { if (!busy) start(sci); };
SC.forEach((s, i) => { const b = document.createElement('button'); b.innerHTML = s.em + ' ' + s.t + '<small>' + s.sub + '</small>'; b.onclick = () => { if (!busy) start(i); }; $('#menu').appendChild(b); });
start(0);
