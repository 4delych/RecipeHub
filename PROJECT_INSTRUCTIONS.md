# Инструкция для ИИ по проекту RecipeHub

## 1. Общий контекст

RecipeHub - учебное серверное веб-приложение на Java и Spring Boot.

Проект реализован как MPA: страницы формируются на сервере через Spring MVC и Thymeleaf. В проекте нет SPA-фронтенда, отдельного REST API для клиента, JavaScript-фреймворков, Bootstrap или Docker.

Основные возможности проекта:

- регистрация пользователя;
- вход и выход;
- хранение паролей в виде BCrypt-хешей;
- публикация рецептов авторизованными пользователями;
- автоматическая привязка рецепта к текущему пользователю;
- публичная лента рецептов;
- просмотр отдельного рецепта;
- отображение автора и даты публикации;
- локализация интерфейса на русский и английский;
- перевод рецептов на английский через DeepL;
- кеширование переводов рецептов в PostgreSQL;
- управление схемой базы через Flyway.

## 2. Технологии

Фактически используемый стек:

- Java 21;
- Spring Boot 4.1.1;
- Spring MVC;
- Spring Security;
- Spring Data JPA;
- Hibernate;
- Thymeleaf;
- Thymeleaf Spring Security Extras;
- PostgreSQL;
- Flyway;
- Maven;
- официальный Java-клиент DeepL.

Если нужна точная версия зависимости, нужно смотреть `pom.xml`, а не придумывать ее вручную.

## 3. Архитектура

Проект использует простую слоистую архитектуру:

```text
Controller -> Service -> Repository -> PostgreSQL
```

Назначение пакетов:

- `config` - конфигурация Spring Security, локализации и общих bean-компонентов;
- `controller` - MVC-контроллеры, принимающие HTTP-запросы и возвращающие Thymeleaf-шаблоны;
- `dto` - объекты HTML-форм с Bean Validation;
- `model` - JPA-сущности;
- `repository` - Spring Data JPA-репозитории;
- `service` - бизнес-логика приложения.

Правила:

- контроллеры не должны напрямую обращаться к базе;
- контроллеры вызывают сервисы;
- сервисы используют репозитории;
- JPA-сущности не должны использоваться как формы ввода, если для сценария уже есть DTO.

## 4. Точка входа

Точка входа:

```text
src/main/java/com/recipehub/RecipeHubApplication.java
```

Класс запускает приложение через `SpringApplication.run`.

Аннотация `@SpringBootApplication` включает автоконфигурацию и component scan для пакета `com.recipehub` и вложенных пакетов.

## 5. Конфигурация

Основной файл:

```text
src/main/resources/application.properties
```

Важные настройки:

```properties
spring.application.name=RecipeHUB

spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/recipehub}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false

deepl.api-key=${DEEPL_API_KEY:}
```

Правила:

- не сохранять реальные логины, пароли и API-ключи в проекте;
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` должны приходить из переменных окружения;
- `DEEPL_API_KEY` должен приходить из переменной окружения;
- если `DEEPL_API_KEY` отсутствует, приложение должно продолжать работать;
- Hibernate не должен создавать или изменять таблицы;
- изменения схемы выполняются только через Flyway;
- не включать обратно `spring.jpa.open-in-view=true`.

## 6. База данных и Flyway

Миграции лежат в:

```text
src/main/resources/db/migration
```

Текущие миграции:

```text
V1__init_schema.sql
V2__create_recipe_translations.sql
```

Правила работы с миграциями:

- не изменять уже примененные миграции;
- любые изменения структуры БД оформлять новой миграцией `V3__...sql`, `V4__...sql` и далее;
- не добавлять тестовые данные в production-миграции без явного требования;
- не использовать `serial`, `bigserial` и sequence-based id;
- для id использовать `uuid`;
- для PostgreSQL использовать `gen_random_uuid()`;
- не добавлять `uuid-ossp`, если это явно не требуется.

Существующие таблицы:

- `users`;
- `recipes`;
- `recipe_translations`;
- `flyway_schema_history`.

`flyway_schema_history` создается Flyway автоматически и хранит историю примененных миграций.

## 7. JPA-сущности

Сущности находятся в:

```text
src/main/java/com/recipehub/model
```

### User

Файл:

```text
model/User.java
```

Соответствует таблице `users`.

Поля:

- `UUID id`;
- `String name`;
- `String email`;
- `String passwordHash`;
- `OffsetDateTime createdAt`.

Особенности:

- `id` генерируется через `GenerationType.UUID`;
- `passwordHash` соответствует столбцу `password_hash`;
- `createdAt` соответствует `created_at`;
- `createdAt` заполняется через `@CreationTimestamp`;
- пароль в открытом виде в сущности не хранится.

### Recipe

Файл:

```text
model/Recipe.java
```

Соответствует таблице `recipes`.

Поля:

- `UUID id`;
- `String title`;
- `String description`;
- `String ingredients`;
- `String instructions`;
- `User author`;
- `OffsetDateTime createdAt`.

Связь с автором:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "author_id", nullable = false)
```

