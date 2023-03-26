# KMap

This is a kotlin compiler plugin (using [KSP](https://github.com/google/ksp)) to generate mapping code from one class to another.

## Usage

To use `KMap` with Gradle have a look [here](example/build.gradle.kts) and with Maven have a look [here](example/pom.xml)

There are three different ways to use `KMap`:

1. Using `@KMapTo`:
   ```kotlin
   @KMapTo(PersonDto::class)
   data class Person(val firstName: String, val lastName: String)
   data class PersonDto(val firstName: String, val lastName: String)
   ```
   This will generate the following extension function
   ```kotlin
   fun Person.mapToPersonDto(): PersonDto =
      PersonDto(firstName = firstName, lastName = lastName)
   ```

2. Using `@KMapFrom` (especially useful, if you cannot change the code of the source class)
   ```kotlin
   data class Person(val firstName: String, val lastName: String) {
      @KMapFrom(PersonDto::class)
      companion object
   }
   data class PersonDto(val firstName: String, val lastName: String)
   ```
   This will generate the following extension function
   ```kotlin
   fun Person.Companion.fromPersonDto(personDto: PersonDto): Person =
      Person(firstName = personDto.firstName, lastName = personDto.lastName)
   ```

3. Using `@KMapper` and `@KMapping`:
   ```kotlin
   data class Person(val firstName: String, val lastName: String)
   data class PersonDto(val firstName: String, val lastName: String)

   @KMapper
   interface PersonMapper {
      @KMapping
      fun mapToDto(person: Person): PersonDto
   }
   ```
   This will generate the following object
   ```kotlin
   object PersonMapperImpl: PersonMapper {
      override fun mapToDto(person: Person): PersonDto
         = PersonDto(firstName = person.firstName, lastName = person.lastName)
   }
   ```

### Fine tuning

Most of the time, the source and target classes might not have the same property names and types.
You can configure specific mappings using `KMap` in the annotation:

```kotlin
@KMapTo(
   PersonDto::class, mappings = [
      KMap(source = "firstName", target = "givenName"),
      KMap(source = "lastName", target = "familyName")
   ]
)
data class Person(val firstName: String, val lastName: String, val birthday: Instant)
data class PersonDto(val givenName: String, val familyName: String, val birthday: Date)
```

This will generate the following extension function

```kotlin
fun Person.mapToPersonDto(): PersonDto = PersonDto(
   givenName = firstName,
   familyName = lastName,
   birthday = birthday.let { java.util.Date.from(it) }
)
```

For further functionality, have a look into
the KDocs of the [annotations](api/src/main/kotlin/io/mcarle/kmap/api/annotation),
the [example project](example/src/main/kotlin/io/mcarle/kmap/example)
or the [tests](processor/src/test/kotlin/io/mcarle/kmap/processor).

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
