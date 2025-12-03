'use strict';

// ======== Chat Client (ES2025) using @stomp/stompjs ========
// This refactor replaces the legacy Stomp.over(SockJS) API
// with the modern Client class from @stomp/stompjs.
// -----------------------------------------------------------
// 1. Works with both native WebSocket *and* SockJS fallback.
// 2. Uses ES modules (import) & modern JS syntax (const/let, arrow fn).
// 3. Encapsulates app logic in a simple ChatApp object so you can
//    extend / reuse easily (e.g. switch endpoint, add private rooms).
// ===========================================================

// Optional SockJS fallback — comment‑out if you only need pure WS.

/* ------------------------------------------------------------------
 * Element references
 * ----------------------------------------------------------------*/
const $ = (selector) => document.querySelector(selector);
const usernamePage      = $('#username-page');
const chatPage          = $('#chat-page');
const usernameForm      = $('#usernameForm');
const messageForm       = $('#messageForm');
const messageInput      = $('#message');
const messageArea       = $('#messageArea');
const connectingElement = $('.connecting');

/* ------------------------------------------------------------------
 * Theme helpers
 * ----------------------------------------------------------------*/
const colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];
const getAvatarColor = (name = '') => {
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = 31 * hash + name.charCodeAt(i);
    return colors[Math.abs(hash % colors.length)];
};

/* ------------------------------------------------------------------
 * Chat application object
 * ----------------------------------------------------------------*/
const ChatApp = {
    username: null,
    client  : /** @type {Client|null} */ (null),

    /* --------------------------- UI transitions --------------------------- */
    showChat()   { usernamePage.classList.add('hidden'); chatPage.classList.remove('hidden'); },
    showLogin()  { chatPage.classList.add('hidden');   usernamePage.classList.remove('hidden'); },
    showStartChat(Username){
        document.getElementById("submission").classList.remove('hidden');
        document.getElementById("rbutton").classList.add('hidden');
        document.getElementById("name").classList.add('hidden');
        document.getElementById("password").classList.add('hidden');
        document.querySelector("h1").innerHTML="Welcome:"+Username;
    },
     getCookie(name) {
    // 1. 获取所有cookie
    const allCookies = document.cookie;

    // 2. 按分号分割成数组
    const cookies = allCookies.split('; ');

    // 3. 遍历查找目标cookie
    for (let cookie of cookies) {
        const [cookieName, cookieValue] = cookie.split('=');
        if (cookieName === name) {
            return cookieValue;
        }
    }
    return null;
     },
    handleConnectionError() {
        console.log("连接错误，尝试重连...");
        setTimeout(() => {
            if (this.username && this.token) {
                this.client.activate();
            }
        }, 5000);
    },
    handleDisconnection() {
        console.log("连接断开处理");
        connectingElement.textContent = '连接已断开，正在重连...';
    },
    /* --------------------------- Connection ------------------------------ */
    connect(event) {
        event?.preventDefault();
        this.username = $('#name').value.trim();
        if (!this.username) return;

        this.showChat();
        // 关键修改：从Cookie读取token，而不是localStorage
        console.log("Cookie是",document.cookie);
        const token = this.getCookie('authToken'); // 根据你的Cookie名称调整
        console.log("后端传来的token="+token);
        this.token=token;
        // --- 2. 关键修改：拼接 Token 到 WebSocket URL ---
        const wsBaseUrl = 'ws://localhost:8080/ws';
        // 拼接 Token 作为 URL 参数（注意编码，避免特殊字符问题）
        const wsUrlWithToken = `${wsBaseUrl}?token=${encodeURIComponent(token)}`;
        // --- Create and activate Stomp client ---
        this.client = new StompJs.Client({
            // Use native WebSocket if brokerURL is provided; otherwise SockJS.
            // brokerURL: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`,

            brokerURL:wsUrlWithToken,
            heartbeatIncoming:0,
            heartbeatOutgoing:20000,
            reconnectDelay: 0,
            debug: (str) => console.log(str),
            onWebSocketClose:(event)=>{
                console.log("连接断开", event);
                this.hasJoined = false; // 重置标记
            },
            onConnect   : (frame) => this.onConnected(frame),
            onStompError: (frame) => {
                console.error("STOMP错误:", frame);
                // this.handleConnectionError();
                },
            onDisconnect: (frame) => {
                console.log("STOMP断开", frame);
                this.hasJoined = false;
            },
            connectHeaders:{
                Token:token,
                username:this.username
            }
        });
        this.client.activate();
    },

    disconnect() {
        this.client?.deactivate();
        this.showLogin();
    },

    /* --------------------------- Callbacks ------------------------------- */
    onConnected(frame) {
        // Subscribe to public room
        this.client.subscribe('/topic/public', (msg) => this.onMessageReceived(msg));

        //2025/12/3新增
        this.client.subscribe('/user/queue/private', (msg) => {
            // 这里复用消息显示逻辑，或者你可以写个单独的 onPrivateMessageReceived
            this.onMessageReceived(msg, true);
        });
        // Announce join
        if (!this.hasJoined) {
            this.publish('/app/chat.addUser', { sender: this.username, type: 'JOIN' });
            this.hasJoined = true;
        }

        connectingElement.classList.add('hidden');
    },

    onError(frame) {
        console.error('Broker error', frame);
        connectingElement.textContent = 'Could not connect. Please refresh to try again!';
        connectingElement.style.color = 'red';
    },

    /* --------------------------- Messaging ------------------------------- */
    sendMessage(event) {
        event?.preventDefault();

        const text = messageInput.value.trim();

        // 假设你在 HTML 里加了一个输入框 id="receiverInput"
        // 如果这个框里有字，就当是私聊；没字就是群聊
        const receiverInput = document.getElementById('receiverInput');
        const receiver = receiverInput ? receiverInput.value.trim() : null;

        if (!text || !this.client?.connected) return;

        if (receiver) {
            // === 发送私聊 ===
            this.publish('/app/chat.private', {
                sender : this.username,
                receiver: receiver, // 告诉后端发给谁
                content: text,
                type   : 'CHAT'
            });

            // 【可选】可以在这里把自己发的消息手动显示在屏幕上，因为私聊通常不会回推给自己
            this.displayLocalMessage(this.username, text, true);

        }else {
            this.publish('/app/chat.sendMessage', {
                sender: this.username,
                content: text,
                type: 'CHAT',
            });
        }

        messageInput.value = '';
    },

    publish(destination, bodyObj) {
        this.client?.publish({
            destination,
            body: JSON.stringify(bodyObj),
            headers:{
                "Token": this.token
            }
        });
    },

    onMessageReceived(message /** @type {IMessage} */,isPrivate=false) {
        const data = JSON.parse(message.body);

        const li = document.createElement('li');
        const p  = document.createElement('p');


        // 如果是私聊，给个特殊的样式，比如红色背景
        if (isPrivate) {
            li.style.border = "2px solid red";
            data.content = `[私信] ${data.content}`;
        }


        if (data.type === 'JOIN' || data.type === 'LEAVE') {
            li.classList.add('event-message');
            p.textContent = `${data.sender} ${data.type === 'JOIN' ? 'joined' : 'left'}!`;
        } else {
            li.classList.add('chat-message');

            // Avatar
            const avatar = document.createElement('i');
            avatar.textContent = data.sender?.charAt(0).toUpperCase() ?? '?';
            avatar.style.backgroundColor = getAvatarColor(data.sender);
            li.appendChild(avatar);

            // Username
            const nameSpan = document.createElement('span');
            nameSpan.textContent = data.sender;
            li.appendChild(nameSpan);

            // Message body
            p.textContent = data.content;
        }

        li.appendChild(p);
        messageArea.appendChild(li);
        messageArea.scrollTop = messageArea.scrollHeight;
    },
// 【新增】本地显示自己发的私聊消息
    displayLocalMessage(sender, content, isPrivate) {
        const li = document.createElement('li');
        li.classList.add('chat-message');
        if(isPrivate) li.style.border = "2px solid red"; // 自己的私聊也标红

        const p = document.createElement('p');
        p.textContent = `(我发给别人): ${content}`;

        li.appendChild(p);
        messageArea.appendChild(li);
        messageArea.scrollTop = messageArea.scrollHeight;
    }

};


