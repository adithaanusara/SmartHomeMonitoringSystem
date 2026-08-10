import { initializeApp } from 'https://www.gstatic.com/firebasejs/12.17.1/firebase-app.js';
import {
  getAuth,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut,
} from 'https://www.gstatic.com/firebasejs/12.17.1/firebase-auth.js';
import {
  getDatabase,
  onValue,
  push,
  ref,
  serverTimestamp,
  update,
} from 'https://www.gstatic.com/firebasejs/12.17.1/firebase-database.js';

import { HEARTBEAT_INTERVAL_MS, HOUSE_ID, firebaseConfig } from './config.js';

/**
 * Hardware Simulator.
 *
 * Stands in for the physical appliances. It subscribes to the same Realtime Database nodes as the
 * Android client, so a toggle on the phone lights a lamp here within the round trip, and a fault
 * injected here propagates back to the phone without a refresh. That two-way propagation is the
 * spec's bidirectional-sync requirement made visible.
 */

const STATUS = { ON: 'ON', OFF: 'OFF', ERROR: 'ERROR', DISCONNECTED: 'DISCONNECTED' };
const SOURCE_SIMULATOR = 'SIMULATOR';

const GLYPH = {
  OUTLET: '🔌',
  MULTI_SWITCH: '🎛️',
  LIGHT: '💡',
  HAZARD: '🔥',
  CAMERA: '📷',
};

const el = {
  linkState: document.getElementById('link-state'),
  signOut: document.getElementById('sign-out'),
  authView: document.getElementById('auth-view'),
  authForm: document.getElementById('auth-form'),
  authHouse: document.getElementById('auth-house'),
  authEmail: document.getElementById('auth-email'),
  authPassword: document.getElementById('auth-password'),
  authSubmit: document.getElementById('auth-submit'),
  authError: document.getElementById('auth-error'),
  houseView: document.getElementById('house-view'),
  floors: document.getElementById('floors'),
  emptyState: document.getElementById('empty-state'),
  heartbeatNote: document.getElementById('heartbeat-note'),
  fatal: document.getElementById('fatal'),
};

/** Devices whose heartbeat the operator has deliberately cut, to demo the DISCONNECTED path. */
const mutedHeartbeats = new Set();

let db = null;
let devices = {};
let floors = {};
let heartbeatTimer = null;
let countdownTimer = null;
let unsubscribes = [];

// ---------------------------------------------------------------- bootstrap

function fatal(message) {
  el.fatal.hidden = false;
  el.fatal.innerHTML = message;
  el.authView.hidden = true;
  el.houseView.hidden = true;
}

const missingConfig = ['apiKey', 'databaseURL', 'projectId'].filter(
  (key) => !firebaseConfig[key],
);

if (missingConfig.length > 0) {
  fatal(
    '<strong>Not configured yet.</strong><br />Missing <code>' +
      missingConfig.join('</code>, <code>') +
      '</code> in <code>simulator/config.js</code>.<br /><br />' +
      'Register a <strong>Web</strong> app in Firebase console → Project settings → Your apps, ' +
      'then copy <code>apiKey</code> and <code>appId</code> from it. The Android key will not ' +
      'work in a browser.',
  );
} else {
  start();
}

function start() {
  const app = initializeApp(firebaseConfig);
  const auth = getAuth(app);
  db = getDatabase(app);

  el.authHouse.textContent = HOUSE_ID;
  el.emptyState.querySelector('code').textContent = `devices/${HOUSE_ID}`;

  onAuthStateChanged(auth, (user) => {
    if (user) {
      el.authView.hidden = true;
      el.houseView.hidden = false;
      el.signOut.hidden = false;
      setLinkState('Live', true);
      subscribe();
    } else {
      el.authView.hidden = false;
      el.houseView.hidden = true;
      el.signOut.hidden = true;
      setLinkState('Offline', false);
      teardown();
    }
  });

  el.authForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    el.authError.hidden = true;
    el.authSubmit.disabled = true;
    try {
      await signInWithEmailAndPassword(auth, el.authEmail.value.trim(), el.authPassword.value);
    } catch (error) {
      el.authError.hidden = false;
      el.authError.textContent = friendlyAuthError(error);
    } finally {
      el.authSubmit.disabled = false;
    }
  });

  el.signOut.addEventListener('click', () => signOut(auth));
}

function friendlyAuthError(error) {
  const code = error?.code ?? '';
  if (code.includes('invalid-credential') || code.includes('wrong-password')) {
    return 'Incorrect email or password.';
  }
  if (code.includes('user-not-found')) return 'No account found for this email.';
  if (code.includes('configuration-not-found')) {
    return 'Email/password sign-in is not enabled for this Firebase project yet.';
  }
  if (code.includes('network')) return 'No connection to Firebase.';
  return error?.message ?? 'Sign-in failed.';
}