Правила:

- автор рецепта не приходит из формы;
- автор назначается по текущему authenticated user;
- не менять `LAZY` на `EAGER` без необходимости;
- не добавлять двунаправленную связь в `User`, если задача этого явно не требует.

### RecipeTranslation

Файл:

```text
model/RecipeTranslation.java
```

Соответствует таблице `recipe_translations`.

Поля:

- `UUID id`;
- `Recipe recipe`;
- `String languageCode`;
- `String title`;
- `String description`;
- `String ingredients`;
- `String instructions`;
- `OffsetDateTime createdAt`.

Особенности:

- хранит переведенную копию текстовых полей рецепта;
- оригинальный `Recipe` не изменяется;
- пара `recipe_id + language_code` уникальна;
- связь с `Recipe` должна оставаться `LAZY`.

## 8. Репозитории

Репозитории находятся в:

```text
src/main/java/com/recipehub/repository
```

### UserRepository

Методы:

```java
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
```

Используется для регистрации, входа и поиска текущего пользователя.

### RecipeRepository

Методы:

```java
@EntityGraph(attributePaths = "author")
List<Recipe> findAllByOrderByCreatedAtDesc();

List<Recipe> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

@EntityGraph(attributePaths = "author")
Optional<Recipe> findWithAuthorById(UUID id);
```

Важно:

- для ленты и страницы рецепта автор должен быть загружен заранее;
- из-за `spring.jpa.open-in-view=false` нельзя полагаться на ленивую загрузку в шаблоне;
- для загрузки автора использовать `@EntityGraph` или JPQL fetch join;
- не использовать native SQL без необходимости.

### RecipeTranslationRepository

Методы:

```java
Optional<RecipeTranslation> findByRecipeIdAndLanguageCode(UUID recipeId, String languageCode);

List<RecipeTranslation> findByRecipeIdInAndLanguageCode(Collection<UUID> recipeIds, String languageCode);
```

Используется для поиска сохраненных переводов.

## 9. DTO и валидация

DTO находятся в:

```text
src/main/java/com/recipehub/dto
```

Текущие DTO:

- `RegistrationForm`;
- `RecipeForm`.

Правила:

- HTML-формы должны работать через DTO, а не напрямую через JPA-сущности;
- использовать Bean Validation;
- сообщения валидации задавать через ключи из `messages*.properties`;
- не использовать Lombok;
- не добавлять лишние DTO без необходимости.

`RegistrationForm`:

- `name`;
- `email`;
- `password`;
- `passwordConfirmation`.

Особенности:

- `setName` делает `trim`;
- `setEmail` делает `trim`;
- пароль и подтверждение пароля не trim-ятся;
- lowercase email выполняется в `UserService`, а не в DTO.

`RecipeForm`:

- `title`;
- `description`;
- `ingredients`;
- `instructions`.

Особенности:

- `title` trim-ится;
- остальные поля не должны терять пользовательское форматирование без причины.

## 10. Регистрация

Ключевые файлы:

```text
controller/AuthController.java
service/UserService.java
dto/RegistrationForm.java
repository/UserRepository.java
templates/auth/register.html
```

Поток регистрации:

```text
GET /register
-> AuthController.registerForm
-> auth/register.html

POST /register
-> AuthController.register
-> @Valid RegistrationForm
-> BindingResult
-> UserService.register
-> UserRepository.existsByEmail
-> BCryptPasswordEncoder.encode
-> UserRepository.save
-> redirect:/login?registered
```

Правила:

- обычный пароль нельзя сохранять;
- пароль сохраняется только как BCrypt-хеш;
- email сохраняется в нижнем регистре;
- email нормализуется через `trim` и `toLowerCase(Locale.ROOT)`;
- ошибки занятого email и несовпадающих паролей должны отображаться у соответствующих полей;
- не создавать сложную иерархию исключений без необходимости.

## 11. Аутентификация и Spring Security

Ключевые файлы:

```text
config/SecurityConfig.java
service/DatabaseUserDetailsService.java
templates/auth/login.html
```

Используется вход по email и паролю.

`DatabaseUserDetailsService`:

- реализует `UserDetailsService`;
- принимает `UserRepository`;
- нормализует email через `trim` и `toLowerCase(Locale.ROOT)`;
- ищет пользователя через `findByEmail`;
- передает Spring Security email и `passwordHash`;
- назначает роль `USER` только на уровне Spring Security;
- не раскрывает, существует ли конкретный email.

`SecurityConfig`:

- настраивает `SecurityFilterChain`;
- использует `DaoAuthenticationProvider`;
- использует существующий `PasswordEncoder`;
- не должен создавать in-memory user;
- не должен отключать CSRF без явной причины;
- не должен добавлять JWT или HTTP Basic без явной задачи.

Ожидаемые правила доступа:

- `/` - публично;
- `/register` - публично;
- `/login` - публично;
- GET `/recipes/{id}` - публично;
- `/recipes/new` - только для авторизованных;
- POST `/recipes/new` - только для авторизованных;
- остальные запросы - по текущей security-конфигурации.

Logout должен оставаться POST-запросом.

## 12. Публикация рецептов

Ключевые файлы:

```text
controller/RecipeController.java
service/RecipeService.java
dto/RecipeForm.java
model/Recipe.java
repository/RecipeRepository.java
templates/recipes/new.html
```

Поток публикации:

```text
GET /recipes/new
-> RecipeController.newRecipeForm
-> recipes/new.html

POST /recipes/new
-> RecipeController.createRecipe
-> @Valid RecipeForm
-> Principal
-> RecipeService.createRecipe
-> UserRepository.findByEmail
-> new Recipe(...)
-> RecipeRepository.save
-> redirect:/recipes/{id}
```

Правила:

- `author_id` не должен приходить из формы;
- текущий автор определяется через `Principal`;
- `Principal.getName()` содержит email пользователя;
- не добавлять поля автора в форму рецепта.

## 13. Главная лента

Ключевые файлы:

```text
controller/HomeController.java
service/RecipeService.java
service/RecipeTranslationService.java
templates/index.html
```

GET `/`:

- публичный;
- получает список рецептов через `RecipeService.findAll`;
- рецепты сортируются от новых к старым;
- автор должен быть доступен без `LazyInitializationException`;
- при английской локали лента использует уже сохраненные переводы;
- лента не должна массово вызывать DeepL.

Правило: не обращаться из `HomeController` напрямую к `RecipeRepository`.

## 14. Страница рецепта

Ключевые файлы:

```text
controller/RecipeController.java
service/RecipeService.java
service/RecipeTranslationService.java
templates/recipes/details.html
```

GET `/recipes/{id}`:

- публичный;
- принимает `UUID`;
- если рецепт не найден, возвращается 404 через `ResponseStatusException(HttpStatus.NOT_FOUND)`;
- автор загружается заранее;
- контент рецепта для отображения берется через `RecipeTranslationService.getRecipeContent`.

Страница должна показывать:

- название;
- автора;
- дату;
- описание, если оно есть;
- ингредиенты;
- инструкцию;
- сообщение о недоступном переводе, если английский перевод недоступен.

Не нужно показывать отдельную надпись `Оригинал` или `Original`.

## 15. Локализация интерфейса

Ключевые файлы:

```text
config/LocaleConfig.java
resources/messages.properties
resources/messages_ru.properties
resources/messages_en.properties
```

Используется:

- `CookieLocaleResolver`;
- `LocaleChangeInterceptor`;
- параметр `lang`;
- cookie `recipehub_locale`.

Принцип работы:

```text
пользователь нажимает /?lang=en
-> LocaleChangeInterceptor считывает lang
-> CookieLocaleResolver сохраняет локаль в cookie
-> Thymeleaf берет тексты из messages_en.properties
```

