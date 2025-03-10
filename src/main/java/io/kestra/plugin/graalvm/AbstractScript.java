package io.kestra.plugin.graalvm;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
abstract class AbstractScript extends Task {
    @Schema(
        title = "A full script"
    )
    @PluginProperty(dynamic = true)
    protected String script;

    private volatile transient Engine engine;

    protected Engine getEngine() {
        // double-checked locking idiom
        Engine engine = this.engine;
        if (engine == null) {
            synchronized (this) {
                engine = this.engine;
                if (engine == null) {
                    engine = this.engine = Engine.create();
                }
            }
        }
        return engine;
    }


    protected Source generateSource(String languageId, RunContext runContext) throws IllegalVariableEvaluationException {
        var rendered = runContext.render(this.script);
        return Source.create(languageId, rendered);
    }
}