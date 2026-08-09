@args UrlRepositoryJdbc.UrlRow url, String flash
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>URL @url.id()</title>
</head>
<body>
  <h1>Страница</h1>

  @if (flash != null) {
    <p style="color:green">@flash</p>
  @}

  <p><b>ID:</b> @url.id()</p>
  <p><b>Base URL:</b> @url.baseUrl()</p>

  <a href="/urls">Все URL</a>
  <a href="/">Назад</a>
</body>
</html>
