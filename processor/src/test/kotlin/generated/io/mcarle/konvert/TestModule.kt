package generated.io.mcarle.konvert

import io.mcarle.konvert.api.GeneratedKonvertModule

@GeneratedKonvertModule(
    konverterFQN = [
        "thisLineShouldBeIgnoredAsNoFunctionExistsWithThisName",
        "io.mcarle.konvert.processor.SomeTestMapperImpl.toSomeOtherTestClass",
        "io.mcarle.konvert.processor.SomeTestMapperImpl.toSomeOtherTestClasses",
        "io.mcarle.konvert.processor.SomeTestMapperImpl.fromSomeOtherTestClass",
        "io.mcarle.konvert.processor.SomeSecondTestMapperImpl.fromSomeOtherTestClasses"
    ],
    konvertToFQN = ["io.mcarle.konvert.processor.toSomeOtherTestClass"],
    konvertFromFQN = ["io.mcarle.konvert.processor.fromSomeTestClass"],
)
interface TestModule
