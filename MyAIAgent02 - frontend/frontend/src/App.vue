<template>
  <div class="app-container">
    <header class="header">
      <div class="header-content">
        <div>
          <h1>🤖 AI Agent Chat</h1>
          <p>{{ t('header.subtitle') }}</p>
        </div>
        <div class="language-switch">
          <button @click="switchLanguage('zh')" :class="{ active: currentLang === 'zh' }">{{ t('languageSwitch.chinese') }}</button>
          <button @click="switchLanguage('en')" :class="{ active: currentLang === 'en' }">{{ t('languageSwitch.english') }}</button>
        </div>
      </div>
    </header>

    <div class="main-content">
      <div class="sidebar">
        <div class="sidebar-section">
          <h3>{{ t('sidebar.sessions') }}</h3>
          <div class="sessions-list">
            <div v-for="session in sessions" :key="session.sessionId"
                 :class="['session-item', { active: session.sessionId === activeSessionId }]"
                 @click="switchSession(session.sessionId)">
              <div class="session-title">{{ session.title || t('sidebar.sessionUntitled') }}</div>
              <div class="session-meta">
                <span class="message-count">{{ session.messageCount }} {{ t('sidebar.sessionMessages') }}</span>
                <span class="last-active">{{ formatLastActive(session.lastActiveAt) }}</span>
              </div>
              <div class="session-actions">
                <button class="session-action-btn" @click.stop="renameSession(session.sessionId)">{{ t('sidebar.renameButton') }}</button>
                <button class="session-action-btn delete" @click.stop="deleteSession(session.sessionId)">{{ t('sidebar.deleteButton') }}</button>
              </div>
            </div>
            <div v-if="sessions.length === 0" class="no-sessions">
              {{ t('sidebar.noSessions') }}
            </div>
          </div>
          <div class="session-controls">
            <input v-model="newSessionTitle" :placeholder="t('sidebar.newSessionPlaceholder')" @keyup.enter="createSession" />
            <button @click="createSession" :disabled="!newSessionTitle">{{ t('sidebar.newSessionButton') }}</button>
          </div>
        </div>

        <div class="sidebar-section">
          <h3>{{ t('sidebar.documents') }}</h3>
          <div class="upload-area">
            <textarea v-model="documentText" :placeholder="t('sidebar.documentPlaceholder')"></textarea>
            <input v-model="documentId" :placeholder="t('sidebar.documentIdPlaceholder')" />
            <button @click="uploadDocument" :disabled="!documentText">{{ t('sidebar.uploadButton') }}</button>
          </div>
        </div>

        <div class="sidebar-section">
          <h3>{{ t('sidebar.skills') }}</h3>
          <div class="skills-list">
            <div v-for="skill in skills" :key="skill.name" class="skill-item">
              <strong>{{ skill.name }}</strong>
              <p>{{ skill.description }}</p>
            </div>
          </div>
          <div class="skill-execute">
            <select v-model="selectedSkill">
              <option value="">{{ t('sidebar.skillPlaceholder') }}</option>
              <option v-for="skill in skills" :key="skill.name" :value="skill.name">{{ skill.name }}</option>
            </select>
            <input v-model="skillInput" :placeholder="t('sidebar.skillInputPlaceholder')" />
            <button @click="executeSkill" :disabled="!selectedSkill">{{ t('sidebar.executeButton') }}</button>
          </div>
        </div>

        <div class="sidebar-section">
          <h3>{{ t('sidebar.info') }}</h3>
          <p>{{ t('sidebar.infoDescription') }}</p>
          <p>{{ t('sidebar.backend') }}</p>
          <p>{{ t('sidebar.frontend') }}</p>
        </div>
      </div>

      <div class="chat-area">
        <div class="messages" ref="messagesContainer">
          <div v-for="(message, index) in messages" :key="index" :class="['message', message.type]">
            <div class="avatar">{{ message.type === 'user' ? '👤' : '🤖' }}</div>
            <div class="content">
              <div class="text">{{ message.text }}</div>
              <div class="timestamp">{{ message.timestamp }}</div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <textarea
            v-model="userInput"
            :placeholder="t('chat.placeholder')"
            @keyup.enter.exact="sendMessage"
          ></textarea>
          <button @click="sendMessage" :disabled="!userInput">{{ t('chat.sendButton') }}</button>
        </div>
      </div>
    </div>

    <footer class="footer">
      <p>{{ t('footer.description') }}{{ activeSessionDisplay }}</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
const currentLang = ref(locale.value)

