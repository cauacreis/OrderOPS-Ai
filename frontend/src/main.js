import './style.css';

// System State
let orders = [];
let drivers = [];
let logs = [];
let ws = null;
let reconnectInterval = null;

// REST API Base URL
const API_BASE = 'http://localhost:8080/api';
const WS_URL = 'ws://localhost:8080/ws';

// GPS Projection Settings
const RESTAURANT_LAT = -23.5615;
const RESTAURANT_LNG = -46.6620;
const RADAR_SCALE = 12000; // Multiplier to map lat/lng coordinates to canvas pixels

// DOM Elements
const gatewayStatusDot = document.getElementById('gateway-status-dot');
const gatewayStatusText = document.getElementById('gateway-status-text');
const btnMockFeed = document.getElementById('btn-mock-feed');
const btnReset = document.getElementById('btn-reset');
const selectSpeed = document.getElementById('simulation-speed');
const ingestForm = document.getElementById('ingest-form');
const ingestRawText = document.getElementById('ingest-raw-text');
const ingestPlatform = document.getElementById('ingest-platform');
const agentConsole = document.getElementById('agent-console');
const driversRoster = document.getElementById('drivers-roster');
const radarCanvas = document.getElementById('radar-canvas');
const xaiConsole = document.getElementById('xai-console');

// Stat Elements
const statActiveOrders = document.getElementById('stat-active-orders');
const statCompletedOrders = document.getElementById('stat-completed-orders');
const statDriversActive = document.getElementById('stat-drivers-active');
const statAiEfficiency = document.getElementById('stat-ai-efficiency');

// Initialize Dashboard
document.addEventListener('DOMContentLoaded', () => {
  setupEventListeners();
  initWebSocket();
  fetchInitialState();
  initRadarAnimation();
  startXAILogPolling();
});

// Event Listeners
function setupEventListeners() {
  btnMockFeed.addEventListener('click', async () => {
    try {
      const res = await fetch(`${API_BASE}/orders/mock-feed`, { method: 'POST' });
      const data = await res.json();
      console.log('Mock feed triggered:', data);
    } catch (e) {
      addSystemLog('Error triggering mock order feed: ' + e.message, 'warning');
    }
  });

  btnReset.addEventListener('click', async () => {
    try {
      const res = await fetch(`${API_BASE}/reset`, { method: 'POST' });
      const data = await res.json();
      addSystemLog('Reset server command issued.', 'info');
      orders = [];
      logs = [];
      renderOrders();
      renderLogs();
    } catch (e) {
      addSystemLog('Error resetting workspace state: ' + e.message, 'warning');
    }
  });

  selectSpeed.addEventListener('change', async (e) => {
    const multiplier = parseFloat(e.target.value);
    try {
      await fetch(`${API_BASE}/speed?value=${multiplier}`, { method: 'POST' });
      addSystemLog(`Simulation speed scaled to ${multiplier}x`, 'info');
    } catch (err) {
      console.error('Failed to change speed:', err);
    }
  });

  ingestForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const rawText = ingestRawText.value.trim();
    const platform = ingestPlatform.value;
    if (!rawText) return;

    try {
      const res = await fetch(`${API_BASE}/orders/ingest`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rawText, platform })
      });
      if (res.ok) {
        ingestRawText.value = '';
        addSystemLog(`Raw order submitted manually via ${platform}`, 'info');
      }
    } catch (err) {
      addSystemLog('Ingestion submission failed: ' + err.message, 'warning');
    }
  });
}

// Fetch Initial State
async function fetchInitialState() {
  try {
    const ordersRes = await fetch(`${API_BASE}/orders`);
    if (ordersRes.ok) orders = await ordersRes.json();

    const driversRes = await fetch(`${API_BASE}/drivers`);
    if (driversRes.ok) drivers = await driversRes.json();

    const logsRes = await fetch(`${API_BASE}/logs`);
    if (logsRes.ok) {
      const fetchedLogs = await logsRes.json();
      logs = fetchedLogs.reverse(); // Reverse descending order to chronological ascending
    }

    renderOrders();
    renderDrivers();
    renderLogs();
  } catch (err) {
    addSystemLog('Failed loading initial workspace state. Check if backend is running on port 8080.', 'warning');
  }
}

