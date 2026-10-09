import { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const categories = ["All", "Living", "Kitchen", "Workspace", "Carry"];
const money = (cents) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(cents / 100);

function ProductCard({ product, quantity, onAdd, index }) {
  const soldOut = quantity >= product.stock;

  return (
    <article className="product-card" style={{ animationDelay: `${Math.min(index * 45, 225)}ms` }}>
      <div className="product-image">
        <img src={product.imageUrl} alt={product.name} loading="lazy" />
        <span className="product-tag">{product.category}</span>
        <button
          className="add-button"
          type="button"
          onClick={() => onAdd(product)}
          aria-label={`Add ${product.name} to bag`}
          disabled={soldOut}
        >
          {soldOut ? "–" : "+"}
        </button>
      </div>
      <div className="product-info">
        <h3>{product.name}</h3>
        <span className="product-price">{money(product.priceCents)}</span>
      </div>
      <p className="product-description">{product.description}</p>
    </article>
  );
}

function App() {
  const [products, setProducts] = useState([]);
  const [category, setCategory] = useState("All");
  const [search, setSearch] = useState("");
  const [cart, setCart] = useState({});
  const [cartOpen, setCartOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [checkoutMessage, setCheckoutMessage] = useState("");
  const [orderConfirmation, setOrderConfirmation] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const closeButton = useRef(null);
  const cartButton = useRef(null);

  useEffect(() => {
    async function loadProducts() {
      try {
        const response = await fetch("/api/products");
        if (!response.ok) throw new Error("The shop is having trouble loading. Refresh to try again.");
        setProducts(await response.json());
      } catch (error) {
        setLoadError(error.message);
      } finally {
        setLoading(false);
      }
    }

    loadProducts();
  }, []);

  useEffect(() => {
    if (!cartOpen) return undefined;

    closeButton.current?.focus();
    document.body.style.overflow = "hidden";
    const closeOnEscape = (event) => {
      if (event.key === "Escape") setCartOpen(false);
    };
    document.addEventListener("keydown", closeOnEscape);

    return () => {
      document.body.style.overflow = "";
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [cartOpen]);

  const visibleProducts = products.filter((product) =>
    (category === "All" || product.category === category)
    && `${product.name} ${product.description} ${product.category}`.toLowerCase().includes(search.trim().toLowerCase()));
  const cartLines = Object.entries(cart)
    .map(([id, quantity]) => ({ product: products.find((item) => item.id === Number(id)), quantity }))
    .filter((line) => line.product);
  const cartCount = cartLines.reduce((sum, line) => sum + line.quantity, 0);
  const subtotal = cartLines.reduce((sum, line) => sum + line.product.priceCents * line.quantity, 0);

  function chooseCategory(value) {
    setCategory(value);
    document.querySelector("#shop")?.scrollIntoView({ behavior: "smooth" });
  }

  function addToCart(product) {
    setCart((current) => ({
      ...current,
      [product.id]: Math.min((current[product.id] ?? 0) + 1, product.stock),
    }));
    setCheckoutMessage("");
    setOrderConfirmation("");
    setCartOpen(true);
  }

  function changeQuantity(productId, delta) {
    setCart((current) => {
      const quantity = (current[productId] ?? 0) + delta;
      if (quantity < 1) {
        const next = { ...current };
        delete next[productId];
        return next;
      }
      const product = products.find((item) => item.id === productId);
      return { ...current, [productId]: Math.min(quantity, product.stock) };
    });
    setCheckoutMessage("");
  }

  async function placeOrder(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    setSubmitting(true);
    setCheckoutMessage("Sending your order...");
    const form = new URLSearchParams(new FormData(formElement));
    form.set("items", cartLines.map(({ product, quantity }) => `${product.id}:${quantity}`).join(","));

    try {
      const response = await fetch("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: form,
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.error || "We couldn't place your order.");

      setCart({});
      setOrderConfirmation(`Order #${result.id} is in. Thanks for shopping thoughtfully.`);
      setCheckoutMessage("");
      const productsResponse = await fetch("/api/products");
      if (productsResponse.ok) setProducts(await productsResponse.json());
      formElement.reset();
    } catch (error) {
      setCheckoutMessage(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <div className="announcement">
        A little more considered. A lot more useful.
        <span>Complimentary shipping over $75</span>
      </div>

      <header className="site-header">
        <a className="wordmark" href="/" aria-label="Fieldnote Supply home">fieldnote<span>®</span></a>
        <nav className="main-nav" aria-label="Main navigation">
          <a className={category === "All" ? "active" : ""} href="#shop" onClick={() => setCategory("All")}>Shop all</a>
          <a className={category === "Living" ? "active" : ""} href="#shop" onClick={() => setCategory("Living")}>The home edit</a>
          <a className={category === "Workspace" ? "active" : ""} href="#shop" onClick={() => setCategory("Workspace")}>At the desk</a>
        </nav>
        <button className="cart-trigger" type="button" ref={cartButton} onClick={() => setCartOpen(true)} aria-label="Open shopping bag">
          Bag <span>{cartCount}</span>
        </button>
      </header>

      <main>
        <section className="intro" aria-labelledby="intro-title">
          <div className="intro-copy">
            <p className="eyebrow"><span /> OBJECTS FOR EVERYDAY RITUALS</p>
            <h1 id="intro-title">Useful things,<br /><em>thoughtfully made.</em></h1>
            <p className="intro-description">A small collection for slower mornings, better workdays, and the everyday in between.</p>
            <a className="text-link" href="#shop">Meet your new favorites <span>↘</span></a>
          </div>
          <div className="intro-image" role="img" aria-label="Sunlit, thoughtfully furnished living room">
            <span className="image-note">No. 04 &nbsp; / &nbsp; The Sunday edit</span>
          </div>
        </section>

        <section className="shop-section" id="shop" aria-labelledby="shop-title">
          <div className="shop-heading">
            <div><p className="eyebrow">THE GOOD THINGS</p><h2 id="shop-title">Find your everyday</h2></div>
            <label className="search-box">
              <span aria-hidden="true">⌕</span>
              <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Find something..." aria-label="Search products" />
            </label>
          </div>
          <div className="shop-layout">
            <aside className="filters" aria-label="Product categories">
              <p className="filter-label">BROWSE BY</p>
              {categories.map((item) => (
                <button className={`filter-option ${category === item ? "selected" : ""}`} type="button" key={item} onClick={() => setCategory(item)}>
                  {item === "All" ? "Everything" : item}<span>{item === "All" ? "08" : String(products.filter((product) => product.category === item).length).padStart(2, "0")}</span>
                </button>
              ))}
              <div className="filter-note"><span className="note-mark">✳</span><p>Made to be used.<br />Made to be kept.</p></div>
            </aside>
            <div className="product-area">
              <div className="results-line"><span>Showing {visibleProducts.length} considered thing{visibleProducts.length === 1 ? "" : "s"}</span><span>DESIGNED FOR THE EVERYDAY</span></div>
              {loading ? <p className="empty-state">Finding the good things...</p> : null}
              {loadError ? <p className="empty-state">{loadError}</p> : null}
              {!loading && !loadError && visibleProducts.length === 0 ? <p className="empty-state">No finds this time. Try another search.</p> : null}
              <div className="product-grid" aria-live="polite">
                {visibleProducts.map((product, index) => (
                  <ProductCard key={product.id} product={product} quantity={cart[product.id] ?? 0} onAdd={addToCart} index={index} />
                ))}
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="site-footer">
        <a className="wordmark" href="/">fieldnote<span>®</span></a>
        <p>Good objects. Ordinary days. Made a little better.</p>
        <span>© 2026 FIELDNOTE SUPPLY</span>
      </footer>

      {cartOpen ? <div className="scrim" onClick={() => setCartOpen(false)} aria-hidden="true" /> : null}
      <aside className={`cart-drawer ${cartOpen ? "open" : ""}`} aria-labelledby="cart-title" aria-hidden={!cartOpen} role="dialog" aria-modal={cartOpen}>
        <div className="drawer-heading">
          <div><p className="eyebrow">YOUR GOOD THINGS</p><h2 id="cart-title">Your bag <span>({cartCount})</span></h2></div>
          <button className="icon-button" type="button" ref={closeButton} onClick={() => { setCartOpen(false); cartButton.current?.focus(); }} aria-label="Close shopping bag">×</button>
        </div>
        <div className="cart-items">
          {orderConfirmation && cartLines.length === 0 ? <p className="cart-empty">{orderConfirmation}</p> : null}
          {!orderConfirmation && cartLines.length === 0 ? <p className="cart-empty">Your bag is taking a quiet moment. Add something you love.</p> : null}
          {cartLines.map(({ product, quantity }) => (
            <article className="cart-line" key={product.id}>
              <img src={product.imageUrl} alt="" />
              <div>
                <h3>{product.name}</h3><p>{money(product.priceCents)}</p>
                <div className="quantity-control">
                  <button type="button" onClick={() => changeQuantity(product.id, -1)} aria-label={`Remove one ${product.name}`}>−</button>
                  <span>{quantity}</span>
                  <button type="button" onClick={() => changeQuantity(product.id, 1)} aria-label={`Add one ${product.name}`} disabled={quantity >= product.stock}>+</button>
                </div>
              </div>
              <button className="remove-line" type="button" onClick={() => setCart((current) => { const next = { ...current }; delete next[product.id]; return next; })} aria-label={`Remove ${product.name}`}>×</button>
            </article>
          ))}
        </div>
        {cartLines.length > 0 ? (
          <div className="cart-checkout">
            <p className="shipping-note">Complimentary shipping on orders over $75.</p>
            <div className="subtotal"><span>Subtotal</span><strong>{money(subtotal)}</strong></div>
            <form onSubmit={placeOrder}>
              <label htmlFor="customer-name">Your name</label>
              <input id="customer-name" name="name" autoComplete="name" required minLength="2" maxLength="80" placeholder="Name" />
              <label htmlFor="customer-email">Email address</label>
              <input id="customer-email" name="email" type="email" autoComplete="email" required placeholder="you@example.com" />
              <button className="checkout-button" type="submit" disabled={submitting}>Place order <span>↗</span></button>
              <p className="checkout-message" role="status">{checkoutMessage}</p>
            </form>
          </div>
        ) : null}
      </aside>
    </>
  );
}

createRoot(document.getElementById("root")).render(<App />);