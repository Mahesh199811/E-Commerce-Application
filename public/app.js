const grid = document.querySelector("#product-grid");
const searchInput = document.querySelector("#search");
const cartDrawer = document.querySelector("#cart-drawer");
const scrim = document.querySelector("#scrim");
const cart = new Map();
let products = [];
let selectedCategory = "All";

const money = (cents) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(cents / 100);

function renderProducts() {
  const term = searchInput.value.trim().toLowerCase();
  const visible = products.filter((product) =>
    (selectedCategory === "All" || product.category === selectedCategory)
    && `${product.name} ${product.description} ${product.category}`.toLowerCase().includes(term));
  document.querySelector("#result-count").textContent = `Showing ${visible.length} considered thing${visible.length === 1 ? "" : "s"}`;
  document.querySelector("#empty-state").hidden = visible.length > 0;
  grid.innerHTML = visible.map((product, index) => `
    <article class="product-card" style="animation-delay:${Math.min(index * 45, 225)}ms">
      <div class="product-image">
        <img src="${product.imageUrl}" alt="${product.name}" loading="lazy">
        <span class="product-tag">${product.category}</span>
        <button class="add-button" data-add="${product.id}" aria-label="Add ${product.name} to bag" ${product.stock < 1 ? "disabled" : ""}>${product.stock < 1 ? "–" : "+"}</button>
      </div>
      <div class="product-info"><h3>${product.name}</h3><span class="product-price">${money(product.priceCents)}</span></div>
      <p class="product-description">${product.description}</p>
    </article>`).join("");
}

function renderCart() {
  const count = [...cart.values()].reduce((sum, quantity) => sum + quantity, 0);
  const lines = [...cart.entries()].map(([id, quantity]) => ({ product: products.find((item) => item.id === id), quantity })).filter((line) => line.product);
  document.querySelector("#cart-count").textContent = count;
  document.querySelector("#drawer-count").textContent = `(${count})`;
  document.querySelector("#subtotal").textContent = money(lines.reduce((sum, line) => sum + line.product.priceCents * line.quantity, 0));
  document.querySelector("#cart-items").innerHTML = lines.length ? lines.map(({ product, quantity }) => `
    <article class="cart-line">
      <img src="${product.imageUrl}" alt="">
      <div><h3>${product.name}</h3><p>${money(product.priceCents)}</p>
        <div class="quantity-control"><button data-change="${product.id}" data-delta="-1" aria-label="Remove one ${product.name}">−</button><span>${quantity}</span><button data-change="${product.id}" data-delta="1" aria-label="Add one ${product.name}">+</button></div>
      </div>
      <button class="remove-line" data-remove="${product.id}" aria-label="Remove ${product.name}">×</button>
    </article>`).join("") : '<p class="cart-empty">Your bag is taking a quiet moment. Add something you love.</p>';
  document.querySelector("#cart-checkout").hidden = !lines.length;
  document.querySelector("#checkout-message").textContent = "";
}

function setCartOpen(open) {
  cartDrawer.classList.toggle("open", open);
  cartDrawer.setAttribute("aria-hidden", String(!open));
  scrim.hidden = !open;
  document.body.style.overflow = open ? "hidden" : "";
  if (open) document.querySelector("#cart-close").focus();
}

document.querySelectorAll("[data-category]").forEach((button) => button.addEventListener("click", () => {
  selectedCategory = button.dataset.category;
  document.querySelectorAll(".filter-option").forEach((option) => option.classList.toggle("selected", option === button));
  renderProducts();
}));

document.querySelectorAll("[data-nav-category]").forEach((link) => link.addEventListener("click", () => {
  selectedCategory = link.dataset.navCategory;
  document.querySelectorAll(".filter-option").forEach((option) => option.classList.toggle("selected", option.dataset.category === selectedCategory));
  renderProducts();
}));

searchInput.addEventListener("input", renderProducts);
grid.addEventListener("click", (event) => {
  const button = event.target.closest("[data-add]");
  if (!button) return;
  const id = Number(button.dataset.add);
  cart.set(id, (cart.get(id) ?? 0) + 1);
  renderCart();
  setCartOpen(true);
});

document.querySelector("#cart-items").addEventListener("click", (event) => {
  const change = event.target.closest("[data-change]");
  const remove = event.target.closest("[data-remove]");
  if (remove) cart.delete(Number(remove.dataset.remove));
  if (change) {
    const id = Number(change.dataset.change);
    const quantity = (cart.get(id) ?? 0) + Number(change.dataset.delta);
    if (quantity < 1) cart.delete(id);
    else cart.set(id, quantity);
  }
  renderCart();
});

document.querySelector("#cart-open").addEventListener("click", () => setCartOpen(true));
document.querySelector("#cart-close").addEventListener("click", () => setCartOpen(false));
scrim.addEventListener("click", () => setCartOpen(false));
document.addEventListener("keydown", (event) => { if (event.key === "Escape") setCartOpen(false); });

document.querySelector("#checkout-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector("button[type=submit]");
  const message = document.querySelector("#checkout-message");
  button.disabled = true;
  message.textContent = "Sending your order...";
  const form = new URLSearchParams(new FormData(event.currentTarget));
  form.set("items", [...cart.entries()].map(([id, quantity]) => `${id}:${quantity}`).join(","));
  try {
    const response = await fetch("/api/orders", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body: form });
    const result = await response.json();
    if (!response.ok) throw new Error(result.error || "We couldn't place your order.");
    cart.clear();
    await loadProducts();
    renderCart();
    document.querySelector("#cart-items").innerHTML = `<p class="cart-empty">Order #${result.id} is in. Thanks for shopping thoughtfully.</p>`;
    event.currentTarget.reset();
  } catch (error) {
    message.textContent = error.message;
  } finally {
    button.disabled = false;
  }
});

async function loadProducts() {
  const response = await fetch("/api/products");
  if (!response.ok) throw new Error("The shop is having trouble loading. Refresh to try again.");
  products = await response.json();
  renderProducts();
}

renderCart();
loadProducts().catch((error) => {
  grid.innerHTML = `<p class="empty-state">${error.message}</p>`;
});