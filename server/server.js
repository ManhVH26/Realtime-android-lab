// Mock WebSocket server cho BÀI 1 (test reconnect / backoff / RTT).
// Chạy local: npm install ; npm start   (mặc định cổng 8080)
// Deploy: Render đọc render.yaml ở gốc repo (xem server/README.md).
//
// Dùng http.Server + ws gắn vào nó, để:
//   - trả 200 cho HTTP GET  -> Render health-check thấy service sống
//   - nâng cấp lên WebSocket -> client kết nối
//
// Route quyết định hành vi (nối vào URL, vd wss://<host>/echo):
//   /echo    : echo lại mọi message (giữ nguyên "PING:<t>" để client tính RTT)
//   /slow    : giống /echo nhưng trễ 2s mỗi lần trả -> để thấy RTT xấu
//   /drop    : sau 3-8s tự đóng kết nối (close 1011) -> ép client reconnect
//   /policy  : sau 3s đóng với close code 4001 = "đừng nối lại" -> client dừng hẳn

const http = require('http')
const { WebSocketServer } = require('ws')

const PORT = process.env.PORT || 8080

const server = http.createServer((req, res) => {
  // Trình duyệt / health-check của Render vào đây.
  res.writeHead(200, { 'Content-Type': 'text/plain' })
  res.end('ws lab ok - noi WebSocket toi /echo /slow /drop /policy\n')
})

const wss = new WebSocketServer({ server })

wss.on('connection', (ws, req) => {
  const path = (req.url || '/').split('?')[0]
  console.log('client kết nối:', path)
  ws.send(`welcome ${path}`)

  if (path === '/drop') {
    const ms = 3000 + Math.floor(Math.random() * 5000)
    setTimeout(() => {
      console.log('drop: đóng kết nối để test reconnect')
      ws.close(1011, 'server drop')
    }, ms)
  }

  if (path === '/policy') {
    setTimeout(() => {
      console.log('policy: đóng 4001 (đừng nối lại)')
      ws.close(4001, 'go away')
    }, 3000)
  }

  ws.on('message', (data) => {
    const text = data.toString()
    const reply = () => {
      try { ws.send(text) } catch (_) { /* socket đã đóng */ }
    }
    if (path === '/slow') setTimeout(reply, 2000)
    else reply()
  })

  ws.on('close', () => console.log('client rời:', path))
})

server.listen(PORT, () => {
  console.log(`ws lab listening on ${PORT}`)
  console.log('routes: /echo  /slow  /drop  /policy')
})
