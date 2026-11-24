# Data Generator

`DataGeneratorWorkflowRoutine` версии **1.1.0** — источник исходных числовых метрик
и готовых тестовых статусов. Прежняя функция `GenerateData` совместима с версией 1.0.0:
каждый вызов независимо выбирает целое число **0, 1 или 2**
с равной вероятностью: OK, warning, alarm. Повторяющиеся значения допустимы.
Входные метрики, аргументы функции и файл настроек генератора не требуются.

Источником случайных чисел служит `java.util.Random` из модуля `java.base`.
Рутина работает в Java 17 JRE без опционального модуля `jdk.random`:
поиск реализации алгоритма через `RandomGenerator.getDefault()` не используется.

Рутина использует общую инфраструктуру выполнения из `sharedresources-project-common`:
загрузка DSL из параметров `MCg3-WorkflowRoutine-DSL` или
`MCg3-WorkflowRoutine-DSL-File`, обработка `PROCESS`/`PUT` и сохранение `DataItem`
через Storage API. Для `GenerateData` имя метрики — идентификатор результата в нижнем регистре,
значение — строка `"0"`, `"1"` или `"2"`, время — текущий `Instant`.
Для warning и alarm добавляется метка `reason` с путём ноды, чтобы проверять
перенос причин в `ProcessStatus`.

Интерфейс редактора описан через `RoutineEditorDescriptorProvider` из
`lib-tessera-dfe-project-io` той же версии, что у остальных рутин.
Фабрика выполнения и провайдер метаданных зарегистрированы через `AutoService`.

## Генерация исходных числовых данных

Новая функция `GenerateNumber` выдаёт числовую метрику без вычисления статуса:

```text
PROCESS FUNCTION GenerateNumber("MetricName=temperature", "Min=0", "Max=120", "Type=DECIMAL", "PromLabel_unit=celsius") RETURN RAW;
PUT DATA RAW TO STORAGE MEASUREMENTS MODIFIER local BY DATA ID temperature;
```

| Аргумент | Назначение |
| --- | --- |
| `Min`, `Max` | Обязательные конечные числовые границы, `Min <= Max`. |
| `Type` | `DECIMAL` по умолчанию: диапазон `[Min, Max)`; `INTEGER`: обе границы включены. |
| `MetricName` | Имя метрики; по умолчанию идентификатор результата в нижнем регистре. |
| `PromLabel_*` | Метки исходной метрики, например `PromLabel_unit=celsius`. |

Для `INTEGER` границы должны быть целыми числами в диапазоне signed 64-bit.
Для `DECIMAL` используется `BigDecimal`: значение вычисляется как
`Min + (Max - Min) * random.nextDouble()` без округления границ до `double`.
При равных границах оба типа возвращают константу без обращения к random source;
это позволяет задавать точные значения для проверки условий.
Числа записываются с десятичной точкой, допускается экспоненциальная запись.
Пропущенные или повторные аргументы диапазона, неверный тип, пустое имя, `NaN`,
бесконечность, дробные INTEGER-границы и `Min > Max` завершают вызов ошибкой.

`GenerateNumber` сохраняет значение числом в строковом представлении, время текущего
вызова и заданные метки, не добавляя `reason`. Дальше метрику можно передать в
[`EvaluateStatus`](../workflowroutine-status-evaluation/README.md), а полученный статус — в `ProcessStatus`.
Полный пример цепочки находится в документации Status Evaluation.

### Воспроизводимость

Необязательный параметр **routine ENV** `Seed` задаёт signed 64-bit seed для
`java.util.Random`. Пустой или отсутствующий `Seed` использует случайный seed.
Одинаковые seed, скрипты и последовательности вызовов воспроизводят значения;
повторные `run()` продолжают последовательность, создание нового экземпляра начинает
её заново. Все вызовы `GenerateData` и `GenerateNumber` одного экземпляра используют
общий random source. Неверный или повторный `Seed` считается ошибкой инициализации.

Metadata объявляет `RoutineENV` с необязательным `Seed`, для `GenerateNumber` —
`FuncENV` и `FuncOutputData`. Контракт старой функции `GenerateData` не изменился.

## Генерация готовых статусов (совместимость с 1.0.0)

Разместить генератор в дочерней ноде и настроить выходные storage/data ID, например:

```text
PROCESS FUNCTION GenerateData RETURN STATUS;
PUT DATA STATUS TO STORAGE StatusStorage BY DATA ID Status;
```

В родительской ноде с `ProcessingStatusWorkflowRoutine` прочитать статусы двух
дочерних нод (`SourceA` и `SourceB` — их имена в графе):

```text
GET FROM CHILD SourceA STORAGE StatusStorage BY DATA ID Status AS A;
GET FROM CHILD SourceB STORAGE StatusStorage BY DATA ID Status AS B;
PROCESS FUNCTION ProcessStatus("MetricName=aggregated_status", "DataId=A", "DataId=B") RETURN TOTAL;
PUT DATA TOTAL TO STORAGE StatusStorage BY DATA ID Status;
```

Ожидаемый результат `ProcessStatus` — максимум статусов дочерних нод, от 0 до 2;
причины ненулевых статусов объединяются. Входные ноды должны завершиться до
запуска агрегационной ноды согласно зависимостям графа.

## Сборка

```sh
mvn -pl workflowroutine-data-generator -am verify
```

Артефакт рутины: `target/workflowroutine-data-generator-1.1.0-shaded.jar`.
Общие ресурсы `sharedresources-project-common` должны быть доступны загрузчику
Tessera, как и для остальных рутин.

Тест совместимости запускает фабрику и DSL рутины в отдельной JVM с
`--limit-modules java.se`, где `jdk.random` недоступен.