function setLinkState(text, live) {
  el.linkState.textContent = text;
  el.linkState.className = `pill ${live ? 'pill-live' : 'pill-idle'}`;
}

// ---------------------------------------------------------------- subscriptions

function subscribe() {
  teardown();

  unsubscribes.push(
    onValue(
      ref(db, `floors/${HOUSE_ID}`),
      (snapshot) => {
        floors = snapshot.val() ?? {};
        render();
      },
      (error) => fatal(`Cannot read floors: ${error.message}`),
    ),
  );

  unsubscribes.push(
    onValue(
      ref(db, `devices/${HOUSE_ID}`),
      (snapshot) => {
        const previous = devices;
        devices = snapshot.val() ?? {};
        restoreReconnectedDevices(previous);
        render();
      },
      (error) =>
        fatal(
          `Cannot read devices: ${error.message}<br />` +
            'Is this account a member of the house? Check <code>houses/' +
            `${HOUSE_ID}/members</code>.`,
        ),
    ),
  );

  heartbeatTimer = setInterval(sendHeartbeats, HEARTBEAT_INTERVAL_MS);
  sendHeartbeats();

  countdownTimer = setInterval(updateCountdowns, 1000);
}

function teardown() {
  unsubscribes.forEach((off) => off());
  unsubscribes = [];
  clearInterval(heartbeatTimer);
  clearInterval(countdownTimer);
  heartbeatTimer = null;
  countdownTimer = null;
}

// ---------------------------------------------------------------- heartbeat

/**
 * Writes `lastSeen` for every device the simulator is currently representing.
 *
 * All devices go in a single multi-path update rather than one write each: it is one round trip
 * instead of ten, and every device's timestamp is then taken from the same server clock.
 */
function sendHeartbeats() {
  const ids = Object.keys(devices).filter((id) => !mutedHeartbeats.has(id));
  if (ids.length === 0) {
    updateHeartbeatNote();
    return;
  }

  const updates = {};
  for (const id of ids) {
    updates[`/devices/${HOUSE_ID}/${id}/lastSeen`] = serverTimestamp();
  }
  update(ref(db), updates).catch((error) =>
    console.error('[simulator] heartbeat write failed', error),
  );
  updateHeartbeatNote();
}

function updateHeartbeatNote() {
  const total = Object.keys(devices).length;
  const muted = mutedHeartbeats.size;
  el.heartbeatNote.textContent = muted
    ? `heartbeat: ${total - muted}/${total} reporting · ${muted} cut`
    : `heartbeat: ${total}/${total} reporting every ${HEARTBEAT_INTERVAL_MS / 1000}s`;
}

/**
 * Power-on reset.
 *
 * The worker marks a device DISCONNECTED but never clears it, because only the device knows what
 * state it came back in. This is the other half of that contract: when the simulator resumes
 * responsibility for a device, it reports OFF — what a real appliance reads after a power cycle.
 */
function restoreReconnectedDevices(previous) {
  const updates = {};

  for (const [id, device] of Object.entries(devices)) {
    if (device?.status !== STATUS.DISCONNECTED) continue;
    if (mutedHeartbeats.has(id)) continue;
    // Only act on the transition, not on every snapshot of an already-disconnected device.
    if (previous[id]?.status === STATUS.DISCONNECTED) continue;

    updates[`/devices/${HOUSE_ID}/${id}/status`] = STATUS.OFF;
    Object.assign(
      updates,
      eventPatch(id, { from: STATUS.DISCONNECTED, to: STATUS.OFF }),
    );
  }

  if (Object.keys(updates).length > 0) {
    update(ref(db), updates).catch((error) =>
      console.error('[simulator] power-on reset failed', error),
    );
  }
}

// ---------------------------------------------------------------- writes

function eventPatch(deviceId, { from, to }) {
  const key = push(ref(db, `events/${HOUSE_ID}/${deviceId}`)).key;
  return {
    [`/events/${HOUSE_ID}/${deviceId}/${key}`]: {
      ts: serverTimestamp(),
      from,
      to,
      source: SOURCE_SIMULATOR,
      actorUid: null,
    },
  };
}

/**
 * Drives a device from the "hardware" side — the externally-originated change the spec asks the
 * mobile viewport to pick up without a manual refresh.
 */
