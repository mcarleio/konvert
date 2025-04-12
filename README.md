# Konvert

[![Maven Central][maven-image]][maven-url]
[![License][license-image]](LICENSE)
[![Code Coverage][codecov-image]][codecov-url]

This is a kotlin compiler plugin (using [KSP](https://github.com/google/ksp)) to generate mapping code from one class to another.

> This README provides a basic overview, for more details have a look at the [documentation](https://mcarleio.github.io/konvert).

## Usage

### Gradle

To use `Konvert` with Gradle, you have to do the following steps:

1. Add `konvert-api` as a dependency:
   ```kotlin
   dependencies {
      implementation("io.mcarle:konvert-api:$konvertVersion")
   }
   ```

2. Add the KSP plugin matching your Kotlin version:
   ```kotlin
   plugins {
       id("com.google.devtools.ksp").version("2.0.21-1.0.27")
   }
   ```

3. Add `konvert` as a `ksp` dependency:
   ```kotlin
   dependencies {
      ksp("io.mcarle:konvert:$konvertVersion")
   }
   ```

### Maven

To use `Konvert` with Maven, you have to do the following steps:

1. Add `konvert-api` as a dependency:
   ```xml
   <dependency>
       <groupId>io.mcarle</groupId>
       <artifactId>konvert-api</artifactId>
       <version>${konvert.version}</version>
   </dependency>
   ```

2. Configure the `kotlin-maven-plugin` to use `Konvert`:
   ```xml
   <plugin>
       <groupId>org.jetbrains.kotlin</groupId>
       <artifactId>kotlin-maven-plugin</artifactId>
       <configuration>
           <jvmTarget>17</jvmTarget>
           <compilerPlugins>
               <plugin>ksp</plugin>
           </compilerPlugins>
       </configuration>
       <dependencies>
           <dependency>
               <groupId>com.dyescape</groupId>
               <artifactId>kotlin-maven-symbol-processing</artifactId>
               <version>1.6</version>
           </dependency>
           <dependency>
               <groupId>io.mcarle</groupId>
               <artifactId>konvert</artifactId>
               <version>${konvert.version}</version>
           </dependency>
       </dependencies>
   </plugin>
   ```

### Code

For a simple example project have a look into the [example directory](example).

There are three different ways to use `Konvert`:

1. Using `@KonvertTo`:
   ```kotlin
   @KonvertTo(PersonDto::class)
   class Person(val firstName: String, val lastName: String)
   data class PersonDto(val firstName: String, val lastName: String)
   ```
   This will generate the following extension function
   ```kotlin
   fun Person.toPersonDto(): PersonDto =
      PersonDto(firstName = firstName, lastName = lastName)
   ```

2. Using `@KonvertFrom` (especially useful, if you cannot change the code of the source class)
   ```kotlin
   class Person(val firstName: String, val lastName: String) {
      @KonvertFrom(PersonDto::class)
      companion object
   }
   data class PersonDto(val firstName: String, val lastName: String)
   ```
   This will generate the following extension function
   ```kotlin
   fun Person.Companion.fromPersonDto(personDto: PersonDto): Person =
      Person(firstName = personDto.firstName, lastName = personDto.lastName)
   ```

3. Using `@Konverter`:
   ```kotlin
   class Person(val firstName: String, val lastName: String)
   data class PersonDto(val firstName: String, val lastName: String)

   @Konverter
   interface PersonMapper {
      fun toDto(person: Person): PersonDto
   }
   ```
   This will generate the following object
   ```kotlin
   object PersonMapperImpl: PersonMapper {
      override fun toDto(person: Person): PersonDto
         = PersonDto(firstName = person.firstName, lastName = person.lastName)
   }
   ```

### Type mappings

For simple type mappings, like from `Instant` to `Date`, there already is a type converter provided with `Konvert`:

```kotlin
@KonvertTo(PersonDto::class)
class Person(val name: String, val birthday: Instant)
data class PersonDto(val name: String, val birthday: Date)
```

This will generate the following extension function

```kotlin
fun Person.toPersonDto(): PersonDto = PersonDto(
   name = name,
   birthday = birthday.let { java.util.Date.from(it) }
)
```

Have a look at the [documentation](https://mcarleio.github.io/konvert/typeconverter/) for a list of provided type converters.

🛈: You can also create your own type converter library by
implementing [TypeConverter](converter-api/src/main/kotlin/io/mcarle/konvert/converter/api/TypeConverter.kt) and register it
using [SPI](https://en.wikipedia.org/wiki/Service_provider_interface).

### Fine tuning

Most of the time, the source and target classes might not have the same property names and types.
You can configure specific mappings and rename the generated extension function like this:

```kotlin
@KonvertTo(
   PersonDto::class,
   mappings = [
      Mapping(source = "firstName", target = "givenName"),
      Mapping(source = "lastName", target = "familyName")
   ],
   mapFunctionName = "asDto"
)
class Person(val firstName: String, val lastName: String)
data class PersonDto(val givenName: String, val familyName: String)
```

This will generate the following extension function

```kotlin
fun Person.asDto(): PersonDto = PersonDto(
   givenName = firstName,
   familyName = lastName
)
```

For further functionality, have a look into
the [documentation](https://mcarleio.github.io/konvert/)
the KDocs of the [annotations](annotations/src/main/kotlin/io/mcarle/konvert/api),
the [example project](example/src/main/kotlin/io/mcarle/konvert/example)
or the [tests](processor/src/test/kotlin/io/mcarle/konvert/processor).

## Further information

* `Konvert` is primarily compiled and tested with JDK >=17. It should also work with anything below JDK 17, but is not guaranteed to.
* `Konvert` is able to convert classes from and to classes written in Java (and probably also in other JVM languages).

## Building

### Gradle

To build the project, simply run

> gradle build

#### Run all tests

By default, only a subset of available tests are executed, which should verify most of `Konvert`'s functionality.
To run all tests, append the property `runAllTests`, e.g.:

> gradle test -PrunAllTests

### Documentation

To serve the Jekyll site locally, simply run the following command inside `docs`:

> docker run --rm -it -v "$PWD":/site -p 4000:4000 bretfisher/jekyll-serve

### CI

GitHub Actions are used to:

* [build and test](.github/workflows/build.yml)
* [release and publish](.github/workflows/release.yml) to a Maven repository
* [generate documentation](.github/workflows/pages.yml) and deploy it to GitHub Pages.

## Changelog

The [changelog](CHANGELOG.md) contains all notable changes.

## License

    Copyright 2023 Marcel Carlé

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[maven-image]: https://img.shields.io/maven-central/v/io.mcarle/konvert.svg
[maven-url]: https://central.sonatype.com/artifact/io.mcarle/konvert/
[license-image]: https://img.shields.io/github/license/mcarleio/konvert.svg
[codecov-image]: https://img.shields.io/codecov/c/github/mcarleio/konvert.svg
[codecov-url]: https://codecov.io/gh/mcarleio/konvert
