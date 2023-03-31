# Usage of Konvert

In the following a minimal step-by-step guide is shown, how to use `Konvert` with different build tools.

See the example project with [Gradle](./example/build.gradle.kts) or [Maven](./example/pom.xml).

> See [here](https://central.sonatype.com/artifact/io.mcarle/konvert/1.0.0/versions) for the current version.

## Build-Tools
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
       id("com.google.devtools.ksp").version("1.8.10-1.0.9")
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
               <version>1.4</version>
           </dependency>
           <dependency>
               <groupId>io.mcarle</groupId>
               <artifactId>konvert</artifactId>
               <version>${konvert.version}</version>
           </dependency>
       </dependencies>
   </plugin>
   ```

   > At the time of writing, `kotlin-maven-symbol-processing` does not support Kotlin >=1.8.
   Please verify the details regarding the plugin by referring to the corresponding project's [GitHub page](https://github.com/Dyescape/kotlin-maven-symbol-processing).

## Further information

* `Konvert` is primarily compiled and tested with JDK >=17. It is not guaranteed to work with anything below JDK 17.