// WebSockets Gateway Connection
function initWebSocket() {
  ws = new WebSocket(WS_URL);

  ws.onopen = () => {
    gatewayStatusDot.classList.add('connected');
    gatewayStatusText.innerText = 'ONLINE';
    addSystemLog('Gateway Connection established with REST/WS backend.', 'success');
    if (reconnectInterval) {
      clearInterval(reconnectInterval);
      reconnectInterval = null;
    }
    fetchInitialState(); // Refresh state upon reconnect
  };

  ws.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data);
      handleWebSocketMessage(payload);
    } catch (e) {
      console.error('Failed parsing WebSocket JSON:', event.data, e);
    }
  };

  ws.onclose = () => {
    gatewayStatusDot.classList.remove('connected');
    gatewayStatusText.innerText = 'DISCONNECTED';
    if (!reconnectInterval) {
      addSystemLog('Gateway disconnected. Attempting reconnection...', 'warning');
      reconnectInterval = setInterval(initWebSocket, 3000);
    }
  };

  ws.onerror = (e) => {
    console.error('WebSocket Error:', e);
  };
}

// Handle Incoming Web Socket Message
function handleWebSocketMessage(payload) {
  const { type, data } = payload;

  switch (type) {
    case 'ORDER_UPDATE':
      upsertOrder(data);
      break;
    case 'DRIVER_UPDATE':
      upsertDriver(data);
      break;
    case 'AGENT_LOG':
      appendAgentLog(data);
      break;
    case 'SYSTEM':
      addSystemLog(data.message || data, 'info');
      break;
    default:
      console.warn('Unhandled message type:', type, data);
  }
}

// State Mutation Helpers
function upsertOrder(updatedOrder) {
  const index = orders.findIndex(o => o.id === updatedOrder.id);
  if (index !== -1) {
    orders[index] = updatedOrder;
  } else {
    orders.push(updatedOrder);
  }
  renderOrders();
}

function upsertDriver(updatedDriver) {
  const index = drivers.findIndex(d => d.id === updatedDriver.id);
  if (index !== -1) {
    drivers[index] = updatedDriver;
  } else {
    drivers.push(updatedDriver);
  }
  renderDrivers();
}

function appendAgentLog(log) {
  logs.push(log);
  renderLogs();
}

// Local Logger Fallback
function addSystemLog(message, level = 'info') {
  const log = {
    timestamp: Date.now(),
    agentName: 'System',
    level: level.toUpperCase(),
    message: message,
    orderId: null
  };
  appendAgentLog(log);
}