const messages = ref([])
const userInput = ref('')
const documentText = ref('')
const documentId = ref('')
const skills = ref([])
const selectedSkill = ref('')
const skillInput = ref('')
const sessionId = ref(generateSessionId())
const messagesContainer = ref(null)

// Session management
const sessions = ref([])
const activeSessionId = ref('')
const newSessionTitle = ref('')
const isLoadingSessions = ref(false)

const activeSessionDisplay = computed(() => {
  if (!activeSessionId.value) return sessionId.value
  const session = sessions.value.find(s => s.sessionId === activeSessionId.value)
  return session ? `${session.title || t('sidebar.sessionUntitled')} (${session.sessionId})` : sessionId.value
})

function generateSessionId() {
  return 'session-' + Math.random().toString(36).substr(2, 9)
}

// Session management functions
async function loadSessions() {
  isLoadingSessions.value = true
  try {
    const response = await fetch('http://localhost:8081/api/sessions')
    if (response.ok) {
      sessions.value = await response.json()
      // Restore active session from localStorage or use first session
      const savedSessionId = localStorage.getItem('activeSessionId')
      if (savedSessionId && sessions.value.some(s => s.sessionId === savedSessionId)) {
        activeSessionId.value = savedSessionId
        sessionId.value = savedSessionId
      } else if (sessions.value.length > 0) {
        activeSessionId.value = sessions.value[0].sessionId
        sessionId.value = sessions.value[0].sessionId
        localStorage.setItem('activeSessionId', activeSessionId.value)
      }
      // Load messages for active session
      if (activeSessionId.value) {
        await loadSessionHistory(activeSessionId.value)
      }
    }
  } catch (error) {
    console.error('Failed to load sessions:', error)
  } finally {
    isLoadingSessions.value = false
  }
}

async function loadSessionHistory(sessionIdToLoad) {
  try {
    const response = await fetch(`http://localhost:8081/api/history/${sessionIdToLoad}`)
    if (response.ok) {
      const history = await response.json()
      messages.value = history.map(msg => ({
        text: msg.content,
        type: msg.role,
        timestamp: new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      }))
    }
  } catch (error) {
    console.error('Failed to load session history:', error)
  }
}

async function createSession() {
  const title = newSessionTitle.value.trim()
  if (!title) return

  try {
    const response = await fetch('http://localhost:8081/api/sessions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        title: title,
        sessionId: null // Let backend generate
      })
    })
    if (response.ok) {
      const newSession = await response.json()
      sessions.value.push(newSession)
      newSessionTitle.value = ''
      // Switch to the new session
      await switchSession(newSession.sessionId)
    }
  } catch (error) {
    console.error('Failed to create session:', error)
  }
}

async function switchSession(sessionIdToSwitch) {
  activeSessionId.value = sessionIdToSwitch
  sessionId.value = sessionIdToSwitch
  localStorage.setItem('activeSessionId', sessionIdToSwitch)
  await loadSessionHistory(sessionIdToSwitch)
}

async function renameSession(sessionIdToRename) {
  const newTitle = prompt('Enter new session title:')
  if (!newTitle || !newTitle.trim()) return

  try {
    const response = await fetch(`http://localhost:8081/api/sessions/${sessionIdToRename}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        title: newTitle
      })
    })
    if (response.ok) {
      const updatedSession = await response.json()
      const index = sessions.value.findIndex(s => s.sessionId === sessionIdToRename)
      if (index !== -1) {
        sessions.value[index] = updatedSession
      }
    }
  } catch (error) {
    console.error('Failed to rename session:', error)
  }
}

async function deleteSession(sessionIdToDelete) {
  if (!confirm('Are you sure you want to delete this session? All messages will be lost.')) {
    return
  }

  try {
    const response = await fetch(`http://localhost:8081/api/sessions/${sessionIdToDelete}`, {
      method: 'DELETE'
    })
    if (response.ok) {
      sessions.value = sessions.value.filter(s => s.sessionId !== sessionIdToDelete)
      if (activeSessionId.value === sessionIdToDelete) {
        // Switch to another session if available
        if (sessions.value.length > 0) {
          await switchSession(sessions.value[0].sessionId)
        } else {
          activeSessionId.value = ''
          sessionId.value = generateSessionId()
          messages.value = []
          localStorage.removeItem('activeSessionId')
        }
      }
    }
  } catch (error) {
    console.error('Failed to delete session:', error)
  }
}

function formatLastActive(timestamp) {
  if (!timestamp) return 'Never'
  const date = new Date(timestamp)
  const now = new Date()
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return `${diffMins}m ago`
  if (diffHours < 24) return `${diffHours}h ago`
  return `${diffDays}d ago`
}