async function register(event){
    event.preventDefault();
    const username=document.getElementById("name").value;
    const password=document.getElementById("password").value;

    let response=await fetch(
        "/Registry",
        {
            method:"POST",
            headers:{"Content-Type":"application/json",
            "testheaders":"xujunjie"},
            body:JSON.stringify({username:username,password:password}),
            credentials:"include",
            mode:"cors"
        }
    )
    if(response.status==403){
        return alert("用户已经注册");
    }

    const data=await response.text();

    // ChatApp.showStartChat(username);

    return alert(data);

};

async function login(event){
    event.preventDefault();
    const username=document.getElementById("name").value;
    const password=document.getElementById("password").value;
    let response = await fetch(
        "/Login",
        {
            method:"POST",
            headers:{"Content-Type":"application/json"},
            body:JSON.stringify({username:username,password:password}),
            credentials:"include",
            mode:"cors"
        }
    )
    if(!response.ok){
        return alert("用户不存在或者密码错误");
    }

    const result=await response.json();
    if(result.code!==200){
        return alert(result.msg||"登陆失败")
    }
    ChatApp.showStartChat(username);
    return alert("登陆成功");
}


/* ------------------------------------------------------------------
 * Event bindings
 * ----------------------------------------------------------------*/
// usernameForm.addEventListener('submit', (e) => ChatApp.connect(e), true);
messageForm.addEventListener('submit', (e) => ChatApp.sendMessage(e), true);
document.getElementById("rbutton").addEventListener('click',register,true);
document.getElementById("submission").addEventListener('click',(e)=>ChatApp.connect(e));
// Optional: neat cleanup when user navigates away
window.addEventListener('beforeunload', () => ChatApp.disconnect());
document.getElementById("login").addEventListener('click',login,true);
document.getElementById("testToken").addEventListener('click',async function(e){
       e.preventDefault();
       let response=await fetch(
           "/TestToken",
           {
               method:"GET",
               credentials:"include",
               mode:"cors"
           }
       );
       var cookie=document.cookie;
       console.log("Test函数被调用了");
       console.log(cookie);
       return alert("调用成功test方法");
});
/* ------------------------------------------------------------------
 * Notes
 * ------------------------------------------------------------------
 * 1. You can switch between pure WebSocket and SockJS by toggling
 *    brokerURL vs webSocketFactory config above.
 * 2. If you bundle with Vite/Webpack, remember to install the deps:
 *      npm i @stomp/stompjs sockjs-client
 * 3. To use without a bundler, load UMD builds:
 *      <script src="/lib/sockjs.min.js"></script>
 *      <script src="/lib/stomp.umd.js"></script>
 *      <script type="module" src="chat-client.js"></script>
 * ----------------------------------------------------------------*/
