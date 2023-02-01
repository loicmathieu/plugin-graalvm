package io.kestra.plugin.templates;

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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractScript extends Task {
    @Schema(
        title = "A full script"
    )
    @PluginProperty(dynamic = true)
    protected String script;

    protected String generateScript(RunContext runContext) throws IllegalVariableEvaluationException {
        return runContext.render(this.script);
    }
}