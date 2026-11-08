# Лабораторна робота №2 — Збірка проєкту за допомогою Gradle та робота з Java Collections Framework

## Опис проєкту

Проєкт реалізує аналізатор серверних логів `LogAnalyzer` (варіант 0). Він обробляє записи у форматі **Common Log Format (CLF)**, перевіряє їхню структуру та визначає унікальні й найчастіше вживані IPv4-адреси.

- **Валідація** побудована на регулярному виразі, складеному з окремих шаблонів для полів CLF.
- **Дата й час** додатково перевіряються через `java.time` з урахуванням реальних календарних дат.
- **Некоректні рядки** ігноруються та не впливають на результати аналізу.
- **Колекції Java** застосовуються для збирання унікальних IP-адрес, підрахунку частот і сортування результатів.
- **Збірка та тести** налаштовані через Gradle Kotlin DSL і Version Catalogs.

### Використані можливості Java

| Можливість | Де застосовано |
|---|---|
| `Pattern`, `Matcher` | Перевірка структури записів CLF та отримання IP-адрес |
| `DateTimeFormatter`, `OffsetDateTime` | Перевірка коректності дати, часу й часового поясу |
| `List`, `Set`, `LinkedHashSet` | Зберігання записів та унікальних IP-адрес |
| `Map`, `Collectors.groupingBy`, `Collectors.counting` | Підрахунок кількості входжень кожної IP-адреси |
| `Stream`, `Comparator` | Сортування за частотою та лексикографічним порядком |
| `IllegalArgumentException` | Обробка некоректних аргументів методів |

## Розрахунок варіанта

Залікової книжки немає, тому, згідно із завданням, використано порядковий номер студента у списку групи ІМ-42.

N = 21

```text
V = N mod 3 = 21 mod 3 = 0
```

Отже, виконано **варіант 0** — аналізатор логів серверів.

## Структура проєкту

```text
java-lab-2/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── README.md
└── src/
    ├── main/java/ua/kpi/comsys/collections/
    │   └── LogAnalyzer.java
    └── test/java/ua/kpi/comsys/collections/
        └── LogAnalyzerTest.java
```

Усі Java-класи знаходяться в пакеті `ua.kpi.comsys.collections`.

## Формат вхідних даних

Аналізатор приймає список рядків у форматі **Common Log Format**:

```text
127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] "GET /index.html HTTP/1.1" 200 9481
```

| Поле | Приклад | Перевірка |
|---|---|---|
| IP-адреса | `127.0.0.1` | Обов'язкова; підтримується IPv4, кожен октет — від 0 до 255 |
| `ident` | `ident` | Допустимий ідентифікатор або `-` |
| `user` | `alice` | Допустиме ім'я або `-` |
| Дата й час | `[01/May/2025:07:20:10 +0000]` | Формат CLF і коректна календарна дата; пропуск `-` або `[-]` |
| HTTP-запит | `"GET /index.html HTTP/1.1"` | Метод, ціль запиту та версія HTTP; пропуск `-` або `"-"` |
| Статус | `200` | Код від 100 до 599 або `-` |
| Кількість байтів | `9481` | Невід'ємне ціле число або `-` |

Згідно з уточненням викладача, **IP-адреса має бути присутня**, а інші поля можуть містити пропуски. Підтримки IPv4 достатньо, тому IPv6 не обробляється.

Наприклад, рядок із пропущеними іншими полями залишається коректним:

```text
127.0.0.1 - - [-] "-" - -
```

Перевірка HTTP-запиту орієнтована на поширені методи (`GET`, `POST`, `PUT`, `DELETE` тощо) та типові форми адрес. Вона не є повним RFC-валідатором усіх можливих HTTP-запитів.

## Методи аналізатора

```java
public class LogAnalyzer {
    public Set<String> getUniqueIps(List<String> logLines) { ... }
    public List<String> getTopIps(List<String> logLines, int topN) { ... }
}
```

