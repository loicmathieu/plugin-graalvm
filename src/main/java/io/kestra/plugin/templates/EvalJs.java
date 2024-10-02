package io.kestra.plugin.templates;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Execute a  JavaScript script using the GraalVM scripting engine."
)
@Plugin(
    examples = {
        @Example(
            code = """
                outputs:
                  - out
                  - map
                script: |
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
    }
)
public class EvalJs extends Eval {
    @Override
    public Eval.Output run(RunContext runContext) throws Exception {
        return this.run(runContext, "js");
    }
}
