'use strict';

// ====================== 完整 main.js（纯私聊版） ======================

const $ = (selector) => document.querySelector(selector);

const usernamePage      = $('#username-page');
const chatPage          = $('#chat-page');
const usernameForm      = $('#usernameForm');
const messageForm       = $('#messageForm');
const messageInput      = $('#message');
const messageArea       = $('#messageArea');
const connectingElement = $('.connecting');
const userList          = $('#userList');          // 新增：在线用户列表
const chatHeaderTitle   = $('.chat-header h2');    // 聊天窗口标题

// 头像颜色
const colors = ['#2196F3', '#32c787', '#00BCD4', '#ff5652', '#ffc107', '#ff85af', '#FF9800', '#39bbb0'];
const getAvatarColor = (name = '') => {
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = 31 * hash + name.charCodeAt(i);
    return colors[Math.abs(hash % colors.length)];
};

const ChatApp = {
    username: null,
    client: null,
    token: null,
    currentTarget: null,   // 当前正在聊天的对象（用户名）

    // ==================== Cookie 工具 ====================
    getCookie(name) {
        const cookies = document.cookie.split('; ');
        for (let cookie of cookies) {
            const [key, value] = cookie.split('=');
            if (key === name) return decodeURIComponent(value);
        }
        return null;
    },

    // ==================== 连接 WebSocket ====================
    connect(event) {
        event?.preventDefault();

        this.username = $('#name').value.trim();
        if (!this.username) return alert('请输入用户名');

        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        const token = this.getCookie('authToken');
        if (!token) return alert('未找到登录凭证，请重新登录');

        const wsUrl = `ws://localhost:8080/ws?token=${encodeURIComponent(token)}`;

        this.client = new StompJs.Client({
            brokerURL: wsUrl,
            heartbeatIncoming: 0,
            heartbeatOutgoing: 20000,
            reconnectDelay: 5000,
            debug: str => console.log('[STOMP]', str),

            connectHeaders: {
                Token: token
            },

            onConnect: (frame) => this.onConnect(frame),
            onStompError: (frame) => console.error('STOMP Error', frame),
            onWebSocketClose: () => connectingElement.textContent = '连接断开，正在重连...',
        });

        this.client.activate();
    },

    // ==================== 连接成功后执行 ====================
    onConnect(frame) {
        console.log('WebSocket 已连接', frame);
        connectingElement.textContent = '已连接';

        // 1. 订阅私人消息（所有别人发给我的、私聊消息）
        this.client.subscribe('/user/queue/messages', message => {
            this.onMessageReceived(message);
        });

        // 2. 订阅历史消息返回
        this.client.subscribe('/user/queue/history', message => {
            const history = JSON.parse(message.body);
            messageArea.innerHTML = '';
            // 数据库倒序查的，要正序显示
            history.reverse().forEach(msg => {
                this.onMessageReceived({ body: JSON.stringify(msg) });
            });
        });

        // 3. 加载在线用户列表（你后端要提供 /api/online-users 接口）
        this.loadOnlineUsers();

        // 【建议】做一个轮询，每 5 秒刷新一次用户列表，防止有人掉线了你不知道
        if (this.refreshInterval) clearInterval(this.refreshInterval);
        this.refreshInterval = setInterval(() => this.loadOnlineUsers(), 2000);
    },

    // ==================== 加载在线用户列表 ====================
    loadOnlineUsers() {
        fetch('/api/online-users', { credentials: 'include' })
            .then(r => r.json())
            .then(users => {
                userList.innerHTML = '';
                users.forEach(u => {
                    if (u === this.username) return;

                    const li = document.createElement('li');
                    li.textContent = u;
                    li.style.padding = '12px 15px';
                    li.style.cursor = 'pointer';
                    li.style.borderBottom = '1px solid #eee';
                    li.onclick = () => this.openPrivateChat(u);
                    userList.appendChild(li);
                });
            })
            .catch(() => {
                userList.innerHTML = '<li style="padding:15px;color:#999;">加载用户列表失败</li>';
            });
    },

    // ==================== 打开私聊窗口 ====================
    openPrivateChat(targetUsername) {
        if (targetUsername === this.username) {
            alert('不能和自己聊天哦~');
            return;
        }

        this.currentTarget = targetUsername;
        chatHeaderTitle.textContent = `与 ${targetUsername} 的聊天`;
        messageArea.innerHTML = '<li class="event-message"><p>正在加载历史消息...</p></li>';

        // 请求历史消息
        this.client.publish({
            destination: '/app/chat.history',
            body: JSON.stringify({
                target: targetUsername,
                Chattype: 'PRIVATE'
            })
        });
    },

    // ==================== 发送消息 ====================
    sendMessage(e) {
        e.preventDefault();
        const content = messageInput.value.trim();
        if (!content) return;
        if (!this.currentTarget) {
            alert('请先选择一个聊天对象');
            return;
        }

        const msg = {
            // sender 故意不传！由后端强制写入，防止伪造
            content: content,
            target: this.currentTarget,
            chat_type: 'PRIVATE'
        };

        this.client.publish({
            destination: '/app/chat.send',
            body: JSON.stringify(msg)
        });
        // 本地立即显示自己发的消息
        // this.displayLocalMessage(content);
        messageInput.value = '';
    },

    // ==================== 显示自己发的消息（本地即时显示） ====================
    displayLocalMessage(content) {
        const li = document.createElement('li');
        li.className = 'chat-message my-message';

        const p = document.createElement('p');
        p.textContent = content;
        li.appendChild(p);
        messageArea.appendChild(li);
        messageArea.scrollTop = messageArea.scrollHeight;
    },

    // ==================== 接收消息并显示 ====================
    onMessageReceived(message) {
        const data = JSON.parse(message.body);

        const li = document.createElement('li');
        const p = document.createElement('p');
        p.textContent = data.content;

        if (data.sender === this.username) {
            // 自己发的消息（右边对齐）
            li.className = 'chat-message my-message';
        } else {
            // 别人发的消息（左边对齐）
            li.className = 'chat-message';

            const avatar = document.createElement('i');
            avatar.textContent = data.sender.charAt(0).toUpperCase();
            avatar.style.backgroundColor = getAvatarColor(data.sender);
            li.appendChild(avatar);

            const nameSpan = document.createElement('span');
            nameSpan.textContent = data.sender;
            nameSpan.style.fontWeight = '600';
            li.appendChild(nameSpan);
        }

        li.appendChild(p);
        messageArea.appendChild(li);
        messageArea.scrollTop = messageArea.scrollHeight;
    },

    // ==================== 断开连接 ====================
    disconnect() {
        if (this.refreshInterval) clearInterval(this.refreshInterval);
        if (this.client) this.client.deactivate();
    }
};
async function register(event) {
    event?.preventDefault();

    const username = document.getElementById("name").value.trim();
    const password = document.getElementById("password").value;

    if (!username || !password) {
        return alert("用户名和密码不能为空");
    }

    const response = await fetch("/Registry", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
        credentials: "include",
        mode: "cors"
    });

    const text = await response.text();

    if (response.ok) {
        alert("注册成功！请登录");
    } else {
        alert(text || "注册失败（可能用户已存在）");
    }
}