Правила:

- русский язык является языком по умолчанию;
- интерфейсные строки должны идти через `messages*.properties`;
- в шаблонах использовать `#{...}`;
- при добавлении нового текста добавлять ключи во все message-файлы;
- не путать локализацию интерфейса и перевод пользовательского рецепта.

## 16. DeepL и перевод рецептов

Ключевые файлы:

```text
service/DeepLTranslationClient.java
service/RecipeTranslationService.java
repository/RecipeTranslationRepository.java
model/RecipeTranslation.java
```

Настройка:

```properties
deepl.api-key=${DEEPL_API_KEY:}
```

API-ключ должен храниться только в переменной окружения `DEEPL_API_KEY`.

### DeepLTranslationClient

Это техническая обертка над официальным Java-клиентом DeepL.

Задачи:

- получить API-ключ из конфигурации;
- создать `DeepLClient`, если ключ есть;
- определить доступность DeepL через `isAvailable`;
- отправить текстовые поля рецепта на перевод;
- вернуть результат как `TranslatedRecipe`;
- при ошибке вернуть `Optional.empty`.

Переводятся:

- `title`;
- `description`, если заполнено;
- `ingredients`;
- `instructions`.

Не переводятся:

- автор;
- дата;
- id;
- email;
- системные поля.

### RecipeTranslationService

Это бизнес-логика перевода.

Поток для страницы рецепта:

```text
RecipeController.recipeDetails
-> RecipeTranslationService.getRecipeContent(recipe, locale)
-> normalizeLanguage(locale)
-> если ru: вернуть оригинал
-> если en: поискать перевод в recipe_translations
-> если перевод есть: вернуть перевод
-> если перевода нет и DeepL недоступен: вернуть оригинал + translationUnavailable
-> если DeepL доступен: перевести, сохранить, вернуть перевод
```

Правила:

- оригинальный `Recipe` не изменять;
- перевод сохранять в `recipe_translations`;
- повторно не вызывать DeepL, если перевод уже есть;
- использовать unique constraint `recipe_id + language_code`;
- при конфликте сохранения повторно читать существующий перевод;
- не вызывать DeepL массово для общей ленты;
- лента должна использовать только уже сохраненные переводы.

## 17. Thymeleaf-шаблоны

Шаблоны находятся в:

```text
src/main/resources/templates
```

Текущие страницы:

```text
templates/index.html
templates/auth/login.html
templates/auth/register.html
templates/recipes/new.html
templates/recipes/details.html
```

Правила UI:

- использовать простой семантический HTML;
- не добавлять Bootstrap без явной задачи;
- не добавлять JavaScript без явной задачи;
- не добавлять декоративную верстку;
- не добавлять сложное меню без необходимости;
- не добавлять рекламные тексты;
- использовать Thymeleaf-атрибуты для форм, ссылок, ошибок и локализации.

Используемые Thymeleaf-механизмы:

- `th:action`;
- `th:object`;
- `th:field`;
- `th:errors`;
- `th:text`;
- `th:if`;
- `th:each`;
- `th:href`;
- `sec:authorize`;
- `sec:authentication`.

## 18. Транзакции

Правила:

- методы записи должны быть `@Transactional`;
- методы чтения можно помечать `@Transactional(readOnly = true)`;
- не держать транзакцию БД открытой во время долгого внешнего HTTP-запроса без необходимости;
- при `open-in-view=false` нужные LAZY-связи загружать в транзакции заранее.

Текущие примеры:

- регистрация пользователя - транзакция записи;
- создание рецепта - транзакция записи;
- получение рецептов - read-only транзакция;
- получение рецепта по id - read-only транзакция;
- чтение сохраненных переводов для ленты - read-only транзакция.

## 19. Безопасность

Обязательные правила:

- не хранить обычные пароли;
- не хранить реальные DB-пароли;
- не хранить реальный `DEEPL_API_KEY`;
- не отключать CSRF без явного требования;
- logout оставлять POST-запросом;
- `author_id` не принимать из формы;
- вход выполнять через Spring Security;
- пароль проверять через `PasswordEncoder`;
- не добавлять роли в БД без отдельной задачи;
- не добавлять JWT, OAuth, remember-me без отдельной задачи.

## 20. Git и работа с изменениями