// 切换语言
function switchLanguage(lang) {
  locale.value = lang
  currentLang.value = lang
  localStorage.setItem('lang', lang) // 保存语言设置到本地存储
}

function addMessage(text, type) {
  const now = new Date()
  const timestamp = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  messages.value.push({
    text,
    type,
    timestamp
  })
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

async function sendMessage() {
  const text = userInput.value.trim()
  if (!text) return

  addMessage(text, 'user')
  userInput.value = ''

  try {
    const response = await fetch('http://localhost:8081/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'X-Requested-With': 'XMLHttpRequest'
      },
      body: JSON.stringify({
        message: text,
        sessionId: sessionId.value
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    if (response.body) {
      const reader = response.body.getReader()
      const decoder = new TextDecoder()

      // 添加一个空消息作为占位符
      addMessage('', 'bot')
      const messageIndex = messages.value.length - 1

      let buffer = ''
      let partialEvent = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // 处理缓冲区中的所有完整事件（以两个换行符分隔）
        while (true) {
          // 查找两个连续的换行符（事件分隔符）
          const eventEnd = buffer.indexOf('\n\n')
          if (eventEnd === -1) {
            // 没有完整的事件，保留在buffer中等待更多数据
            break
          }

          // 提取一个完整的事件（包含两个换行符）
          const eventData = buffer.substring(0, eventEnd + 2)
          buffer = buffer.substring(eventEnd + 2)

          // 处理这个SSE事件
          processSSEEvent(eventData, messageIndex)
        }
      }

      // 处理缓冲区中剩余的未完成数据
      if (buffer.trim() !== '') {
        // 如果还有剩余数据，可能是部分事件，保存到partialEvent中
        partialEvent = buffer
        // 检查是否是一个完整的data:行（但没有结束符）
        if (partialEvent.startsWith('data:') && !partialEvent.includes('\n\n')) {
          const content = partialEvent.substring(5).trim()
          if (content !== '[DONE]' && content !== '') {
            messages.value[messageIndex].text += content
            scrollToBottom()
          }
        }
      }

      // 处理SSE事件的辅助函数
      function processSSEEvent(eventData, messageIndex) {
        // 按行分割事件
        const lines = eventData.split('\n')

        for (const line of lines) {
          // 跳过空行
          if (line.trim() === '') {
            continue
          }

          // 处理SSE字段
          if (line.startsWith('data:')) {
            const content = line.substring(5).trim()

            // 检查是否为结束标记
            if (content === '[DONE]') {
              return
            }

            // 添加到消息内容
            if (content !== '') {
              messages.value[messageIndex].text += content
              scrollToBottom()
            }
          }
          // 可以处理其他SSE字段如event:, id:, retry:等
          else if (line.startsWith(':')) {
            // 注释行，忽略
            continue
          }
          // 如果不是标准的SSE字段，可能是格式错误，但为了兼容性，尝试作为普通文本处理
          else if (line.trim() !== '') {
            // 作为普通文本追加（向后兼容）
            messages.value[messageIndex].text += line.trim()
            scrollToBottom()
          }
        }
      }
    } else {
      throw new Error('Response body is null')
    }
  } catch (error) {
    addMessage(t('messages.errorResponse'), 'bot')
    console.error(error)
  }
}

async function uploadDocument() {
  const text = documentText.value.trim()
  if (!text) return

  try {
    const response = await fetch('http://localhost:8081/api/upload', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/plain,application/json,*/*',
        'X-Requested-With': 'XMLHttpRequest'
      },
      body: JSON.stringify({
        text,
        documentId: documentId.value
      })
    })
    if (response.ok) {
      addMessage(t('messages.documentUploaded', {0: documentId.value || 'N/A'}), 'bot')
      documentText.value = ''
      documentId.value = ''
    } else {
      addMessage(t('messages.failedUpload'), 'bot')
    }
  } catch (error) {
    addMessage(t('messages.errorUpload'), 'bot')
    console.error(error)
  }
}

async function fetchSkills() {
  try {
    const response = await fetch('http://localhost:8081/api/skills')
    if (response.ok) {
      skills.value = await response.json()
    }
  } catch (error) {
    console.error('Failed to fetch skills:', error)
  }
}

async function executeSkill() {
  if (!selectedSkill.value) return

  const input = skillInput.value.trim()
  try {
    const response = await fetch('http://localhost:8081/api/skill', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/plain,application/json,*/*',
        'X-Requested-With': 'XMLHttpRequest'
      },
      body: JSON.stringify({
        skillName: selectedSkill.value,
        input
      })
    })
    const result = await response.text()
    addMessage(t('messages.skillExecution', {0: selectedSkill.value, 1: result}), 'bot')
    skillInput.value = ''
  } catch (error) {
    addMessage(t('messages.errorSkill', {0: error.message}), 'bot')
    console.error(error)
  }
}