function setStatus(deviceId, nextStatus) {
  const device = devices[deviceId];
  if (!device) return;

  const base = `/devices/${HOUSE_ID}/${deviceId}`;
  const updates = {
    [`${base}/status`]: nextStatus,
    ...eventPatch(deviceId, { from: device.status ?? STATUS.OFF, to: nextStatus }),
  };

  // Mirrors DeviceRepository on the client: a hazard device turning on must arm the worker's
  // cutoff, and turning off must disarm it.
  if (device.safety) {
    updates[`${base}/safety/onSince`] = nextStatus === STATUS.ON ? serverTimestamp() : null;
  }

  if (device.channels && nextStatus !== STATUS.ON) {
    for (const channelId of Object.keys(device.channels)) {
      updates[`${base}/channels/${channelId}/status`] = nextStatus;
    }
  }

  update(ref(db), updates).catch((error) =>
    console.error(`[simulator] failed to set ${deviceId} -> ${nextStatus}`, error),
  );
}

function setChannelStatus(deviceId, channelId, nextStatus) {
  const device = devices[deviceId];
  if (!device) return;

  const base = `/devices/${HOUSE_ID}/${deviceId}`;
  const others = Object.entries(device.channels ?? {})
    .filter(([id]) => id !== channelId)
    .some(([, channel]) => channel?.status === STATUS.ON);
  const unitStatus = nextStatus === STATUS.ON || others ? STATUS.ON : STATUS.OFF;

  update(ref(db), {
    [`${base}/channels/${channelId}/status`]: nextStatus,
    [`${base}/status`]: unitStatus,
    ...eventPatch(deviceId, {
      from: device.channels?.[channelId]?.status ?? STATUS.OFF,
      to: nextStatus,
    }),
  }).catch((error) => console.error(`[simulator] failed to set channel ${channelId}`, error));
}

function toggleHeartbeat(deviceId) {
  if (mutedHeartbeats.has(deviceId)) {
    mutedHeartbeats.delete(deviceId);
    sendHeartbeats();
  } else {
    mutedHeartbeats.add(deviceId);
  }
  render();
}

// ---------------------------------------------------------------- rendering

function render() {
  const entries = Object.entries(devices);
  el.emptyState.hidden = entries.length > 0;

  const floorEntries = Object.entries(floors).sort(
    ([, a], [, b]) => (a?.level ?? 0) - (b?.level ?? 0),
  );

  // Devices whose floorId matches nothing still need somewhere to appear.
  const knownFloorIds = new Set(floorEntries.map(([id]) => id));
  const orphaned = entries.filter(([, d]) => !knownFloorIds.has(d?.floorId));

  const sections = floorEntries.map(([floorId, floor]) => ({
    title: floor?.name ?? floorId,
    devices: entries.filter(([, d]) => d?.floorId === floorId),
  }));

  if (orphaned.length > 0) {
    sections.push({ title: 'Unassigned', devices: orphaned });
  }

  el.floors.replaceChildren(
    ...sections
      .filter((section) => section.devices.length > 0)
      .map((section) => renderFloor(section)),
  );

  updateHeartbeatNote();
}

function renderFloor({ title, devices: floorDevices }) {
  const section = document.createElement('section');
  section.className = 'floor';

  const head = document.createElement('div');
  head.className = 'floor-head';
  const heading = document.createElement('h2');
  heading.textContent = title;
  const count = document.createElement('span');
  count.className = 'count';
  const onCount = floorDevices.filter(([, d]) => d?.status === STATUS.ON).length;
  count.textContent = `${onCount} of ${floorDevices.length} on`;
  head.append(heading, count);

  const grid = document.createElement('div');
  grid.className = 'grid';
  grid.append(
    ...floorDevices
      .sort(([, a], [, b]) => (a?.name ?? '').localeCompare(b?.name ?? ''))
      .map(([id, device]) => renderDevice(id, device)),
  );

  section.append(head, grid);
  return section;
}

