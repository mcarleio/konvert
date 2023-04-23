---
layout: default
title: Modules
nav_order: 4
---

Konvert is split into these modules:

* `konvert-api`

  This module contains only the annotations you will be using to annotate your types for Konvert to process during compilement.


* `konvert-converter-api`

  This module can be used to build your own `TypeConverter`s to be used by Konvert during compilement. As Konvert is using SPI, you can
  build your own library and just include it into KSP classpath!


* `konvert-converter`

  Konvert already comes with a lot of default `TypeConverter`s, which are all packed into this module.


* `konvert-processor`

  This is the module which contains the actual KSP SymbolProcessor implementation with all its logic.


* `konvert`

  This is an empty library which simply has dependencies on `konvert-processor` and `konvert-converter`
