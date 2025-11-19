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

    /* --------------------------- Connection ------------------------------ */
    connect(event) {
        event?.preventDefault();
        this.username = $('#name').value.trim();
        if (!this.username) return;

        this.showChat();

        // --- Create and activate Stomp client ---
        this.client = new StompJs.Client({
            // Use native WebSocket if brokerURL is provided; otherwise SockJS.
            // brokerURL: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`,

            brokerURL:'/ws',

            debug: (str) => console.log(str),

            onConnect   : (frame) => this.onConnected(frame),
            onStompError: (frame) => {

                alert("token过期");
                },
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

        // Announce join
        this.publish('/app/chat.addUser', { sender: this.username, type: 'JOIN' });

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
        if (!text || !this.client?.connected) return;

        this.publish('/app/chat.sendMessage', {
            sender : this.username,
            content: text,
            type   : 'CHAT',
        });

        messageInput.value = '';
    },

    publish(destination, bodyObj) {
        this.client?.publish({
            destination,
            body: JSON.stringify(bodyObj),
            headers:{
                "Token": localStorage.getItem("Token")
            }
        });
    },

    onMessageReceived(message /** @type {IMessage} */) {
        const data = JSON.parse(message.body);

        const li = document.createElement('li');
        const p  = document.createElement('p');

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
    if(response.status==404){
        return alert("用户不存在或者密码错误");
    }
    const data=await response.text();
    localStorage.setItem("Token",data);
    ChatApp.showStartChat(username);
    return alert("登陆成功");
}

async function checktoken(e){
    e.preventDefault();

    let response = await fetch({

    })
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
