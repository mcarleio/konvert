# Konvert

This is a kotlin compiler plugin (using [KSP](https://github.com/google/ksp)) to generate mapping code from one class to another.

## Usage

To use `Konvert` with Gradle have a look [here](example/build.gradle.kts) and with Maven have a look [here](example/pom.xml)

There are three different ways to use `Konvert`:

1. Using `@KonvertTo`:
   ```kotlin
   @KonvertTo(PersonDto::class)
   data class Person(val firstName: String, val lastName: String)
   data class PersonDto(val firstName: String, val lastName: String)
   ```
   This will generate the following extension function
   ```kotlin
   fun Person.toPersonDto(): PersonDto =
      PersonDto(firstName = firstName, lastName = lastName)
   ```

2. Using `@KonvertFrom` (especially useful, if you cannot change the code of the source class)
   ```kotlin
   data class Person(val firstName: String, val lastName: String) {
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

3. Using `@Konverter` and `@Konvert`:
   ```kotlin
   data class Person(val firstName: String, val lastName: String)
   data class PersonDto(val firstName: String, val lastName: String)

   @Konverter
   interface PersonMapper {
      @Konvert
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
data class Person(val name: String, val birthday: Instant)
data class PersonDto(val name: String, val birthday: Date)
```
This will generate the following extension function
```kotlin
fun Person.toPersonDto(): PersonDto = PersonDto(
   name = name,
   birthday = birthday.let { java.util.Date.from(it) }
)
```

Have a look [here](converter/src/main/kotlin/io/mcarle/konvert/converter) for a list of provided type converters.

Hint: You can also create your own type converter library by implementing [TypeConverter](converter-api/src/main/kotlin/io/mcarle/konvert/converter/api/TypeConverter.kt) and register it using [SPI](https://en.wikipedia.org/wiki/Service_provider_interface).


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
data class Person(val firstName: String, val lastName: String)
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
the KDocs of the [api](api/src/main/kotlin/io/mcarle/konvert/api),
the [example project](example/src/main/kotlin/io/mcarle/konvert/example)
or the [tests](processor/src/test/kotlin/io/mcarle/konvert/processor).

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
