# micro4j

jax-rs web framework

[![](https://jitpack.io/v/micro4j/micro4j.svg)](https://jitpack.io/#micro4j/micro4j) [ ![Codeship Status for micro4j/micro4j](https://codeship.com/projects/142e4c10-f9df-0133-a3d7-124ad23604b3/status?branch=master)](https://codeship.com/projects/151397) [![Coverage Status](https://coveralls.io/repos/github/micro4j/micro4j/badge.svg?branch=master)](https://coveralls.io/github/micro4j/micro4j?branch=master) [![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/micro4j/micro4j/blob/master/LICENSE.md) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/6ecf755f7f204717aa9f988d20f772da)](https://www.codacy.com/app/admin_20/micro4j?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=micro4j/micro4j&amp;utm_campaign=Badge_Grade)

## Installation

### Maven

Add the following to your POM's `<repositories>` tag:

```xml
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>
```

then add the following dependency:

```xml
<dependency>
  <groupId>com.micro4j.micro4j</groupId>
  <artifactId>micro4j-mvc</artifactId>
  <version>1.3.0</version>
</dependency>
```

### Gradle

Add the repository:

```groovy
repositories {
  maven {
    url "https://jitpack.io"
  }
}
```

and then add the dependency:

```groovy
dependencies {
  compile 'com.micro4j.micro4j:micro4j-mvc:1.3.0'
}
```

