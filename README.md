hale-cli
========

hale command line interface.
Lists available commands when run without arguments.


Build
-----

Build distribution archive:

```
./gradlew distZip
```

Build Debian package:

```
./gradlew buildDeb
```

Build docker image:

```
./gradlew dockerBuildImage
```


Run
---

You can run the application using Gradle. However, there is no possibility to easily provide arguments.

```
./gradlew run
```

Alternatively, you can run the start script of the built application.

```
hale --version
```

If using `./gradlew installDist`, the start script can be found in `./build/install/hale/bin/`.

JVM parameters can be provided to the start script with the `HALE_OPTS` environment variable.


Configuration
-------------


### Logging

The system properties `log.hale.level` and `log.root.level` can be set to control the default logging levels.

```
HALE_OPTS="-Dlog.hale.level=INFO -Dlog.root.level=WARN"
```


### Proxy connection

If you need to connect to the internet via a proxy server, you need to provide that information as system properties as well.

The following system properties can be provided to configure the proxy:

* `http.proxyHost` - the proxy host name or IP address
* `http.proxyPort` - the proxy port number
* `http.nonProxyHosts` - hosts for which the proxy should not be used, separated by | (optional)
* `http.proxyUser` - user name for authentication with the proxy (optional)
* `http.proxyPassword` - password for authentication with the proxy (optional)

Example:

```
HALE_OPTS="-Dhttp.proxyHost=webcache.example.com -Dhttp.proxyPort=8080 -Dhttp.nonProxyHosts='localhost|host.example.com'"
```


### Language

Some commands may produce different results based on your language.
By default the system language is used.
You can override the default locale settings via the following system properties:

* `user.language` - two letter code for the language (e.g. `de`)
* `user.country` - two letter code for the country (e.g. `DE`)
* `user.variant` - name of the variant, if applicable

Example:

```
HALE_OPTS="-Duser.country=DE -Duser.language=de"
```


Helpers
-------

Check which files were installed by the `.deb` package:

```
dpkg-query -L hale-cli
```
