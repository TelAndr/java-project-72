@args java.util.List<UrlRepositoryJdbc.UrlRow> urls, String flash
<!doctype html>
<html>
<head><meta charset="utf-8"><title>Все URL</title></head>
<body>
  <h1>Все добавленные URL</h1>

  @if (flash != null) {
    <p style="color:green">@flash</p>
  @}

  @if (urls.isEmpty()) {
    <p>Пока ничего не добавлено.</p>
  @else {
    <ul>
      @for (var u : urls) {
        <li>
          <a href="/urls/@u.id()">@u.baseUrl()</a> (id=@u.id())
        </li>
      @}
    </ul>
  @}

  <a href="/">Назад</a>
</body>
</html>