function renderDevice(id, device) {
  const status = device?.status ?? STATUS.OFF;
  const type = device?.type ?? 'OUTLET';

  const card = document.createElement('article');
  card.className = 'device';
  card.dataset.status = status;
  card.dataset.deviceId = id;

  // Head
  const head = document.createElement('div');
  head.className = 'device-head';
  const names = document.createElement('div');
  const name = document.createElement('div');
  name.className = 'device-name';
  name.textContent = device?.name ?? id;
  const typeLabel = document.createElement('div');
  typeLabel.className = 'device-type';
  typeLabel.textContent = type.replace('_', ' ');
  names.append(name, typeLabel);
  const pill = document.createElement('span');
  pill.className = `pill pill-${status}`;
  pill.textContent = status;
  head.append(names, pill);

  // Appliance visual
  const appliance = document.createElement('div');
  appliance.className = 'appliance';
  if (type === 'CAMERA' && device?.camera?.snapshotUrl) {
    const img = document.createElement('img');
    img.src = device.camera.snapshotUrl;
    img.alt = `${device?.name ?? id} snapshot`;
    img.loading = 'lazy';
    appliance.append(img);
  } else {
    appliance.textContent = GLYPH[type] ?? '⚙️';
  }

  card.append(head, appliance);

  // Channels
  if (type === 'MULTI_SWITCH' && device?.channels) {
    const channels = document.createElement('div');
    channels.className = 'channels';
    for (const [channelId, channel] of Object.entries(device.channels)) {
      channels.append(renderChannel(id, channelId, channel));
    }
    card.append(channels);
  }

  // Hazard countdown
  if (device?.safety?.onSince && status === STATUS.ON) {
    const countdown = document.createElement('div');
    countdown.className = 'countdown';
    countdown.dataset.countdownFor = id;
    card.append(countdown);
  }

  // Staleness readout
  if (device?.lastSeen) {
    const stale = document.createElement('div');
    stale.className = 'stale';
    stale.dataset.staleFor = id;
    card.append(stale);
  }

  card.append(renderActions(id, device, status));
  return card;
}

function renderChannel(deviceId, channelId, channel) {
  const row = document.createElement('div');
  row.className = 'channel';

  const label = document.createElement('div');
  label.className = 'channel-label';
  const led = document.createElement('i');
  led.className = 'led';
  led.dataset.on = String(channel?.status === STATUS.ON);
  const text = document.createElement('span');
  text.textContent = channel?.label ?? channelId;
  label.append(led, text);

  const button = document.createElement('button');
  button.className = 'btn';
  button.textContent = channel?.status === STATUS.ON ? 'Off' : 'On';
  button.addEventListener('click', () =>
    setChannelStatus(
      deviceId,
      channelId,
      channel?.status === STATUS.ON ? STATUS.OFF : STATUS.ON,
    ),
  );

  row.append(label, button);
  return row;
}

/**
 * Fault injection.
 *
 * These are what prove externally-driven state reaches the phone: none of them originate in the
 * app, and all of them must appear on it within a second and without a refresh.
 */
function renderActions(id, device, status) {
  const actions = document.createElement('div');
  actions.className = 'actions';

  const toggle = document.createElement('button');
  toggle.className = 'btn';
  toggle.textContent = status === STATUS.ON ? 'Switch off' : 'Switch on';
  toggle.addEventListener('click', () =>
    setStatus(id, status === STATUS.ON ? STATUS.OFF : STATUS.ON),
  );

  const error = document.createElement('button');
  error.className = 'btn btn-danger';
  error.textContent = status === STATUS.ERROR ? 'Clear fault' : 'Fault';
  error.addEventListener('click', () =>
    setStatus(id, status === STATUS.ERROR ? STATUS.OFF : STATUS.ERROR),
  );

  const muted = mutedHeartbeats.has(id);
  const heartbeat = document.createElement('button');
  heartbeat.className = 'btn btn-warn';
  heartbeat.textContent = muted ? 'Resume beat' : 'Cut beat';
  heartbeat.title = muted
    ? 'Resume heartbeats; the device reports OFF on reconnect'
    : 'Stop heartbeats; the worker marks this DISCONNECTED after 30s';
  heartbeat.addEventListener('click', () => toggleHeartbeat(id));

  actions.append(toggle, error, heartbeat);
  return actions;
}

/** Ticks the hazard countdown and the staleness readout without re-rendering the whole tree. */
function updateCountdowns() {
  const now = Date.now();

  for (const node of document.querySelectorAll('[data-countdown-for]')) {
    const device = devices[node.dataset.countdownFor];
    const onSince = device?.safety?.onSince;
    const limit = device?.safety?.maxOnDurationSec;
    if (!onSince || !limit) continue;

    const remaining = Math.max(0, Math.round((onSince + limit * 1000 - now) / 1000));
    node.textContent = `Safety cutoff in ${formatDuration(remaining)}`;
    node.dataset.critical = String(remaining <= 30);
  }

  for (const node of document.querySelectorAll('[data-stale-for]')) {
    const id = node.dataset.staleFor;
    const lastSeen = devices[id]?.lastSeen;
    if (!lastSeen) continue;
    const age = Math.max(0, Math.round((now - lastSeen) / 1000));
    node.textContent = mutedHeartbeats.has(id)
      ? `heartbeat cut · last seen ${age}s ago`
      : `last seen ${age}s ago`;
  }
}

function formatDuration(totalSeconds) {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return minutes > 0 ? `${minutes}m ${String(seconds).padStart(2, '0')}s` : `${seconds}s`;
}