### `getUniqueIps(List<String> logLines)`

- Повертає множину всіх унікальних IP-адрес із коректних записів.
- Використовує `LinkedHashSet`, тому зберігається порядок першої появи адрес.
- Некоректні рядки та `null`-елементи списку ігноруються.
- Якщо `logLines == null`, кидає `IllegalArgumentException("logLines cannot be null")`.

### `getTopIps(List<String> logLines, int topN)`

- Підраховує кількість входжень кожної IP-адреси.
- Повертає не більш ніж `topN` адрес за **спаданням частоти**.
- За однакової частоти застосовує **лексикографічне сортування за зростанням** (*tie-breaking*).
- Якщо унікальних IP менше за `topN`, повертає всі наявні адреси.
- Якщо `logLines == null`, кидає `IllegalArgumentException("logLines cannot be null")`.
- Якщо `topN <= 0`, кидає `IllegalArgumentException("topN must be greater than 0")`.

### Приклад використання

```java
LogAnalyzer analyzer = new LogAnalyzer();

List<String> logs = List.of(
        "127.0.0.1 - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
        "127.0.0.1 - - [-] \"-\" - -",
        "192.168.1.10 - - [02/May/2025:08:10:00 +0000] \"POST /api HTTP/1.1\" 201 256"
);

System.out.println(analyzer.getUniqueIps(logs));
System.out.println(analyzer.getTopIps(logs, 2));
```

Результат:

```text
[127.0.0.1, 192.168.1.10]
[127.0.0.1, 192.168.1.10]
```

## Вимоги до середовища

- JDK **25**
- **Gradle 9.6.0** (постачається через Gradle Wrapper, окреме встановлення не потрібне)

Перевірити Java й Gradle можна командами:

```bash
java -version
./gradlew --version
```

## Збірка та запуск тестів

Для повної збірки проєкту:

```bash
./gradlew build
```

Для запуску тестів **JUnit 5**:

```bash
./gradlew clean test
```

У Windows замість `./gradlew` можна використовувати `gradlew.bat`.

Тести перевіряють:

- коректні й некоректні IPv4-адреси та відхилення IPv6;
- допустимі значення й пропуски в усіх інших полях CLF;
- обидва варіанти пропущеного HTTP-запиту: `-` та `"-"`;
- коректність HTTP-методів, адрес запиту, версій, статусів і кількості байтів;
- календарні дати, зокрема високосні роки, та некоректний час;
- відхилення неправильно сформованих рядків;
- унікальність IP-адрес, частоти, `topN` і лексикографічний *tie-breaking*;
- порожні списки, `null`-елементи та необхідні винятки.

HTML-звіт із результатами тестування знаходиться за шляхом:

```text
build/reports/tests/test/index.html
```

У звіті відображаються кількість виконаних тестів, відсоток успішності, час виконання та помилки(якщо є).

**Скриншот результатів JUnit 5:**

<img width="1102" height="603" alt="image" src="https://github.com/user-attachments/assets/1637ba5d-6c67-49cc-9b05-c1f70fe9ec2b" />

## Покриття коду

Для оцінювання покриття використовується **JaCoCo**. У конфігурації Gradle генерація звіту запускається автоматично після виконання тестів.

Звіт можна відкрити за шляхом:

```text
build/reports/jacoco/test/html/index.html
```

За потреби звіт можна згенерувати окремою командою:

```bash
./gradlew jacocoTestReport
```

**Скриншот звіту JaCoCo:**

<img width="1103" height="217" alt="image" src="https://github.com/user-attachments/assets/850f6e74-a0d1-4072-838e-e8fb549e3173" />

## Технології

- Java 25
- Gradle 9.6.0 (Kotlin DSL, Gradle Wrapper)
- Gradle Version Catalogs (`gradle/libs.versions.toml`)
- JUnit 5.14.4
- JaCoCo 0.8.15
