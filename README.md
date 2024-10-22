## Nano HTTP Server

### Общее описание

Данный репозитарий содержит обертку над небезызвестным Java-классом [com.sun.net.httpserver.HttpServer](https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html), который можно исопльзовать в качесвте простого WEB-сервера. Помимо обычной функциональности HTTP-сервера, обертка поддерживает несколько дополнительных возможности:

- дерево статических страниц сайта (на основе интерфейса файловой системы моей библиотеки [Pure Library](https://github.com/chav1961/purelib) )
- простейший OpenAPI интерфейс, на основе документа [JSR-311](https://jcp.org/en/jsr/detail?id=311), позволяющий писать для сервера легковесные плагины
- JMX-интерфейс для управления сервером

Параметры запуска проекта следующие:

> java -jar nanohttp.jar <стандартные аргументы> \[-startPipe] \[-join PART] \[-append URI URI URI ...]

ss

| ddddd | ddddd | ddddd |
|------|-------|-------|
|nanoservicePort | | |
|nanoserviceRoot | | |
|nanoserviceLocalhostOnly | true | |
|nanoserviceExecutorPoolSize | 10 | |
|nanoserviceDisableLoopback | true | |
|nanoserviceTemporaryCacheSize | | |
|nanoserviceCreolePrologueURI | | |
|nanoserviceCreoleEpilogueURI | | |
|nanoserviceUseSSL | TLS | |
|nanoserviceUseKeyStore | | |
|nanoserviceSSLKeyStore | | |
|nanoserviceSSLKeyStoreType | | |
|nanoserviceSSLKeyStorePasswd | | |
|nanoserviceUseTrustStore | SunX509 | |
|nanoserviceSSLTrustStore | | |
|nanoserviceSSLTrustStoreType | | |
|nanoserviceSSLTrustStorePasswd | | |

s
