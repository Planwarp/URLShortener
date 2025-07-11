

## Описание

Это REST API-сервис, позволяющий получать короткие ссылки для заданных URL. Сервис обрабатывает запросы на создание коротких ссылок, получение информации по ним и перенаправление по короткому коду. Все ссылки действуют ограниченное время (10 минут с момента создания). Повторный запрос одного и того же URL возвращает ту же короткую ссылку.

## Автор

Попов Елисей

## Функциональность

- Создание короткой ссылки по длинной (POST /shorten)
- Получение информации о короткой ссылке (GET /api/{shortUrl})
- Переход по короткой ссылке с редиректом на оригинальный адрес (GET /{shortCode})

## Эндпоинты

### POST /shorten

Создает короткую ссылку для переданного `longUrl`. Если ссылка уже была создана ранее, возвращается существующий результат.

Пример запроса:
```json
{
  "longUrl": "https://vk.com"
}
````

Пример успешного ответа (201 Created):

```json
{
  "id": 3,
  "longUrl": "https://vk.com",
  "shortUrl": "xY7p2q.ru",
  "createdAt": "2025-06-15T16:24:00Z",
  "expiresAt": "2025-06-15T16:34:00Z"
}
```

Ошибки:

* 400 Bad Request, если передан пустой или некорректный URL:

```json
{
  "error": "URL не может быть пустым"
}
```

### GET /api/{shortUrl}

Возвращает информацию по короткой ссылке (например, `xY7p2q` или `xY7p2q.ru`).

Пример успешного ответа:

```json
{
  "id": 1,
  "longUrl": "https://vk.com",
  "shortUrl": "xY7p2q.ru",
  "createdAt": "2025-06-15T16:24:00Z",
  "expiresAt": "2025-06-15T16:34:00Z"
}
```

Ошибки:

* 404 Not Found, если ссылка не найдена:

```json
{
  "error": "Ссылка не найдена"
}
```

* 410 Gone, если срок действия ссылки истёк (После 10 минут):

```json
{
  "error": "Ссылка истекла"
}
```

### GET /{shortCode}

Осуществляет редирект на оригинальный URL. Используется при открытии короткой ссылки в браузере.

Успешный ответ:

* 302 Found с заголовком `Location: https://vk.com`

Ошибки:

* 400 Bad Request, если короткий код имеет неверный формат:

```json
{
  "error": "Неправильный формат ссылки"
}
```

* 404 Not Found, если код не найден:

```json
{
  "error": "Ссылка не найдена"
}
```

* 410 Gone, если ссылка устарела (После 10 минут):

```json
{
  "error": "Ссылка истекла"
}
```

## Параметры запросов

### POST /shorten

| Параметр | Тип    | Обязателен | Описание                        |
| -------- | ------ | ---------- | ------------------------------- |
| longUrl  | String | Да         | Оригинальный URL (https\://...) |

### GET /api/{shortUrl}

| Параметр | Тип    | Обязателен | Описание                       |
| -------- | ------ | ---------- | ------------------------------ |
| shortUrl | String | Да         | Код или полная короткая ссылка |

### GET /{shortCode}

| Параметр  | Тип    | Обязателен | Описание            |
| --------- | ------ | ---------- | ------------------- |
| shortCode | String | Да         | Код короткой ссылки |

## Ответы сервера

### POST /shorten

* 201 Created — ссылка создана
* 200 OK — ссылка уже существует
* 400 Bad Request — пустой или невалидный `longUrl`

### GET /api/{shortUrl}

* 200 OK — информация о ссылке найдена
* 404 Not Found — ссылка не найдена
* 410 Gone — срок действия ссылки истек

### GET /{shortCode}

* 302 Found — редирект на оригинальный URL
* 400 Bad Request — неверный формат кода
* 404 Not Found — ссылка не найдена
* 410 Gone — ссылка истекла

## Техническая реализация

Приложение разработано на Spring Boot. В качестве базы данных используется H2 (in-memory). Все запросы и ответы, кроме редиректа, используют формат JSON. Сервер запускается на порту 8080 (можно изменить в `application.properties`).

### Структура таблицы LINK

| Поле        | Тип       | Описание                                      |
| ----------- | --------- | --------------------------------------------- |
| ID          | Long      | Автоинкрементный идентификатор                |
| LONG\_URL   | String    | Полный URL                                    |
| SHORT\_URL  | String    | Короткий код или ссылка (например, xY7p2q.ru) |
| CREATED\_AT | Timestamp | Дата и время создания                         |
| EXPIRES\_AT | Timestamp | Время истечения (через 10 минут)              |

## Интеграция

API является автономным. Возможные клиенты: веб-приложения, мобильные клиенты, браузеры и автоматизированные скрипты.

Пример сценария:

1. Отправка POST-запроса на `/shorten`:

```json
{
  "longUrl": "https://vk.com"
}
```

2. Получение ответа:

```json
{
  "shortUrl": "xY7p2q.ru"
}
```

3. Открытие короткой ссылки в браузере:

```
http://localhost:8080/xY7p2q или http://localhost:8080/xY7p2q.ru
```

Редирект происходит автоматически на `https://vk.com`.

Для проверки содержимого базы данных используется веб-интерфейс H2. После запуска приложения можно открыть:
```
http://localhost:8080/h2-console
```
и подключиться к базе, чтобы просматривать таблицу LINK. Там сохраняются все данные о ссылках, включая исходные и короткие URL, дату создания и время истечения.