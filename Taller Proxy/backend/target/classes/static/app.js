const userId = 'default-user';
const apiHeaders = {
  'Content-Type': 'application/json',
  'X-User-Id': userId
};

const promptInput = document.getElementById('promptInput');
const sendButton = document.getElementById('sendButton');
const messagesContainer = document.getElementById('chatMessages');
const planBadge = document.getElementById('planBadge');
const quotaText = document.getElementById('quotaText');
const quotaBar = document.getElementById('quotaBar');
const rateText = document.getElementById('rateText');
const rateCountdown = document.getElementById('rateCountdown');
const tokenEstimate = document.getElementById('tokenEstimate');
const charCount = document.getElementById('charCount');
const clearButton = document.getElementById('clearBtn');
const usageChart = document.getElementById('historyChart');
const upgradeModal = document.getElementById('upgradeModal');
const upgradeButton = document.getElementById('upgradeButton');
const closeModal = document.getElementById('closeModal');
const newChatBtn = document.getElementById('newChatBtn');

let quotaData = {
  plan: 'FREE',
  monthlyQuota: 1000,
  monthlyUsedTokens: 0,
  rateLimitRemaining: 5,
  rateLimitResetSeconds: 60,
  dailyUsage: [0,0,0,0,0,0,0]
};
let upgradePlan = 'FREE';

// Event listeners
promptInput.addEventListener('input', () => {
  if (tokenEstimate) {
    tokenEstimate.textContent = `${estimateTokens(promptInput.value)} tokens`;
  }
  charCount.textContent = `${promptInput.value.length} / 2000`;
  adjustTextareaHeight();
});

promptInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendPrompt();
  }
});

sendButton.addEventListener('click', sendPrompt);
closeModal.addEventListener('click', () => upgradeModal.classList.add('hidden'));
upgradeButton.addEventListener('click', simulateUpgrade);
newChatBtn.addEventListener('click', startNewChat);
clearButton.addEventListener('click', () => {
  promptInput.value = '';
  if (tokenEstimate) {
    tokenEstimate.textContent = '0 tokens';
  }
  charCount.textContent = '0 / 2000';
  adjustTextareaHeight();
});

// Initialize app
window.addEventListener('load', () => {
  refreshUsage();
  setInterval(refreshUsage, 5000);
  setInterval(updateCountdown, 1000);
  showWelcomeMessage();
});

function adjustTextareaHeight() {
  promptInput.style.height = 'auto';
  promptInput.style.height = Math.min(promptInput.scrollHeight, 120) + 'px';
}

async function refreshUsage() {
  try {
    const response = await fetch('/api/usage', { headers: apiHeaders });
    const data = await response.json();
    quotaData = data;
    updateUiFromQuota(data);
  } catch (error) {
    console.error('Error al consultar uso:', error);
    sendButton.disabled = false;
  }
}

function updateUiFromQuota(data) {
  const remaining = Math.max(0, data.monthlyQuota - data.monthlyUsedTokens);
  const used = data.monthlyUsedTokens;
  const percent = Math.min(100, (used / data.monthlyQuota) * 100);

  // Update quota display
  quotaText.textContent = `${used.toLocaleString()} / ${data.monthlyQuota.toLocaleString()}`;
  quotaBar.style.width = `${percent}%`;

  // Update rate limit display
  rateText.textContent = `${data.rateLimitRemaining} requests/min`;
  rateCountdown.textContent = `Reset in ${data.rateLimitResetSeconds}s`;

  // Update plan badge
  planBadge.textContent = data.plan;
  planBadge.className = `badge ${data.plan.toLowerCase()}`;

  // Update plan label in header
  const planLabel = document.getElementById('planLabel');
  if (planLabel) {
    planLabel.textContent = data.plan;
  }

  // Render usage chart
  renderUsageChart(data.dailyUsage || []);

  // Enable/disable send button
  sendButton.disabled = data.rateLimitRemaining <= 0 || remaining <= 0;
}

function renderUsageChart(values) {
  usageChart.innerHTML = '';
  const max = Math.max(...values, 1);
  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  values.slice(-7).forEach((value, index) => {
    const bar = document.createElement('div');
    bar.className = 'bar';
    bar.style.height = `${Math.max(24, (value / max) * 150)}px`;
    bar.innerHTML = `<span>${value}</span>`;
    bar.title = `${days[index]}: ${value} tokens`;
    usageChart.appendChild(bar);
  });
}

function estimateTokens(prompt) {
  if (!prompt || !prompt.trim()) return 0;
  return Math.max(1, Math.min(200, Math.floor(prompt.trim().length / 4)));
}