// Rendering Logic
function renderOrders() {
  const containers = {
    PENDING: document.getElementById('cards-pending'),
    TRIAGED: document.getElementById('cards-pending'), // Shares col 1
    COOKING: document.getElementById('cards-cooking'),
    READY: document.getElementById('cards-cooking'), // Shares col 2
    DELIVERING: document.getElementById('cards-delivering'),
    DELIVERED: document.getElementById('cards-delivered')
  };

  // Clear all lists
  Object.values(containers).forEach(el => {
    if (el) el.innerHTML = '';
  });

  let activeCount = 0;
  let completedCount = 0;

  orders.forEach(order => {
    const parent = containers[order.status];
    if (!parent) return;

    if (order.status !== 'DELIVERED') {
      activeCount++;
    } else {
      completedCount++;
    }

    const card = document.createElement('div');
    card.className = `order-card`;
    card.setAttribute('data-id', order.id);

    const platformClass = order.platform ? order.platform.toLowerCase().replace(' ', '-') : 'ifood';
    const priorityClass = order.priority ? order.priority.toLowerCase() : 'medium';
    
    // Build item details markup
    let itemsMarkup = '';
    if (order.items && order.items.length > 0) {
      itemsMarkup = `<div class="order-items-list">` + 
        order.items.map(it => `
          <div class="order-item-row">
            <span>${it.name}</span>
            <strong>x${it.quantity}</strong>
          </div>
        `).join('') + `</div>`;
    }

    // Build driver assignment markup
    let driverMarkup = '';
    if (order.driverId) {
      const driverObj = drivers.find(d => d.id === order.driverId);
      const driverName = driverObj ? driverObj.name : 'Rider';
      driverMarkup = `<div class="driver-tag">🛵 ${driverName}</div>`;
    }

    // Build specific progress indicator bar
    let progressMarkup = '';
    if (order.status === 'COOKING') {
      progressMarkup = `
        <div class="order-timer">🕒 Prep: ${order.prepTimeMinutes || 15}m</div>
        <div class="progress-bar-container">
          <div class="progress-bar cooking" style="width: 60%; animation: pulse-border 1s infinite;"></div>
        </div>
      `;
    } else if (order.status === 'DELIVERING') {
      progressMarkup = `
        <div class="order-timer">🛵 Transit GPS: Active</div>
        <div class="progress-bar-container">
          <div class="progress-bar delivering" style="width: 85%;"></div>
        </div>
      `;
    } else if (order.status === 'READY') {
      progressMarkup = `<div class="order-timer" style="color: var(--accent-green)">📦 Packed & Waiting Dispatch</div>`;
    } else if (order.status === 'PENDING') {
      progressMarkup = `<div class="order-timer" style="color: var(--accent-yellow)">🔍 Queued in AI Buffer</div>`;
    } else if (order.status === 'DELIVERED') {
      progressMarkup = `<div class="order-timer" style="color: var(--accent-green)">✅ Transit Completed</div>`;
    }

    // Agent Notes annotation
    const notesMarkup = order.agentNotes 
      ? `<div class="order-notes">AI: "${order.agentNotes}"</div>`
      : '';

    card.innerHTML = `
      <div class="order-card-header">
        <span class="order-id">${order.id || 'INGESTING'}</span>
        <span class="platform-badge ${platformClass}">${order.platform || 'INGEST'}</span>
      </div>
      <div class="order-customer">${order.customerName || 'Carregando Triage...'}</div>
      <div class="order-address">${order.address || 'Analisando endereço...'}</div>
      ${itemsMarkup}
      <div class="order-total-priority">
        <span class="order-price">R$ ${(order.totalPrice || 0).toFixed(2)}</span>
        <span class="priority-tag ${priorityClass}">${order.priority || 'MEDIUM'}</span>
      </div>
      ${notesMarkup}
      <div style="margin-top: 0.25rem; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.25rem;">
        ${progressMarkup}
        ${driverMarkup}
      </div>
    `;

    parent.appendChild(card);
  });

  // Update Stats counts
  statActiveOrders.innerText = activeCount;
  statCompletedOrders.innerText = completedCount;

  // Update counts on headers
  document.getElementById('count-pending').innerText = orders.filter(o => o.status === 'PENDING' || o.status === 'TRIAGED').length;
  document.getElementById('count-cooking').innerText = orders.filter(o => o.status === 'COOKING' || o.status === 'READY').length;
  document.getElementById('count-delivering').innerText = orders.filter(o => o.status === 'DELIVERING').length;
  document.getElementById('count-delivered').innerText = orders.filter(o => o.status === 'DELIVERED').length;
}

function renderDrivers() {
  driversRoster.innerHTML = '';
  let activeCount = 0;

  drivers.forEach(d => {
    if (d.status !== 'OFFLINE') activeCount++;

    const row = document.createElement('div');
    row.className = 'driver-row';
    const statusClass = d.status.toLowerCase();
    
    let cargoText = '';
    if (d.currentOrderId) {
      cargoText = `<span style="font-family: monospace; font-size: 0.75rem; color: var(--accent-yellow)"> Cargo: ${d.currentOrderId}</span>`;
    }

    row.innerHTML = `
      <div>
        <span class="driver-name">${d.name}</span>
        <span style="font-size:0.7rem; color:var(--text-muted)"> (${d.vehicleType})</span>
        ${cargoText}
      </div>
      <span class="driver-status ${statusClass}">${d.status}</span>
    `;
    driversRoster.appendChild(row);
  });

  statDriversActive.innerText = `${activeCount} / ${drivers.length}`;
}

