@import hexlet.code.UrlRepositoryJdbc.UrlRow
@param hexlet.code.UrlRepositoryJdbc.UrlRow url
@param String flash
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>URL ${url.id()}</title>
</head>
<body>
  <h1>Страница</h1>

  @if (flash != null) {
    <p style="color:green">${flash}</p>
  @}
  @endif

  <p><b>ID:</b> ${url.id()}</p>
  <p><b>Base URL:</b> ${url.baseUrl()}</p>

  <form method="post" action="/urls/${url.id()}/checks">
      <button type="submit">Запустить проверку</button>
  </form>

    <table data-test="checks">
      <thead>
        <tr>
          <th>ID</th>
          <th>Код ответа</th>
          <th>h1</th>
          <th>title</th>
          <th>description</th>
          <th>Дата создания</th>
        </tr>
      </thead>
      <tbody>
        <!-- строки с результатами, если есть -->
      </tbody>
    </table>

  <a href="/urls">Все URL</a>
  <a href="/">Назад</a>
</body>
</html>
