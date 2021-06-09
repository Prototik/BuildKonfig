BuildKonfig
===

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.codingfeline.buildkonfig/buildkonfig-gradle-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.codingfeline.buildkonfig/buildkonfig-gradle-plugin)

BuildConfig for Kotlin Multiplatform Project.  
It currently supports embedding values from gradle file.

## Table Of Contents

- [Motivation](#motivation)
- [Usage](#usage)
    - [Requirements](#requirements)
    - [Gradle Configuration](#gradle-configuration)
    - [Product Flavor?](#product-flavor)
    - [Overwriting Values](#overwriting-values)
- [Supported Types](#supported-types)
- [Try out the sample](#try-out-the-sample)

<a name="motivation"/>

## Motivation

Passing values from Android/iOS or any other platform code should work, but it's a hassle.  
Setting up Android to read values from properties and add those into BuildConfig, and do the equivalent in iOS?  
Rather I'd like to do it once.


<a name="usage"/>

## Usage

<a name="requirements"/>

### Requirements

- Kotlin **1.4.0** or later
- Kotlin Multiplatform Project
- Gradle 6.5 or later

<a name="gradle-configuration"/>

### Gradle Configuration

#### Simple configuration

##### Groovy DSL

```gradle
buildScript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0'
        classpath 'com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:latest_version'
    }
}

apply plugin: 'org.jetbrains.kotlin.multiplatform'
apply plugin: 'com.codingfeline.buildkonfig'

kotlin {
    // your target config...
    android()
    iosX64('ios')
}

buildkonfig {
    packageName = 'com.example.app'
    // objectName = 'YourAwesomeConfig'
    // exposeObjectWithName = 'YourAwesomePublicConfig'

    defaultConfigs {
        buildConfigField 'STRING', 'name', 'value'
    }
}
```

##### Kotlin DSL

```kotlin
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0")
        classpath("com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:latest_version")
    }
}

plugins {
    kotlin("multiplatform")
    id("com.codingfeline.buildkonfig")
}

kotlin {
    // your target config...
    android()
    iosX64('ios')
}

buildkonfig {
    packageName = "com.example.app"
    // objectName = "YourAwesomeConfig"
    // exposeObjectWithName = "YourAwesomePublicConfig"

    defaultConfigs {
        buildConfigField(STRING, "name", "value")
    }
}
```

- `packageName` Set the package name where BuildKonfig is being placed. **Required**.
- `objectName` Set the name of the generated object. Defaults to `BuildKonfig`.
- `exposeObjectWithName` Set the name of the generated object, and make it public.
- `defaultConfigs` Set values which you want to have in common. **Required**.

To generate BuildKonfig files, run `generateBuildKonfig` task.  
This task will be automatically run upon execution of kotlin compile tasks.

Above configuration will generate following simple object.

```kotlin
// commonMain
package com.example.app

internal object BuildKonfig {
    val name: String = "value"
}
```

#### Configuring `target` dependent values

If you want to change value depending on your targets, you can use `targetConfigs` to define target-dependent values.

##### Groovy DSL

```gradle
buildScript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0'
        classpath 'com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:latest_version'
    }
}

apply plugin: 'org.jetbrains.kotlin.multiplatform'
apply plugin: 'com.codingfeline.buildkonfig'

kotlin {
    // your target config...
    android()
    iosX64('ios')
}

buildkonfig {
    packageName = 'com.example.app'
    
    // default config is required
    defaultConfigs {
        buildConfigField 'STRING', 'name', 'value'
        buildConfigNullableField 'STRING', 'nullableField', null
    }
    
    targetConfigs {
        // this name should be the same as target names you specified
        android {
            buildConfigField 'STRING', 'name2', 'value2'
            buildConfigNullableField 'STRING', 'nullableField', 'NonNull-value'
        }
        
        ios {
            buildConfigField 'STRING', 'name', 'valueForNative'
        }
    }
}
```

##### Kotlin DSL

```kotlin
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0")
        classpath("com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:latest_version")
    }
}

plugins {
    kotlin("multiplatform")
    id("com.codingfeline.buildkonfig")
}

kotlin {
    // your target config...
    android()
    iosX64('ios')
}

buildkonfig {
    packageName = "com.example.app"

    // default config is required
    defaultConfigs {
        buildConfigField(STRING, "name", "value")
    }

    targetConfigs {
        // names in create should be the same as target names you specified
        create("android") {
            buildConfigField(STRING, "name2", "value2")
            buildConfigNullableField(STRING, "nullableField", "NonNull-value")
        }

        create("ios") {
            buildConfigField(STRING, "name", "valueForNative")
        }
    }
}
```

- `packageName` Set the package name where BuildKonfig is being placed. **Required**.
- `objectName` Set the name of the generated object. Defaults to `BuildKonfig`.
- `exposeObjectWithName` Set the name of the generated object, and make it public.
- `defaultConfigs` Set values which you want to have in common. **Required**.
- `targetConfigs` Set target specific values as closure. You can overwrite values specified in `defaultConfigs`.
- `buildConfigField(String type, String name, String value)` Add new value or overwrite existing one.
- `buildConfigNullableField((String type, String name, String value)` Add new nullable value or overwrite existing one.

Above configuration will generate following codes.

```kotlin
// commonMain
package com.example.app

internal expect object BuildKonfig {
    val name: String
    val nullableField: String?
}
```

```kotlin
// androidMain
package com.example.app

internal actual object BuildKonfig {
    actual val name: String = "value"
    actual val nullableField: String? = "NonNull-value"
    val name2: String = "value2"
}
```

```kotlin
// iosMain
package com.example.app

internal actual object BuildKonfig {
    actual val name: String = "valueForNative"
    actual val nullableField: String? = null
}
```

#### Note about the hierarchical project structure

Kotlin 1.4.0 adds support for the hierarchical project structure, but BuildKonfig currently does not support this. You
can use the hierarchical project structure, but intermediate SourceSets can only see fields defined in `defaultConfigs`
block. See details and progress at [here](https://github.com/yshrsmz/BuildKonfig/issues/38).

<a name="product-flavor"/>

### Product Flavor?

Yes(sort of).  
Kotlin Multiplatform Project does not support product flavor. Kotlin/Native part of the project has release/debug
distinction, but it's not global.  
So to mimick product flavor capability of Android, we need to provide additional property in order to determine flavors.

Specify default flavor in your `gradle.properties`

```properties
# ROOT_DIR/gradle.properties
buildkonfig.flavor=dev
```

##### Groovy DSL

```gradle
// ./mpp_project/build.gradle

buildkonfig {
    packageName = 'com.example.app'
    
    // default config is required
    defaultConfigs {
        buildConfigField 'STRING', 'name', 'value'
    }
    // flavor is passed as a first argument of defaultConfigs 
    defaultConfigs("dev") {
        buildConfigField 'STRING', 'name', 'devValue'
    }
    
    targetConfigs {
        android {
            buildConfigField 'STRING', 'name2', 'value2'
        }
        
        ios {
            buildConfigField 'STRING', 'name', 'valueIos'
        }
    }
    // flavor is passed as a first argument of targetConfigs
    targetConfigs("dev") {
        ios {
            buildConfigField 'STRING', 'name', 'devValueIos'
        }
    }
}
```

##### Kotlin DSL

```kotlin
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.
import com.codingfeline.buildkonfig.gradle.TargetConfigDsl

buildkonfig {
    packageName = "com.example.app"

    // default config is required
    defaultConfigs {
        buildConfigField(STRING, "name", "value")
    }
    // flavor is passed as a first argument of defaultConfigs 
    defaultConfigs("dev") {
        buildConfigField(STRING, "name", "devValue")
    }

    targetConfigs(closureOf<NamedDomainObjectContainer<TargetConfigDsl>> {
        create("android") {
            buildConfigField(STRING, "name2", "value2")
        }

        create("ios") {
            buildConfigField(STRING, "name", "valueIos")
        }
    })
    // flavor is passed as a first argument of targetConfigs
    targetConfigs("dev", closureOf<NamedDomainObjectContainer<TargetConfigDsl>> {
        create("ios") {
            buildConfigField(STRING, "name", "devValueIos")
        }
    })
}
```

In a development phase you can change value in `gradle.properties` as you like.  
In CI environment, you can pass value via CLI `$ ./gradlew build -Pbuildkonfig.flavor=release`


<a name="overwriting-values"/>

### Overwriting Values

If you configure same field across multiple defaultConfigs and targetConfigs, flavored targetConfigs is the strongest.

Lefter the stronger.

```
Flavored TargetConfig > TargetConfig > Flavored DefaultConfig > DefaultConfig
```

<a name="supported-types"/>

## Supported Types

- String
- Int
- Long
- Float
- Boolean

<a name="try-out-the-sample"/>

## Try out the sample

Have a look at `./sample` directory.

```
# Publish the latest version of the plugin to mavenLocal()
$ ./gradlew publishToMavenLocal

# Try out the samples.
# BuildKonfig will be generated in ./sample/build/buildkonfig
$ ./gradlew -p sample generateBuildKonfig
```