function renderLogs() {
  agentConsole.innerHTML = '';
  
  // Cap displayed console logs to 80 to prevent DOM performance degradation
  const recentLogs = logs.slice(-80);
  
  recentLogs.forEach(log => {
    const entry = document.createElement('div');
    entry.className = 'log-entry';

    const timeStr = new Date(log.timestamp).toLocaleTimeString();
    const agentClass = log.agentName ? log.agentName.replace('Agent', '').toLowerCase() : 'system';
    
    let textClass = '';
    if (log.level === 'THINKING') textClass = 'thinking';
    else if (log.level === 'SUCCESS') textClass = 'success';
    else if (log.level === 'WARNING') textClass = 'warning';

    const orderIndicator = log.orderId 
      ? `<span style="color:#f59e0b; margin-right: 0.25rem;">[${log.orderId}]</span>`
      : '';

    entry.innerHTML = `
      <span class="log-timestamp">${timeStr}</span>
      <span class="log-agent ${agentClass}">${log.agentName}</span>
      ${orderIndicator}
      <span class="log-text ${textClass}">${log.message}</span>
    `;

    agentConsole.appendChild(entry);
  });

  // Auto scroll console window
  agentConsole.scrollTop = agentConsole.scrollHeight;

  // Calculate Average Triage Speed (simple simulation statistic)
  const triageLogs = logs.filter(l => l.agentName === 'TriageAgent' && l.level === 'SUCCESS');
  if (triageLogs.length > 0) {
    // LLM parsing usually takes 2.5s in our logs simulator
    statAiEfficiency.innerText = '1.8s';
  } else {
    statAiEfficiency.innerText = '0s';
  }
}

// GPS Air Traffic / Radar Animation Loop
function initRadarAnimation() {
  const ctx = radarCanvas.getContext('2d');
  
  // Resize canvas to fill element bounds
  function resizeCanvas() {
    const bounds = radarCanvas.parentElement.getBoundingClientRect();
    radarCanvas.width = bounds.width;
    radarCanvas.height = bounds.height;
  }
  
  window.addEventListener('resize', resizeCanvas);
  resizeCanvas();

  let sweepAngle = 0;

  function drawRadar() {
    if (!ctx) return;
    
    const width = radarCanvas.width;
    const height = radarCanvas.height;
    const cx = width / 2;
    const cy = height / 2;
    const maxRadius = Math.min(width, height) * 0.45;

    // Clear canvas with trace tail effect (alpha background clear)
    ctx.fillStyle = 'rgba(11, 11, 14, 0.15)';
    ctx.fillRect(0, 0, width, height);

    // Draw Grid Lines (concentric radar ranges)
    ctx.strokeStyle = 'rgba(0, 162, 255, 0.1)';
    ctx.lineWidth = 1.5;
    for (let r = maxRadius / 4; r <= maxRadius; r += maxRadius / 4) {
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.stroke();
    }

    // Crosshairs
    ctx.beginPath();
    ctx.moveTo(cx - maxRadius, cy);
    ctx.lineTo(cx + maxRadius, cy);
    ctx.moveTo(cx, cy - maxRadius);
    ctx.lineTo(cx, cy + maxRadius);
    ctx.stroke();

    // Radar Sweeper line
    sweepAngle += 0.02;
    ctx.strokeStyle = 'rgba(0, 162, 255, 0.2)';
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + Math.cos(sweepAngle) * maxRadius, cy + Math.sin(sweepAngle) * maxRadius);
    ctx.stroke();

    // Render HQ / Restaurant Center Point
    ctx.fillStyle = 'var(--accent-yellow)';
    ctx.beginPath();
    ctx.arc(cx, cy, 6, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#000';
    ctx.lineWidth = 1.5;
    ctx.stroke();

    // Center pulsating ring
    const hqPulse = (Date.now() % 1500) / 1500;
    ctx.strokeStyle = `rgba(255, 213, 0, ${1.0 - hqPulse})`;
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.arc(cx, cy, 6 + hqPulse * 16, 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = '#000000';
    ctx.font = 'bold 8px sans-serif';
    ctx.fillText('HQ', cx - 5, cy + 3);

    // Helper: Map GPS coordinates to canvas coordinates
    function project(lat, lng) {
      return {
        x: cx + (lng - RESTAURANT_LNG) * RADAR_SCALE,
        y: cy - (lat - RESTAURANT_LAT) * RADAR_SCALE // Inverted because GPS lat increases upwards, canvas Y goes downwards
      };
    }

    // Render active order destinations
    orders.forEach(order => {
      if (order.status === 'DELIVERING' || order.status === 'READY' || order.status === 'COOKING') {
        // Derive static pseudo-coordinates for order destination based on its ID so it matches the driver's path
        const idNum = parseInt((order.id || '').replace(/\D/g, '')) || 100;
        
        // Deterministic offset based on order id
        const offsetLat = ((idNum * 17) % 100 - 50) / 7500.0;
        const offsetLng = ((idNum * 23) % 100 - 50) / 7500.0;
        const destLat = RESTAURANT_LAT + offsetLat;
        const destLng = RESTAURANT_LNG + offsetLng;
        
        const pt = project(destLat, destLng);

        // Draw pulsing destination marker
        const destPulse = ((Date.now() + idNum * 250) % 2000) / 2000;
        ctx.strokeStyle = `rgba(255, 0, 127, ${1.0 - destPulse})`;
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 4 + destPulse * 18, 0, Math.PI * 2);
        ctx.stroke();

        ctx.fillStyle = 'var(--accent-pink)';
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 4, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 8px monospace';
        ctx.fillText(order.id, pt.x + 8, pt.y + 3);
      }
    });

    // Render active drivers
    drivers.forEach(d => {
      if (d.status === 'OFFLINE') return;

      const pt = project(d.latitude, d.longitude);

      // If active delivering, draw trail path from HQ
      if (d.status === 'DELIVERING' && d.currentOrderId) {
        // Draw dotted path from restaurant to current, and current to destination
        ctx.strokeStyle = 'rgba(0, 162, 255, 0.4)';
        ctx.lineWidth = 1.5;
        ctx.setLineDash([4, 4]);
        
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.lineTo(pt.x, pt.y);
        ctx.stroke();
        ctx.setLineDash([]); // Reset
      }

      // Draw Driver Node
      ctx.fillStyle = d.status === 'DELIVERING' ? 'var(--accent-blue)' : 'var(--accent-green)';
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, 5, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#000000';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      // Driver tag name
      ctx.fillStyle = '#a1a1aa';
      ctx.font = '9px sans-serif';
      const shortName = d.name.split(' ')[0];
      ctx.fillText(shortName, pt.x - 12, pt.y - 8);
    });

    requestAnimationFrame(drawRadar);
  }

  // Start animation loop
  requestAnimationFrame(drawRadar);
}

