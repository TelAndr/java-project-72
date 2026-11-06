@import java.util.List
@import hexlet.code.UrlRepositoryJdbc.UrlRow

@param java.util.List<hexlet.code.UrlRepositoryJdbc.UrlRow> urls
@param String flash
<!doctype html>
<html>
<head><meta charset="utf-8"><title>Все URL</title></head>
<body>
  <h1 data-test="urls-page">Все добавленные URL</h1>

  @if (flash != null)
    <p style="color:green">@flash</p>
  @endif

  @if (urls.isEmpty())
    <p>Пока ничего не добавлено.</p>
  @else
    <ul>
      @for (var u : urls) {
        <li>
          <a href="/urls/@u.id()">@u.baseUrl()</a> (id=@u.id())
        </li>
      @endfor
    </ul>
  @endif

  <a href="/">Назад</a>
</body>
</html>
