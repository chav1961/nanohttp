## Nano HTTP Server

### Общее описание

Данный репозитарий содержит обертку над небезызвестным Java-классом [com.sun.net.httpserver.HttpServer](https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html), который можно использовать в качестве простейшего WEB-сервера. Обертка запускается как обычное приложение, не требует предварительной установки, практически не требует администрирования, и позволяет в некоторых случаях обойтись без полноценного WEB-программирования. 

Помимо обычной функциональности HTTP-сервера (включая поддержку защищенных протоколов обмена), обертка поддерживает несколько дополнительных возможностей:

- дерево статических страниц сайта (на основе интерфейса файловой системы моей библиотеки [Pure Library](https://github.com/chav1961/purelib) )
- простейший OpenAPI интерфейс (на основе документа [JSR-311](https://jcp.org/en/jsr/detail?id=311) ), позволяющий писать для сервера легковесные плагины - аналоги REST-сервисов
- JMX-интерфейс для управления сервером

Параметры запуска обертки следующие:

> java -jar nanohttp.jar [<режим>] -conf URI \[-appDir URI] \[-d]

где:
- **режим** - режим управления сервером. Допустимы следующие значения:
    - *start* - запустить ранее остановленный сервер
    - *suspend* - временно запретить прием и обработку HTTP-запросов сервером
    - *resume* - возобновить прием и обработку HTTP-запросов сервером
    -	*stop* - остановить ранее запущенный сервер
    -	*terminateAndExit* - остановить ранее запущенный сервер и завершить его работу как приложения
- **-conf** - URI источника конфигурации (например, файла).
- **-appDir** - директория c файлам плагинов.
- **-d** - флаг включения отладочного вывода в поток System.err

При первоначальном запуске сервера параметр **режим** НЕ должен задаваться. Все режимы используются только для управления уже ранее запущенным сервером.

Источник конфгурации сервера может иметь любую природу и должен содержать в себе данные в формате, совместимом с Java-классом [java.util.Properties](https://en.wikipedia.org/wiki/.properties). Список допустимых ключей конфигурации приведен в таблице:

| Параметр | По умолчанию | Назначение параметра |
|------|-------|-------|
|nanoservicePort | 8080 | порт, на котором следует поднять сервер |
|nanoserviceRoot | | URI файловой системы, которая будет использована в качестве *корня* сайта. При отсутствии параметра статическая часть сайта не будет поддержана. |
|nanoserviceLocalhostOnly | true | разрешает обработку запросов только с того же компьютера, на котором запущен сам сервер |
|nanoserviceExecutorPoolSize | 10 | размер пула потоков для обработки запросов к серверу |
|nanoserviceDisableLoopback | true | отключение возможности обработки эхо-запросов (эхо-запросы бывают полезны при отладке) |
|nanoserviceTemporaryCacheSize | | |
|nanoserviceCreolePrologueURI | | URI источника данных, вставляемых при автоматической генерации страниц сайта для Creole-страниц |
|nanoserviceCreoleEpilogueURI | | URI источника данных, вставляемых при автоматической генерации страниц сайта для Creole-страниц |
|nanoserviceUseSSL | false | разрешает использование защищенного протокола (SSL, TLS) |
|nanoserviceUseKeyStore | false | разрешает использование хранилища ключей (SSL, TLS) |
|nanoserviceSSLKeyStore | | местоположения хранилища ключей |
|nanoserviceSSLKeyStoreType | | тип хранилища ключей |
|nanoserviceSSLKeyStorePasswd | | пароль хранилища ключей |
|nanoserviceUseTrustStore | false | разрешает использование хранилища доверенных сертификатов (SSL, TLS) |
|nanoserviceSSLTrustStore | | мстоположение хранилища доверенных сертификатов |
|nanoserviceSSLTrustStoreType | | тип хранилища доверенных сертификатов |
|nanoserviceSSLTrustStorePasswd | | пароль хранилища доверенных сертификатов |

### Настройка статической части сайта

Статическая часть сайта - поддерево файловой системы, в которой расположены файлы различного формата. Приложение особым образом обрабатывает
файлы формата \*.html и файлы формата \*.cre - из них генерируются страницы статической части сайта. Все остальные форматы файлов используются
"as-is". Корень поддерева со статической частью сайта задается параметром **nanoserviceRoot** в источнике конфигурации приложения. Ссылка на него
внутри страниц сайта выглядит как href="/".

### Консоль сервера

Сервер может управляться консольными командами. Набор команд приведен в таблице:

| Формат команды | Назначение |
|------|-------|
| deploy <class> to <path> | Загрузить плагин и привязать его к указанному пути |
| undeploy from <path> | Отвязать плагин от указанного пути и выгрузить его |
| list | Получить список плагинов с привязкой их к путям |
| start |  Запустить сервер в работу |
| suspend | Приостановить обработку запросов сервером |
| resume | Возобновить обработку запросов сервером |
| stop | Остановить сервер |
| restart | Перезапустить сервер |
| state | Получить текущее состояние сервера |
| exit | Завершить рабоут приложения |
| help | Выдать справку по консольным командам |

### SPI сервисы

Для расширения функциональности сервера в нем имеется набор SPI-сервисов. Все они представляют собой интерфейсы пакета chav1961.nanothhp.interfaces. 
На данный момент в системе задействованы следующие сервисы:

| SPI-сервис | Назначение сервиса | Реализации |
|------|-------|-------|
| NanoClassSerializer | Сериализатор содержимого классов | StringClassSerializer, MultipartClassSerializer, GsonClassSerializer |
| NanoContentSerializer | Сериализатор входного/выходного потока классов | TextContentSerializer |
| NanoContentEncoder | Кодер/декодер входного/выходного потока данных сервера | GZipContentEncoder |

Сервис NanoClassSerializer предназначен для преобразования входного потока сервера в один из указанных классов (например, класс Gson), и обратного преобразования. Сервис NanoContentSerializer преобразует выходной поток плагинов в байтовые последовательности, а также выполняет обратное преобразование.
Сервис NanoContentEncoder позволяет пребразовавывать байтовые последовательности входного/выходного потоков сервера в другие байтовые последовательности.

Для добавления собственных SPI-сервисов нужно реализовать соответствующий сервис, упаковать его в jar-архив, и указать этот архив в параметре --classpath при запуске приложения.

### Написание плагинов для сервера

Плагин для сервера пишется в стиле документа [JSR-311](https://jcp.org/en/jsr/detail?id=311). Для того, чтобы сервер "подхватил" его в работу, можно воспользоваться одним из трех методов:
- упаковать плагин в архив, реализовать для него SPI-сервис на базе класса chav1961.nanohttp.server.interfaces.NanoSPIPlugin, и включить собранный архив в
параметр --classpath Java-машины при запуске приложения
- упаковать плагин в архив, реализовать для него SPI-сервис на базе класса chav1961.nanohttp.server.interfaces.NanoSPIPlugin, и выложить собранный архив в
директорию, заданную параметром **-appDir** при запуске сервера (плагин будет "подхвачен" автоматически)
- упаковать плагин в архив, выложить его в директорию, заданную параметром **-appDir** при запуске сервера, и ввести для приложения команду с консоли:

> deploy \[квалифицированное_имя_класса] to \[путь_от_корня_сайта]

Следует иметь в виду, что, в отличие от большинства реализаций JSR-311, в данном приложении для обработки всех запросов по тому или иному адресу используется *единстваенный* экземпляр плагина (так же, как и "классический" сервлет). По этой причине, поля инстанции класса плагина, по факту, можно считать static-полями.

### Управление сервером через JMX-соединение

Управление сервером через JMX-соединение может выполняться любым JMX-клиентом (например, стандартной консолью Java **jconsole**). Имя JMX-соединения:

> chav1961.nanohttp:type=basic,name=server

JMX-соединение поддерживает следующие типы операций:

| Имя метода | Назначение |
|------|-------|
| start() | Запускает сервер в работу |
| suspend() | Временно приостанавливает прием запросов сервером |
| resume() | Возобновляет ранее присотановленный прием запросов сервером |
| stop() | Останавливает работу сервера |
| terminateAndExit() | Останавливает сервер и завершает работу приложения |

Кроме того, JMX-соединение поддерживает (в режиме только-чтение) два атрибута сервера:

| Имя атрибута | Тип | Назначение |
|------|-------|-------|
| isStarted | boolean | Сервер в данный момент времени запущен. |
| isSuspended | boolean | Сервер в данный момент времени не принимает запросы. |