async function login(event) {
    event?.preventDefault();
    const username = document.getElementById("name").value.trim();
    const password = document.getElementById("password").value;

    if (!username || !password) return alert("请输入用户名和密码");

    const response = await fetch("/Login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
        credentials: "include" // 必须让浏览器处理 Cookie
    });


    const result=await response.json();
    if(result.code!==200){
        return alert(result.msg||"登陆失败")
    }
    // 修改处：后端返回的是 JSON 或者是空，Token 实际上是在 Cookie 里的
    // 不需要 const token = await response.text();

    alert("登陆成功！");

    // ChatApp 会自己去读 Cookie 里的 authToken
    // 只要你的 cookie 名字对得上 (getCookie('authToken'))

    ChatApp.username = username;
    usernamePage.classList.add('hidden');
    chatPage.classList.remove('hidden');

    ChatApp.connect();
    setTimeout(() => ChatApp.loadOnlineUsers(), 1500);
}

// ====================== 事件绑定 ======================
// document.getElementById('submission').addEventListener('click', e => ChatApp.connect(e));
messageForm.addEventListener('submit', e => ChatApp.sendMessage(e));

// 登录、注册、测试按钮保持不变（你原来的代码）
document.getElementById('rbutton').addEventListener('click', register, true);
document.getElementById('login').addEventListener('click', login, true);
document.getElementById('testToken').addEventListener('click', async () => {
    // ...你原来的 testToken 代码
});

// 页面关闭时断开
window.addEventListener('beforeunload', () => ChatApp.disconnect());