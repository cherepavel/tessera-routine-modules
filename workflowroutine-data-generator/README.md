# Data Generator

`DataGeneratorWorkflowRoutine` — источник тестовых данных для агрегационных нод.
Каждый вызов `GenerateData` независимо выбирает целое число **0, 1 или 2**
с равной вероятностью: OK, warning, alarm. Повторяющиеся значения допустимы.
Входные метрики, аргументы функции и файл настроек генератора не требуются.

Рутина использует общую инфраструктуру выполнения из `sharedresources-project-common`:
загрузка DSL из параметров `MCg3-WorkflowRoutine-DSL` или
`MCg3-WorkflowRoutine-DSL-File`, обработка `PROCESS`/`PUT` и сохранение `DataItem`
через Storage API. Имя метрики — идентификатор результата в нижнем регистре,
значение — строка `"0"`, `"1"` или `"2"`, время — текущий `Instant`.
Для warning и alarm добавляется метка `reason` с путём ноды, чтобы проверять
перенос причин в `ProcessStatus`.

Интерфейс редактора описан через `RoutineEditorDescriptorProvider` из
`lib-tessera-dfe-project-io` той же версии, что у остальных рутин.
Фабрика выполнения и провайдер метаданных зарегистрированы через `AutoService`.

## Использование

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

Артефакт рутины: `target/workflowroutine-data-generator-1.0.0-shaded.jar`.
Общие ресурсы `sharedresources-project-common` должны быть доступны загрузчику
Tessera, как и для остальных рутин.
