# Frontend plan — React web client

## Where the project is

Five Spring Boot microservices plus a Swing chat client, talking over REST and Kafka:

| Service            | Port | Endpoints the frontend uses |
|--------------------|------|-----------------------------|
| product-service    | 8081 | `GET/POST /api/products` (`?search=`, `?category=`), `GET /api/inventory`, `GET /api/inventory/low-stock`, `GET /api/sales`, `GET /api/sales/per-minute?minutes=` |
| customer-service   | 8082 | `POST /api/customers/register`, `POST /api/customers/login`, `GET /api/customers/{id}` (profile + order summary) |
| order-service      | 8083 | `POST /api/orders`, `GET /api/orders?customerId=`, `GET /api/orders/{id}` |
| payment-service    | 8084 | `GET /api/payments` |
| ai-agent-service   | 8085 | `POST /api/chat` (`sessionId`, `customerId`, `message`), `DELETE /api/chat/{sessionId}` |

Order flow: `POST /api/orders` → order is `PENDING` → Kafka → payment-service pays (≤ $1000 succeeds) → order becomes `PAID` or `FAILED`.
The frontend polls the order until it leaves `PENDING`.

## Decisions

- **Vite + React 18 + TypeScript** in a new `frontend/` folder (not a Maven module, so `mvn package` is unaffected).
- **react-router** for pages; no UI library — plain CSS with design tokens, light/dark.
- **Look: a printed shop catalogue.** Warm paper and ink with one signal colour (tomato), Instrument Serif for
  display type, Bricolage Grotesque for UI, JetBrains Mono for prices and figures (self-hosted via @fontsource).
  With no product photos, each product gets a typographic "plate" coloured by category. Signature pieces: a live
  stock ticker fed by the inventory stream, a receipt-style cart, a placed → payment → paid tracker, and a
  member card on the account page.
- **No backend changes.** The services have no CORS config, so the Vite dev server proxies each `/api/...` prefix
  to the right port. The browser only ever talks to `localhost:5173`.
- **Auth** is just the customer returned by `/login`, kept in `localStorage`. (The backend has no tokens.)
- **Cart**: each order holds one product, so checkout places one order per cart line and shows each order's result.

## Pages

1. **Shop** (`/`) — product grid, search box, category filter, add to cart.
2. **Cart** (`/cart`) — quantities, total, checkout (needs sign-in); live PENDING → PAID/FAILED per order.
3. **Sign in / Register** (`/login`).
4. **My account** (`/account`) — profile, totals, full order history with statuses.
5. **Admin** (`/admin`) — live inventory with low-stock highlight, sales totals, per-minute sales,
   payments list, add-product form. Auto-refreshes every 5s.
6. **Chat assistant** — floating panel on every page, keeps `sessionId`, sends `customerId` when signed in,
   "new chat" button calls `DELETE /api/chat/{sessionId}`.

## Steps

- [x] Read the services and API shapes
- [x] Scaffold `frontend/` with Vite, proxy config, typed API client
- [x] Layout, nav, auth + cart context
- [x] Shop, cart/checkout, login/register, account pages
- [x] Admin dashboard
- [x] Chat panel
- [x] `npm run build` passes; checked in the browser against a mock of the APIs
- [ ] Check against the real services (Kafka + all five running)
- [x] README section on running the frontend

## Later / out of scope

- Real auth (JWT / sessions) and admin-only access to `/admin`.
- An API gateway (Spring Cloud Gateway) so a production build needs no Vite proxy.
- Multi-product orders (needs order-service domain change).
