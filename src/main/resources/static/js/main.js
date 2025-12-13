'use strict';

// ====================== 完整 main.js（纯私聊版） ======================

const $ = (selector) => document.querySelector(selector);

// Cookie 操作函数
function getCookie(name) {
    // 获取当前用户名（从全局变量或当前输入框）
    const currentUser = window.ChatApp?.username || document.getElementById("name")?.value.trim();

    if (currentUser) {
        // 1. 优先查找当前用户的特定Cookie（格式：authToken_用户名）
        const userCookieName = 'authToken_' + currentUser;
        const ca = document.cookie.split(';');
        for(let i = 0; i < ca.length; i++) {
            let c = ca[i];
            while (c.charAt(0) === ' ') {
                c = c.substring(1, c.length);
            }
            if (c.startsWith(userCookieName + '=')) {
                console.log('找到用户特定Cookie:', userCookieName);
                return c.substring(c.indexOf('=') + 1);
            }
        }
    }

    // 2. 回退到通用Cookie获取
    const nameEQ = name + "=";
    const ca = document.cookie.split(';');
    for(let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) === ' ') {
            c = c.substring(1, c.length);
        }
        if (c.indexOf(nameEQ) === 0) {
            console.log('找到通用Cookie');
            return c.substring(nameEQ.length, c.length);
        }
    }
    console.log('未找到任何Cookie，当前用户:', currentUser);
    return null;
}

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

        const token = getCookie('authToken');
        if (!token) return alert('未找到登录凭证，请重新登录');

        // 🎯 动态获取当前页面的主机和端口
        const wsUrl = `ws://${window.location.host}/ws?token=${encodeURIComponent(token)}`;

        this.client = new StompJs.Client({
            brokerURL: wsUrl,
            heartbeatIncoming: 0,
            heartbeatOutgoing: 20000,//发送帧
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

    const response = await fetch("/user/Registry", {
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

    const response = await fetch("/user/Login", {
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

// ====================== 群聊相关事件 ======================
// 选项卡切换
document.getElementById('privateChatTab').addEventListener('click', () => {
    document.getElementById('privateChatTab').classList.add('active');
    document.getElementById('groupChatTab').classList.remove('active');
    document.getElementById('privateChatTab').style.borderBottom = '2px solid #128ff2';
    document.getElementById('privateChatTab').style.color = '#128ff2';
    document.getElementById('groupChatTab').style.borderBottom = 'none';
    document.getElementById('groupChatTab').style.color = '#333';
    document.getElementById('privateChatList').style.display = 'block';
    document.getElementById('groupChatList').style.display = 'none';
});

document.getElementById('groupChatTab').addEventListener('click', () => {
    document.getElementById('groupChatTab').classList.add('active');
    document.getElementById('privateChatTab').classList.remove('active');
    document.getElementById('groupChatTab').style.borderBottom = '2px solid #128ff2';
    document.getElementById('groupChatTab').style.color = '#128ff2';
    document.getElementById('privateChatTab').style.borderBottom = 'none';
    document.getElementById('privateChatTab').style.color = '#333';
    document.getElementById('groupChatList').style.display = 'block';
    document.getElementById('privateChatList').style.display = 'none';
    // 加载群聊列表
    ChatApp.loadGroups();
});

// 创建群聊按钮
document.getElementById('createGroupBtn').addEventListener('click', () => {
    ChatApp.showCreateGroupModal();
});

// 取消创建群聊
document.getElementById('cancelCreateGroup').addEventListener('click', () => {
    ChatApp.hideCreateGroupModal();
});

// 创建群聊表单
document.getElementById('createGroupForm').addEventListener('submit', (e) => {
    e.preventDefault();
    ChatApp.createGroup();
});

// 群信息按钮
document.getElementById('groupInfoBtn').addEventListener('click', () => {
    ChatApp.showGroupInfo();
});

// 关闭群信息
document.getElementById('closeGroupInfo').addEventListener('click', () => {
    document.getElementById('groupInfoModal').style.display = 'none';
});

// ====================== 群聊功能扩展 ======================
// 在 ChatApp 类中添加群聊相关方法
ChatApp.groups = [];  // 群聊列表
ChatApp.currentGroupId = null;  // 当前群聊ID

// 加载群聊列表
ChatApp.loadGroups = async function() {
    try {
        const response = await fetch('/api/group/list', {
            headers: {
                'Authorization': 'Bearer ' + getCookie('authToken')
            }
        });
        const result = await response.json();

        if (result.code === 200) {
            this.groups = result.data || [];
            this.renderGroups();
        }
    } catch (error) {
        console.error('加载群聊列表失败:', error);
    }
};

// 渲染群聊列表
ChatApp.renderGroups = function() {
    const groupList = document.getElementById('groupList');
    groupList.innerHTML = '';

    this.groups.forEach(group => {
        const li = document.createElement('li');
        li.style.cssText = `
            padding: 10px 15px;
            border-bottom: 1px solid #eee;
            cursor: pointer;
            display: flex;
            align-items: center;
            background: white;
        `;
        li.onmouseover = () => li.style.background = '#f5f5f5';
        li.onmouseout = () => li.style.background = 'white';

        li.innerHTML = `
            <div style="width: 40px; height: 40px; background: ${getAvatarColor(group.groupName)}; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 10px;">
                群
            </div>
            <div style="flex: 1;">
                <div style="font-weight: bold;">${group.groupName}</div>
                <div style="font-size: 12px; color: #666;">${group.memberCount || 0} 人</div>
            </div>
        `;

        li.onclick = () => this.selectGroup(group);
        groupList.appendChild(li);
    });
};

// 选择群聊
ChatApp.selectGroup = function(group) {
    this.currentChatUser = null;
    this.currentGroupId = group.id;

    // 更新聊天标题
    const chatTitle = document.getElementById('chatTitle');
    chatTitle.textContent = `${group.groupName} (${group.memberCount || 0}人)`;

    // 显示群信息按钮
    document.getElementById('chatActions').style.display = 'block';
    document.getElementById('groupInfoBtn').style.display = 'inline-block';

    // 清空消息区域
    messageArea.innerHTML = '';

    // 高亮选中的群
    this.highlightSelectedGroup(group.id);

    // 订阅群聊主题
    if (this.client && this.client.connected) {
        this.subscribeToGroup(group.id);
        this.loadGroupHistory(group.id);
    }

    // 更新最后阅读时间
    this.updateGroupReadTime(group.id);
};

// 订阅群聊
ChatApp.subscribeToGroup = function(groupId) {
    // 先取消之前的订阅
    if (this.currentSubscription) {
        this.currentSubscription.unsubscribe();
    }

    // 订阅群聊主题
    this.currentSubscription = this.client.subscribe(
        `/topic/group/${groupId}`,
        (message) => this.onMessageReceived(message)
    );
};

// 加载群聊历史
ChatApp.loadGroupHistory = async function(groupId) {
    try {
        const response = await fetch(`/api/group/${groupId}/history`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + getCookie('authToken')
            }
        });

        const result = await response.json();
        if (result.code === 200 && Array.isArray(result.data)) {
            // 数据库倒序查的，要正序显示
            result.data.reverse().forEach(msg => {
                this.onMessageReceived({ body: JSON.stringify(msg) });
            });
        } else {
            messageArea.innerHTML = `<li class="event-message"><p>${result.msg || '加载历史消息失败'}</p></li>`;
        }
    } catch (error) {
        console.error('加载群聊历史失败:', error);
        messageArea.innerHTML = '<li class="event-message"><p>加载历史消息失败</p></li>';
    }
};

// 显示创建群聊模态框
ChatApp.showCreateGroupModal = async function() {
    // 先加载用户列表
    await this.loadUsersForGroupCreation();
    document.getElementById('createGroupModal').style.display = 'block';
    document.getElementById('groupName').value = '';
    document.getElementById('groupDesc').value = '';
};

// 隐藏创建群聊模态框
ChatApp.hideCreateGroupModal = function() {
    document.getElementById('createGroupModal').style.display = 'none';
};

// 加载用户列表用于创建群聊
ChatApp.loadUsersForGroupCreation = async function() {
    try {
        console.log('开始加载用户列表...');
        console.log('当前 Token:', getCookie('authToken'));

        // 先不带认证头试试
        const response = await fetch('/user/online');

        console.log('响应状态:', response.status);
        console.log('响应头:', response.headers);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('HTTP 错误:', errorText);
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }

        const result = await response.json();
        console.log('响应数据:', result);

        const users = result.data || [];
        console.log('用户列表:', users);

        const memberSelectList = document.getElementById('memberSelectList');
        memberSelectList.innerHTML = '';

        if (users.length === 0) {
            memberSelectList.innerHTML = '<p style="color: #999; text-align: center;">暂无在线用户</p>';
            return;
        }

        users.forEach(user => {
            if (user.username !== this.username) {
                const div = document.createElement('div');
                div.style.cssText = `
                    padding: 8px;
                    border-bottom: 1px solid #eee;
                    display: flex;
                    align-items: center;
                `;

                // 创建复选框和标签
                const label = document.createElement('label');
                label.style.cssText = `
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    flex: 1;
                `;

                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.value = user.username;
                checkbox.style.cssText = `
                    margin-right: 10px;
                    cursor: pointer;
                `;

                const textSpan = document.createElement('span');
                textSpan.textContent = user.username;

                // 组装元素
                label.appendChild(checkbox);
                label.appendChild(textSpan);
                div.appendChild(label);

                memberSelectList.appendChild(div);
            }
        });

        console.log('用户列表渲染完成');
    } catch (error) {
        console.error('加载用户列表失败:', error);
        const memberSelectList = document.getElementById('memberSelectList');
        memberSelectList.innerHTML = '<p style="color: red;">加载失败，请重试</p>';
    }
};

// 创建群聊
ChatApp.createGroup = async function() {
    const groupName = document.getElementById('groupName').value.trim();
    const groupDesc = document.getElementById('groupDesc').value.trim();
    const memberCheckboxes = document.querySelectorAll('#memberSelectList input[type="checkbox"]:checked');
    const members = Array.from(memberCheckboxes).map(cb => cb.value);

    if (!groupName) {
        alert('请输入群聊名称');
        return;
    }

    try {
        const response = await fetch('/api/group/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + getCookie('authToken')
            },
            body: JSON.stringify({
                groupName: groupName,
                groupDesc: groupDesc,
                members: members
            })
        });

        const result = await response.json();
        if (result.code === 200) {
            alert('创建群聊成功');
            this.hideCreateGroupModal();
            // 刷新群聊列表
            this.loadGroups();
        } else {
            alert(result.msg || '创建群聊失败');
        }
    } catch (error) {
        console.error('创建群聊失败:', error);
        alert('创建群聊失败');
    }
};

// 高亮选中的群
ChatApp.highlightSelectedGroup = function(groupId) {
    // 清除所有高亮
    document.querySelectorAll('#groupList li').forEach(li => {
        li.style.background = 'white';
    });

    // 高亮选中的群
    const selectedLi = Array.from(document.querySelectorAll('#groupList li')).find(li =>
        li.onclick && li.onclick.toString().includes(groupId)
    );
    if (selectedLi) {
        selectedLi.style.background = '#e3f2fd';
    }
};

// 显示群信息
ChatApp.showGroupInfo = async function() {
    if (!this.currentGroupId) return;

    try {
        const response = await fetch(`/api/group/${this.currentGroupId}`, {
            headers: {
                'Authorization': 'Bearer ' + getCookie('authToken')
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            const group = result.data;
            const content = document.getElementById('groupInfoContent');

            content.innerHTML = `
                <div style="margin-bottom: 20px;">
                    <h4 style="margin: 0 0 10px 0;">${group.groupName}</h4>
                    <p style="margin: 0; color: #666;">${group.groupDesc || '暂无描述'}</p>
                    <p style="margin: 5px 0; color: #666;">成员数：${group.memberCount}人</p>
                    <p style="margin: 5px 0; color: #666;">创建时间：${new Date(group.createTime).toLocaleString()}</p>
                </div>
                <div>
                    <h5 style="margin-bottom: 10px;">群成员</h5>
                    <div id="groupMemberList" style="max-height: 200px; overflow-y: auto;">
                        <!-- 加载成员列表 -->
                    </div>
                </div>
            `;

            // 加载成员列表
            this.loadGroupMembers();

            document.getElementById('groupInfoModal').style.display = 'block';
        }
    } catch (error) {
        console.error('获取群信息失败:', error);
    }
};

// 加载群成员
ChatApp.loadGroupMembers = async function() {
    try {
        const response = await fetch(`/api/group/${this.currentGroupId}/members`, {
            headers: {
                'Authorization': 'Bearer ' + getCookie('authToken')
            }
        });

        const result = await response.json();
        if (result.code === 200) {
            const memberList = document.getElementById('groupMemberList');
            if (memberList) {
                memberList.innerHTML = result.data.map(member => `
                    <div style="padding: 8px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center;">
                        <span>${member.nickname || member.username} ${member.roleName ? `(${member.roleName})` : ''}</span>
                    </div>
                `).join('');
            }
        }
    } catch (error) {
        console.error('加载群成员失败:', error);
    }
};

// 更新群聊阅读时间
ChatApp.updateGroupReadTime = async function(groupId) {
    try {
        await fetch(`/api/group/${groupId}/read`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + getCookie('authToken')
            }
        });
    } catch (error) {
        console.error('更新阅读时间失败:', error);
    }
};

// 修改发送消息方法，支持群聊
ChatApp.sendMessageOriginal = ChatApp.sendMessage;
ChatApp.sendMessage = function(e) {
    e.preventDefault();

    const messageContent = messageInput.value.trim();
    if (messageContent && this.client && this.client.connected) {
        const chatMessage = {
            type: 'CHAT',
            content: messageContent,
            sender: this.username,
            target: this.currentGroupId ? this.currentGroupId.toString() : this.currentChatUser,
            chat_type: this.currentGroupId ? 'GROUP' : 'PRIVATE',
            groupId: this.currentGroupId
        };

        this.client.publish({
            destination: '/app/chat.send',
            body: JSON.stringify(chatMessage)
        });

        messageInput.value = '';
    }
};