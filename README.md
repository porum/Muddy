# Muddy

[![license](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![muddy-gradle-plugin](https://img.shields.io/badge/muddy--gradle--plugin--bytex-2.0.1-brightgreen.svg)](https://search.maven.org/artifact/io.github.porum/muddy-gradle-plugin/2.0.1/jar)

Muddy is a android gradle plugin that can obfuscate the const string in APK.

## Usage

1. include the following `classpath` in your **top level `build.gradle`** file:

   ```groovy
   buildscript {
       repositories {
           mavenCentral()
       }
       dependencies {
           classpath "io.github.porum:muddy-gradle-plugin:$VERSION"
       }
   }
   ```

2. add the following code in your **application module's `build.gradle`** file:

   ```groovy
   apply plugin: "muddy-plugin"
   
   dependencies {
       implementation "io.github.porum:muddy:$VERSION"
   }
   ```

## Thanks

[TrueLicense](https://github.com/christian-schlichtherle/truelicense)

## License

```
Copyright 2021 porum.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

