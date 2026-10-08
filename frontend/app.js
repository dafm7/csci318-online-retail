const PRODUCT_API = 'http://localhost:8081/api/products';
const CHAT_API = 'http://localhost:8085/api/chat';

async function loadProducts() {
  const box = document.getElementById('products');
  try {
    const res = await fetch(PRODUCT_API);
    const products = await res.json();
    box.innerHTML = products.map(p => `
      <div class="product">
        <span>${p.name} <small>(${p.category})</small></span>
        <span>$${p.price.toFixed(2)} · ${p.stock} in stock</span>
      </div>`).join('');
  } catch (err) {
    box.textContent = 'Could not reach product-service on port 8081.';
  }
}

function addMsg(text, who) {
  const div = document.createElement('div');
  div.className = 'msg ' + who;
  div.textContent = (who === 'user' ? 'You: ' : 'Assistant: ') + text;
  document.getElementById('chat-log').appendChild(div);
}

document.getElementById('chat-form').addEventListener('submit', async e => {
  e.preventDefault();
  const input = document.getElementById('chat-input');
  const message = input.value.trim();
  if (!message) return;
  addMsg(message, 'user');
  input.value = '';
  try {
    const res = await fetch(CHAT_API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message })
    });
    const data = await res.json();
    addMsg(data.reply, 'bot');
  } catch (err) {
    addMsg('Could not reach ai-agent-service on port 8085.', 'bot');
  }
});

loadProducts();