async function sendPrompt() {
  const prompt = promptInput.value.trim();
  if (!prompt) return;

  appendMessage('user', prompt);
  sendButton.disabled = true;
  promptInput.value = '';
  if (tokenEstimate) {
    tokenEstimate.textContent = '0 tokens';
  }
  adjustTextareaHeight();

  showTypingIndicator();

  try {
    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: apiHeaders,
      body: JSON.stringify({ prompt })
    });
    const data = await response.json();
    hideTypingIndicator();

    if (!response.ok) {
      if (response.status === 429 || data.blocked) {
        showUpgradeModal();
      }
      appendMessage('bot', data.message || 'Error en la petición.');
    } else {
      appendMessage('bot', data.message);
      refreshUsage();
    }
  } catch (error) {
    hideTypingIndicator();
    appendMessage('bot', 'Error de conexión con el backend.');
    console.error(error);
  } finally {
    sendButton.disabled = false;
  }
}

function appendMessage(type, text) {
  const messageDiv = document.createElement('div');
  messageDiv.className = `message ${type}`;

  const avatarDiv = document.createElement('div');
  avatarDiv.className = 'assistant-avatar';
  avatarDiv.innerHTML = `<div class="avatar-icon">${type === 'user' ? 'U' : 'AI'}</div>`;

  const contentDiv = document.createElement('div');
  contentDiv.className = 'message-content';

  const bubbleDiv = document.createElement('div');
  bubbleDiv.className = 'message-bubble';
  bubbleDiv.textContent = text;

  const metaDiv = document.createElement('div');
  metaDiv.className = 'message-meta';
  metaDiv.innerHTML = `
    <span>${type === 'user' ? 'Tú' : 'Proxy IA'}</span>
    <span>•</span>
    <span>${new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })}</span>
  `;

  contentDiv.appendChild(bubbleDiv);
  contentDiv.appendChild(metaDiv);

  messageDiv.appendChild(avatarDiv);
  messageDiv.appendChild(contentDiv);

  messagesContainer.appendChild(messageDiv);
  scrollToBottom();
}

function showWelcomeMessage() {
  const welcomeDiv = document.createElement('div');
  welcomeDiv.className = 'welcome-message';

  welcomeDiv.innerHTML = `
    <div class="assistant-avatar">
      <div class="avatar-icon">AI</div>
    </div>
    <div class="message-content">
      <div class="message-bubble welcome">
        <h3>¡Bienvenido a Proxy IA!</h3>
        <p>Soy tu asistente de IA inteligente. Puedo ayudarte con:</p>
        <ul>
          <li>Respuestas a preguntas y consultas</li>
          <li>Generación de ideas y contenido</li>
          <li>Análisis y explicación de conceptos</li>
          <li>Asistencia en tareas creativas</li>
        </ul>
        <p>¿En qué puedo ayudarte hoy?</p>
      </div>
    </div>
  `;

  messagesContainer.appendChild(welcomeDiv);
  scrollToBottom();
}

function showTypingIndicator() {
  const typingDiv = document.createElement('div');
  typingDiv.id = 'typingIndicator';
  typingDiv.className = 'message bot';

  typingDiv.innerHTML = `
    <div class="assistant-avatar">
      <div class="avatar-icon">AI</div>
    </div>
    <div class="message-content">
      <div class="typing-indicator">
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
      </div>
    </div>
  `;

  messagesContainer.appendChild(typingDiv);
  scrollToBottom();
}

function hideTypingIndicator() {
  const typing = document.getElementById('typingIndicator');
  if (typing) typing.remove();
}

function scrollToBottom() {
  messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

function startNewChat() {
  // Clear messages except welcome
  const messages = messagesContainer.querySelectorAll('.message:not(.welcome-message)');
  messages.forEach(msg => msg.remove());

  // Reset input
  promptInput.value = '';
  tokenEstimate.textContent = '0';
  adjustTextareaHeight();

  // Show welcome message
  showWelcomeMessage();
}

function updateCountdown() {
  if (!quotaData) return;
  if (quotaData.rateLimitResetSeconds > 0) {
    quotaData.rateLimitResetSeconds -= 1;
    rateCountdown.textContent = `Reset in ${quotaData.rateLimitResetSeconds}s`;
  }
}

function showUpgradeModal() {
  upgradeModal.classList.remove('hidden');
}

function simulateUpgrade() {
  upgradeModal.classList.add('hidden');
  if (upgradePlan === 'FREE') {
    upgradePlan = 'PRO';
    planBadge.textContent = 'PRO';
    planBadge.className = 'badge pro';
    quotaData.plan = 'PRO';
    quotaData.monthlyQuota = 5000;
    quotaData.rateLimitRemaining = 20;
    quotaData.monthlyUsedTokens = 0;
    quotaText.textContent = `${quotaData.monthlyUsedTokens} / ${quotaData.monthlyQuota}`;
    quotaBar.style.width = '0%';
    rateText.textContent = '20 requests/min';
    sendButton.disabled = false;
  }
}