ИИ не должен выполнять без явной просьбы:

- `git commit`;
- `git merge`;
- `git rebase`;
- `git checkout`;
- `git reset`;
- переключение веток;
- изменение истории.

Перед изменениями нужно:

1. Проверить текущую ветку.
2. Изучить существующий код.
3. Посмотреть незакоммиченные изменения.
4. Не перетирать чужие изменения.
5. Менять только необходимые файлы.

Если рабочее дерево грязное, считать, что изменения могли быть сделаны пользователем.

## 21. Maven и проверки

Для проверки использовать Maven Wrapper.

Windows:

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

Важно:

- проект рассчитан на Java 21;
- если окружение использует Java 17, не менять проект ради обхода;
- если отсутствуют `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, тест или запуск может упасть на подключении к PostgreSQL;
- не добавлять H2 только ради тестового запуска;
- не менять `application.properties` ради локального обхода проблемы окружения.

## 22. Что нельзя добавлять без явной задачи

Не добавлять самовольно:

- Docker;
- REST API;
- React, Vue, Angular;
- Bootstrap;
- JavaScript;
- роли и администраторов;
- лишние DTO;
- лишние интерфейсы;
- абстрактные сервисы;
- мапперы;
- сложные исключения;
- тестовые данные в миграции;
- подтверждение email;
- восстановление пароля;
- изображения рецептов;
- категории;
- комментарии;
- оценки;
- избранное;
- страницу профиля;
- редактирование профиля;
- JWT;
- OAuth;
- remember-me.

## 23. Стиль кода

Правила стиля:

- следовать уже существующему стилю проекта;
- использовать constructor injection;
- не использовать Lombok;
- не добавлять комментарии, пересказывающие очевидный код;
- не добавлять декоративные комментарии;
- не добавлять TODO без реальной необходимости;
- не создавать универсальные абстракции для простых задач;
- не усложнять учебный проект промышленными паттернами без причины;
- писать простые понятные имена классов и методов.

## 24. Типовой алгоритм работы ИИ над задачей

Перед началом:

1. Прочитать задачу полностью.
2. Проверить текущую ветку.
3. Изучить связанные файлы.
4. Проверить незакоммиченные изменения.
5. Определить минимальный набор файлов для изменения.
6. Не менять лишнее.

Во время работы:

1. Делать точечные изменения.
2. Не переписывать существующие классы полностью без необходимости.
3. Не менять миграции, которые уже применены.
4. Не менять конфигурацию безопасности без требования.
5. Не добавлять зависимости без реальной необходимости.

После работы:

1. Запустить доступные тесты.
2. Сообщить, какие файлы изменены.
3. Сообщить результат проверки.
4. Указать, если проверка невозможна из-за окружения.
5. Не выполнять commit.

## 25. Как отвечать пользователю

В финальном отчете писать кратко:

- что изменено;
- какие файлы созданы или изменены;
- как теперь работает сценарий;
- какие проверки запускались;
- результат проверок;
- если что-то не удалось проверить - честно указать причину.

Не нужно писать длинные теоретические объяснения, если пользователь просит только технический отчет.

## 26. Важные особенности проекта

- `spring.jpa.open-in-view=false`, поэтому важно заранее загружать LAZY-связи.
- `Recipe.author` - `LAZY`.
- Для ленты и страницы рецепта используется `@EntityGraph(attributePaths = "author")`.
- Интерфейс локализуется через `messages*.properties`.
- Рецепты переводятся через DeepL только на английский.
- Переводы кешируются в таблице `recipe_translations`.
- Главная лента не должна массово вызывать DeepL.
- Если DeepL недоступен, приложение показывает оригинальный текст.
- Пароли пользователей хранятся только как BCrypt-хеши.
- Автор рецепта определяется по текущему `Principal`.
- Схема БД управляется Flyway, а Hibernate только валидирует ее.

## 27. Короткое резюме

Работай с RecipeHub как с учебным Spring Boot MPA-проектом.

Сохраняй простоту. Не добавляй лишнюю архитектуру. Не меняй уже примененные миграции. Не храни секреты в коде. Не отключай безопасность без явной причины. Не ломай локализацию и DeepL-кеширование. Перед изменениями читай существующий код. После изменений запускай доступную проверку. Ничего не коммить без прямой просьбы пользователя.
