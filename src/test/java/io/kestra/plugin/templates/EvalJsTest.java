package io.kestra.plugin.templates;

import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.graalvm.EvalJs;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

@MicronautTest
class EvalJsTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void runValue() throws Exception {
        RunContext runContext = runContextFactory.of();

        EvalJs task = EvalJs.builder()
            .script(
                """
                var BigDecimal = Java.type('java.math.BigDecimal');
                BigDecimal.valueOf(10).pow(20)"""
            )
            .build();

        var runOutput = task.run(runContext);
        System.out.println(runOutput);
    }

    @Test
    void runMember() throws Exception {
        RunContext runContext = runContextFactory.of();

        EvalJs task = EvalJs.builder()
            .script(
                "({ id   : 42, text : '42', arr  : [1,42,3] })"
            )
            .outputs(List.of("id", "text"))
            .build();

        var runOutput = task.run(runContext);
        System.out.println(runOutput);
    }

    @Test
    void runFunction() throws Exception {
        RunContext runContext = runContextFactory.of();

        EvalJs task = EvalJs.builder()
            .id("unit-test")
            .type(EvalJs.class.getName())
            .script(
                """
                    (function() {
                    var Counter = Java.type('io.kestra.core.models.executions.metrics.Counter');
                    var File = Java.type('java.io.File');
                    var FileOutputStream = Java.type('java.io.FileOutputStream');
                    
                    runContext.metric(Counter.of('total', 666, 'name', 'bla'));
                    
                    map = {'test': 'here'};
                    var tempFile = runContext.workingDir().createTempFile().toFile();
                    var output = new FileOutputStream(tempFile);
                    output.write(256);
                    
                    out = runContext.storage().putFile(tempFile);
                    return {"map": map, "out": out};
                    })"""
            )
            .outputs(List.of("map", "out"))
            .build();

        var runOutput = task.run(runContext);
        System.out.println(runOutput);
    }
}