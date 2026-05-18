
(function () {
    'use strict';

    // ──── Cấu hình ────────────────────────────────────────
    const STORAGE_KEY = 'sunilies_chat_history';
    const MAX_HISTORY = 10;   // gửi tối đa 10 lượt gần nhất cho AI để giữ ngắn

    // ──── DOM elements ────────────────────────────────────
    const toggle      = document.getElementById('sun-chat-toggle');
    const panel       = document.getElementById('sun-chat-panel');
    const closeBtn    = document.getElementById('sun-chat-close');
    const body        = document.getElementById('sun-chat-body');
    const form        = document.getElementById('sun-chat-form');
    const input       = document.getElementById('sun-chat-input');
    const sendBtn     = document.getElementById('sun-chat-send');
    const suggestions = document.getElementById('sun-chat-suggestions');
    const badge       = document.getElementById('sun-chat-badge');

    if (!toggle || !panel) return; // fragment chưa được include

    // ──── State ────────────────────────────────────────────
    let isOpen      = false;
    let isLoading   = false;
    let hasGreeted  = false;
    /** history dạng [{role:'user'|'model', text:'...'}] */
    let history     = loadHistory();

    // ──── History persistence (sessionStorage) ────────────
    function loadHistory() {
        try {
            const raw = sessionStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            return [];
        }
    }
    function saveHistory() {
        try {
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify(history));
        } catch (e) { /* sessionStorage full hoặc disable */ }
    }

    // ──── Render tin nhắn ────────────────────────────────
    function renderMessage(text, role, source) {
        // Loại bỏ typing indicator nếu có
        removeTyping();

        const msg = document.createElement('div');
        msg.className = 'sun-chat-msg ' + (role === 'user' ? 'sun-chat-msg--user' : 'sun-chat-msg--bot');

        // Chuyển links thành <a>, escape HTML khác để tránh XSS
        msg.innerHTML = formatText(text);
        body.appendChild(msg);

        // Source badge (chỉ cho bot, dev mode)
        // Comment dòng dưới nếu không muốn hiện badge
        // if (role === 'bot' && source) addSourceBadge(source);

        scrollToBottom();
    }

    function formatText(text) {
        // Escape HTML
        const escaped = text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');

        // Tự động convert URL & đường dẫn /products/... thành <a>
        return escaped.replace(
            /(https?:\/\/[^\s]+|\/products\/[a-z0-9-]+|\/collections\/[a-z0-9-]+|sunilies\.vn|facebook\.com\/sunilies)/gi,
            (m) => {
                const href = m.startsWith('http') || m.startsWith('/') ? m
                           : 'https://' + m;
                return `<a href="${href}" target="_blank" rel="noopener">${m}</a>`;
            }
        );
    }

    function showTyping() {
        removeTyping();
        const t = document.createElement('div');
        t.className = 'sun-chat-typing';
        t.id = 'sun-chat-typing';
        t.innerHTML = '<span></span><span></span><span></span>';
        body.appendChild(t);
        scrollToBottom();
    }
    function removeTyping() {
        const t = document.getElementById('sun-chat-typing');
        if (t) t.remove();
    }
    function scrollToBottom() {
        requestAnimationFrame(() => { body.scrollTop = body.scrollHeight; });
    }

    function renderSuggestions(items) {
        suggestions.innerHTML = '';
        if (!items || !items.length) return;
        items.forEach(text => {
            const b = document.createElement('button');
            b.type = 'button';
            b.className = 'sun-chat-suggestion';
            b.textContent = text;
            b.addEventListener('click', () => {
                sendMessage(text);
                suggestions.innerHTML = '';
            });
            suggestions.appendChild(b);
        });
    }

    // ──── API calls ───────────────────────────────────────
    async function fetchWelcome() {
        try {
            const res = await fetch('/api/chat/welcome');
            const data = await res.json();
            renderMessage(data.reply, 'bot');
            renderSuggestions(data.suggestions || []);
        } catch (e) {
            renderMessage('Xin chào! Em là trợ lý của SUNILIES. Bạn cần em hỗ trợ gì ạ? 🌾', 'bot');
        }
    }

    async function sendMessage(message) {
        if (!message || !message.trim() || isLoading) return;
        message = message.trim();

        // 1. Hiện tin nhắn user
        renderMessage(message, 'user');
        history.push({ role: 'user', text: message });
        saveHistory();

        // 2. Hiện typing
        isLoading = true;
        sendBtn.disabled = true;
        showTyping();

        try {
            // Chỉ gửi MAX_HISTORY lượt gần nhất (trừ tin nhắn hiện tại) để tiết kiệm token
            const historyForApi = history.slice(0, -1).slice(-MAX_HISTORY);

            const res = await fetch('/api/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    message: message,
                    history: historyForApi
                })
            });

            const data = await res.json();
            const reply = data.reply || 'Xin lỗi, em chưa nhận được câu trả lời.';

            renderMessage(reply, 'bot', data.source);
            history.push({ role: 'model', text: reply });
            saveHistory();

        } catch (e) {
            console.error('Chat error:', e);
            renderMessage('Xin lỗi, có sự cố kết nối. Bạn thử lại sau nhé!', 'bot');
        } finally {
            isLoading = false;
            sendBtn.disabled = false;
            input.focus();
        }
    }

    // ──── Toggle panel ───────────────────────────────────
    function openPanel() {
        panel.classList.add('is-open');
        toggle.classList.add('is-open');
        badge.classList.remove('is-show');
        isOpen = true;

        // Lần đầu mở → load greeting
        if (!hasGreeted) {
            hasGreeted = true;
            if (history.length === 0) {
                fetchWelcome();
            } else {
                // Restore history từ session
                history.forEach(h => {
                    renderMessage(h.text, h.role === 'user' ? 'user' : 'bot');
                });
            }
        }
        setTimeout(() => input.focus(), 300);
    }
    function closePanel() {
        panel.classList.remove('is-open');
        toggle.classList.remove('is-open');
        isOpen = false;
    }

    // ──── Event listeners ────────────────────────────────
    toggle.addEventListener('click', () => isOpen ? closePanel() : openPanel());
    closeBtn.addEventListener('click', closePanel);

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const msg = input.value;
        input.value = '';
        sendMessage(msg);
    });

    // Hiện badge "1" để gây chú ý sau 3s
    setTimeout(() => {
        if (!isOpen && history.length === 0) {
            badge.classList.add('is-show');
        }
    }, 3000);

    // Đóng panel khi nhấn ESC
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && isOpen) closePanel();
    });

})();
