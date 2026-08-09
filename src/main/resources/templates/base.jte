@import gg.jte.Content

@param Content content
@param Content? flash = null

<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>

  <title>${title}</title>

  <!-- Bootstrap CSS -->
  <link
    href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
    rel="stylesheet"
    crossorigin="anonymous"
  >
</head>

<body>
  <!-- top navigation -->
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
    <div class="container">
      <a class="navbar-brand" href="/">Мой сайт</a>

      <button
        class="navbar-toggler"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#navbarMain"
        aria-controls="navbarMain"
        aria-expanded="false"
        aria-label="Toggle navigation"
      >
        <span class="navbar-toggler-icon"></span>
      </button>

      <div class="collapse navbar-collapse" id="navbarMain">
        <ul class="navbar-nav ms-auto mb-2 mb-lg-0">
          <li class="nav-item">
            <a class="nav-link" href="/products">Товары</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" href="/about">О нас</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" href="/contact">Контакты</a>
          </li>
        </ul>
      </div>
    </div>
  </nav>

  <!-- container -->
  <main class="container py-4">
    <!-- flash -->
    @if (flash != null)
      ${flash}
    @endif

    <!-- content -->
    ${content}
  </main>

  <!-- Bootstrap JS -->
  <script
    src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
    crossorigin="anonymous"
  ></script>
</body>
</html>