// Explainable AI (XAI) Polling & Rendering
function startXAILogPolling() {
  fetchXAILogs();
  setInterval(fetchXAILogs, 3000);
}

async function fetchXAILogs() {
  try {
    const res = await fetch(`${API_BASE}/logs`);
    if (res.ok) {
      const xaiLogs = await res.json();
      renderXAILogs(xaiLogs);
    }
  } catch (err) {
    console.error('Error fetching XAI logs:', err);
  }
}

function renderXAILogs(xaiLogs) {
  if (!xaiConsole) return;
  xaiConsole.innerHTML = '';
  
  if (!xaiLogs || xaiLogs.length === 0) {
    xaiConsole.innerHTML = '<div class="xai-log-entry" style="color: var(--text-muted)">Sem registros no log de auditoria.</div>';
    return;
  }

  // Reverse to show oldest at the top and newest at the bottom
  // Since endpoint is sorted by date desc, reversing it shows them chronologically.
  const chronologicalLogs = [...xaiLogs].reverse();

  chronologicalLogs.forEach(log => {
    const entry = document.createElement('div');
    entry.className = 'xai-log-entry';

    const timeStr = new Date(log.timestamp).toLocaleTimeString();
    const agent = log.agentName || 'System';
    const levelClass = log.level ? log.level.toLowerCase() : 'info';
    
    entry.innerHTML = `
      <span class="xai-log-time">[${timeStr}]</span>
      <span class="xai-log-agent">[${agent}]</span>
      <span class="xai-log-level ${levelClass}">[${log.level || 'INFO'}]</span>: 
      <span class="xai-log-msg">${log.message}</span>
    `;

    xaiConsole.appendChild(entry);
  });

  // Auto-scroll to bottom of the terminal
  xaiConsole.scrollTop = xaiConsole.scrollHeight;
}

