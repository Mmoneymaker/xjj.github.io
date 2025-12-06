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

// ==================== 用户状态管理器（新增，只改这部分）====================
class UserStatusManager {
    constructor(client, currentUsername) {
        this.client = client;
        this.currentUsername = currentUsername;
        this.onlineUsers = new Set(); // 直接用Set，自动去重
        this.onListUpdated = null;
    }

    // 初始化订阅
    initialize() {
        // 只订阅一个频道：在线状态
        this.subscription = this.client.subscribe(
            '/topic/online-status',
            (message) => this.handleStatusMessage(message)
        );

        console.log('用户状态管理器已初始化，等待在线列表...');
        return this;
    }

    // 处理状态消息
    handleStatusMessage(message) {
        try {
            const data = JSON.parse(message.body);

            if (data.type === 'ONLINE_LIST') {
                this.processOnlineList(data.users);
            }
        } catch (e) {
            console.error('解析状态消息失败:', e, '原始数据:', message.body);
        }
    }

    // 处理在线列表
    processOnlineList(users) {
        // 清空当前集合
        this.onlineUsers.clear();

        // 添加所有用户
        users.forEach(user => {
            if (user.username) {
                this.onlineUsers.add(user.username);
            }
        });

        console.log('收到在线列表，共', this.onlineUsers.size, '人在线');

        // 触发UI更新
        if (this.onListUpdated) {
            this.onListUpdated(this.getOnlineList());
        }
    }

    // 获取在线列表（排序后）
    getOnlineList() {
        return Array.from(this.onlineUsers)
            .sort((a, b) => {
                // 自己排在最前面
                if (a === this.currentUsername) return -1;
                if (b === this.currentUsername) return 1;
                // 其他人按字母排序
                return a.localeCompare(b);
            });
    }

    // 清理
    destroy() {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
        this.onlineUsers.clear();
        console.log('用户状态管理器已销毁');
    }
}

const ChatApp = {
    username: null,
    client: null,
    token: null,
    currentTarget: null,   // 当前正在聊天的对象（用户名）
    statusManager: null,   // 新增：状态管理器
    refreshInterval: null, // 保留但不再用于轮询用户列表

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
    // 保持你的原有代码完全不变
    connect(event) {
        event?.preventDefault();

        this.username = $('#name').value.trim();
        if (!this.username) return alert('请输入用户名');

        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        const token = this.getCookie('authToken');
        if (!token) return alert('未找到登录凭证，请重新登录');

        // 保持你的url不变
        const wsUrl = `ws://localhost:8080/ws?token=${encodeURIComponent(token)}`;

        this.client = new StompJs.Client({
            brokerURL: wsUrl,
            heartbeatIncoming: 0,
            heartbeatOutgoing: 20000,
            reconnectDelay: 5000,
            debug: str => console.log('[STOMP]', str),

            // 保持你的connectHeaders不变
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
    // 只在这里添加状态管理器初始化，其他保持原有
    onConnect(frame) {
        console.log('WebSocket 已连接', frame);
        connectingElement.textContent = '已连接';

        // 1. 【新增】初始化状态管理器（替换轮询）
        this.initializeStatusManager();

        // 2. 订阅私人消息（所有别人发给我的、私聊消息）- 保持原有
        this.client.subscribe('/user/queue/messages', message => {
            this.onMessageReceived(message);
        });

        // 3. 订阅历史消息返回 - 保持原有
        this.client.subscribe('/user/queue/history', message => {
            const history = JSON.parse(message.body);
            messageArea.innerHTML = '';
            // 数据库倒序查的，要正序显示
            history.reverse().forEach(msg => {
                this.onMessageReceived({ body: JSON.stringify(msg) });
            });
        });

        // 4. 【移除】原来的轮询代码，用事件驱动替代
        // if (this.refreshInterval) clearInterval(this.refreshInterval);
        // this.refreshInterval = setInterval(() => this.loadOnlineUsers(), 2000);

        console.log('所有订阅已完成');
    },

    // ==================== 初始化状态管理器（新增）====================
    initializeStatusManager() {
        // 清理旧的
        if (this.statusManager) {
            this.statusManager.destroy();
        }

        // 创建新的
        this.statusManager = new UserStatusManager(this.client, this.username);

        // 注册回调
        this.statusManager.onListUpdated = (onlineList) => {
            this.renderUserList(onlineList);
        };

        // 启动
        this.statusManager.initialize();

        // 更新在线用户标题
        this.updateOnlineCount(0);
    },

    // ==================== 更新在线人数显示 ====================
    updateOnlineCount(count) {
        const title = $('#onlineUsersTitle');
        if (title) {
            title.textContent = `在线用户 (${count})`;
        }
    },

    // ==================== 渲染完整用户列表 ====================
    renderUserList(onlineList) {
        // 清空列表
        userList.innerHTML = '';

        if (onlineList.length === 0) {
            const emptyLi = document.createElement('li');
            emptyLi.textContent = '暂无在线用户';
            emptyLi.style.cssText = 'padding:15px;color:#999;font-style:italic;text-align:center;';
            userList.appendChild(emptyLi);
            this.updateOnlineCount(0);
            return;
        }

        onlineList.forEach(username => {
            const li = this.createUserListItem(username);
            userList.appendChild(li);
        });

        // 更新在线人数显示
        this.updateOnlineCount(onlineList.length);
    },

    // ==================== 创建用户列表项 ====================
    createUserListItem(username) {
        const li = document.createElement('li');
        li.dataset.username = username;

        const isCurrentUser = username === this.username;

        li.innerHTML = `
            <div style="display: flex; align-items: center; justify-content: space-between; padding: 10px 15px;">
                <div style="display: flex; align-items: center; gap: 10px; flex: 1;">
                    <div class="status-indicator ${isCurrentUser ? 'me' : 'online'}"></div>
                    <span style="flex: 1; ${isCurrentUser ? 'color: #1890ff; font-weight: bold;' : ''}">
                        ${username}${isCurrentUser ? ' (我)' : ''}
                    </span>
                </div>
                ${!isCurrentUser ? '<div class="unread-badge" style="display: none;">0</div>' : ''}
            </div>
        `;

        li.style.cssText = `
            cursor: ${isCurrentUser ? 'default' : 'pointer'};
            border-bottom: 1px solid #f0f0f0;
            background: ${isCurrentUser ? '#f0f8ff' : 'transparent'};
            transition: all 0.2s;
        `;

        // 只有不是自己时才有点击事件
        if (!isCurrentUser) {
            li.onmouseenter = () => li.style.background = '#f5f5f5';
            li.onmouseleave = () => li.style.background = '';
            li.onclick = () => this.openPrivateChat(username);
        }

        return li;
    },

    // ==================== 打开私聊窗口（保持原有）====================
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

    // ==================== 发送消息（保持原有）====================
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

    // ==================== 显示自己发的消息（本地即时显示）====================
    displayLocalMessage(content) {
        const li = document.createElement('li');
        li.className = 'chat-message my-message';

        const p = document.createElement('p');
        p.textContent = content;
        li.appendChild(p);
        messageArea.appendChild(li);
        messageArea.scrollTop = messageArea.scrollHeight;
    },

    // ==================== 接收消息并显示（保持原有）====================
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
        if (this.statusManager) this.statusManager.destroy(); // 新增
        if (this.client) this.client.deactivate();
    }
};

// ==================== 保持你的注册/登录函数完全不变 ====================
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
    // setTimeout(() => ChatApp.loadOnlineUsers(), 1500);
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