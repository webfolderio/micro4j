micro4j - JAX-RS MVC Framework
================================

micro4j is MVC framework for JAX-RS.

[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/micro4j/micro4j/blob/master/LICENSE.md)
[![circleci](https://img.shields.io/circleci/project/github/webfolderio/micro4j.svg?label=Build)](https://circleci.com/gh/webfolderio/micro4j)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/11300700026043b79cbef2a4f1b8f26d)](https://www.codacy.com/app/WebFolder/micro4j?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=webfolderio/micro4j&amp;utm_campaign=Badge_Grade)

Features
--------
 * Built-in integration with [PJAX](https://github.com/defunkt/jquery-pjax).
 * [CsrfFeature](https://github.com/webfolderio/micro4j/blob/master/micro4j-mvc/src/main/java/io/webfolder/micro4j/mvc/csrf/CsrfFeature.java) help prevent Cross-Site Request Forgery (CSRF) attacks.
 * Supports mustache template engine with [jmustache](https://github.com/samskivert/jmustache).
 * Lightweight architecture, micro4j does not require external dependency except [RestEasy Framework](https://github.com/resteasy/resteasy).
 * Built-in support to prevent SQL injection attacks.

Stability
---------
This library is suitable for use in production systems.
 

Integration with Maven
----------------------

To use the official release of micro4j, please use the following snippet in your `pom.xml` file.

Add the following to your POM's `<dependencies>` tag:

```xml
<dependency>
    <groupId>io.webfolder</groupId>
    <artifactId>micro4j-mvc</artifactId>
    <version>1.7.4</version>
</dependency>
```
 
Building micro4j
----------------
`mvn install`

How it is tested
----------------
micro4j is regularly built and tested on [circleci](https://circleci.com/gh/webfolderio/micro4j).