onMounted(async () => {
  await loadSessions()
  fetchSkills()
})
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 24px 32px;
  text-align: left;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content div:first-child h1 {
  font-size: 2.5rem;
  margin-bottom: 8px;
}

.header-content div:first-child p {
  opacity: 0.9;
  font-size: 1.1rem;
}

.language-switch {
  display: flex;
  gap: 8px;
}

.language-switch button {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.language-switch button:hover {
  background: rgba(255, 255, 255, 0.3);
}

.language-switch button.active {
  background: rgba(255, 255, 255, 0.4);
  font-weight: bold;
}

.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.sidebar {
  width: 320px;
  background: #f8f9fa;
  padding: 24px;
  overflow-y: auto;
  border-right: 1px solid #e9ecef;
}

.sidebar-section {
  margin-bottom: 32px;
}

.sidebar-section h3 {
  margin-bottom: 16px;
  color: #495057;
  font-size: 1.2rem;
}

.upload-area textarea,
.upload-area input,
.skill-execute input,
.skill-execute select {
  width: 100%;
  padding: 12px;
  margin-bottom: 12px;
  border: 1px solid #ced4da;
  border-radius: 8px;
  font-family: inherit;
}

.upload-area textarea {
  height: 120px;
  resize: vertical;
}

button {
  background: #667eea;
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.2s;
  width: 100%;
}

.language-switch button {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.language-switch button:hover {
  background: rgba(255, 255, 255, 0.3);
}

.language-switch button.active {
  background: rgba(255, 255, 255, 0.4);
  font-weight: bold;
}

button:hover {
  background: #5a67d8;
}

button:disabled {
  background: #a0aec0;
  cursor: not-allowed;
}

.skills-list {
  margin-bottom: 20px;
}

.skill-item {
  background: white;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 12px;
  border: 1px solid #e9ecef;
}

.skill-item strong {
  color: #667eea;
}

.skill-item p {
  font-size: 0.9rem;
  color: #6c757d;
  margin-top: 4px;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 24px;
  background: white;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  margin-bottom: 24px;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  background: #f8f9fa;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message .avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #667eea;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  margin: 0 12px;
}

.message.user .avatar {
  background: #764ba2;
}

.message .content {
  max-width: 70%;
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.message.user .content {
  background: #667eea;
  color: white;
}

.message .text {
  line-height: 1.5;
}

.message .timestamp {
  font-size: 0.8rem;
  opacity: 0.7;
  margin-top: 8px;
}

.input-area {
  display: flex;
  gap: 12px;
}

.input-area textarea {
  flex: 1;
  padding: 16px;
  border: 1px solid #ced4da;
  border-radius: 12px;
  font-family: inherit;
  resize: none;
  height: 80px;
}

.input-area button {
  width: auto;
  padding: 16px 32px;
  align-self: flex-end;
}

/* Session management styles */
.sessions-list {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  background: white;
}

.session-item {
  padding: 12px;
  border-bottom: 1px solid #f1f3f4;
  cursor: pointer;
  transition: background-color 0.2s;
}

.session-item:hover {
  background-color: #f8f9fa;
}

.session-item.active {
  background-color: #e3f2fd;
  border-left: 3px solid #2196f3;
}

.session-item:last-child {
  border-bottom: none;
}

.session-title {
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  color: #666;
}

.message-count {
  color: #5c6bc0;
}

.last-active {
  color: #9e9e9e;
}

.session-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.session-action-btn {
  padding: 4px 8px;
  font-size: 0.75rem;
  background: #f1f3f4;
  color: #5f6368;
  border: 1px solid #dadce0;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  width: auto;
}

.session-action-btn:hover {
  background: #e8eaed;
}

.session-action-btn.delete {
  background: #fce8e6;
  color: #d93025;
  border-color: #fad2cf;
}

.session-action-btn.delete:hover {
  background: #fad2cf;
}

.no-sessions {
  padding: 20px;
  text-align: center;
  color: #9e9e9e;
  font-style: italic;
}

.session-controls {
  display: flex;
  gap: 8px;
}

.session-controls input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.9rem;
}

.session-controls button {
  padding: 8px 16px;
  font-size: 0.9rem;
  width: auto;
}

.footer {
  background: #f8f9fa;
  padding: 16px;
  text-align: center;
  color: #6c757d;
  font-size: 0.9rem;
  border-top: 1px solid #e9ecef;
}
</style>