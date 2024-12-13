package io.kestra.plugin.graalvm;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.graalvm.proxy.RunContextProxy;
import io.micronaut.context.ApplicationContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.slf4j.Logger;

import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractEval extends AbstractScript implements RunnableTask<AbstractEval.Output> {
    @Schema(
        title = "A List of outputs variables that will be usable in outputs."
    )
    @PluginProperty
    protected List<String> outputs;

    protected AbstractEval.Output run(RunContext runContext, String languageId) throws Exception {

        try (Context context = Context.newBuilder()
                .engine(getEngine())
                .allowIO(IOAccess.ALL)
                .allowHostAccess(HostAccess
                        .newBuilder(HostAccess.EXPLICIT)
                        .allowArrayAccess(true).allowListAccess(true).allowBufferAccess(true).allowIterableAccess(true).allowIteratorAccess(true).allowMapAccess(true).allowPublicAccess(true)
                        .build()
                )
                .allowHostClassLoading(true)
                .allowHostClassLookup(name -> name.startsWith("java."))
                .logHandler(System.out)
                .build()) {
            var bindings = context.getBindings(languageId);
            // add all common vars to bindings in case of concurrency
            runContext.getVariables().forEach((key, value) -> bindings.putMember(key, value));
            bindings.putMember("runContext", new RunContextProxy(runContext));
            bindings.putMember("logger", runContext.logger());

            var source = generateSource(languageId, runContext);
            var result = context.eval(source);

            Output.OutputBuilder builder = Output.builder();
            if (result.canExecute()) {
                var results = result.execute();
                if (results.hasMembers() && outputs != null && !outputs.isEmpty()) {
                    builder.outputs(gatherOutputs(results));
                }
            }
            else if (result.isHostObject()){
                builder.result(result.asHostObject());
            }
            else if (result.hasMembers() && outputs != null && !outputs.isEmpty()) {
                builder.outputs(gatherOutputs(result));
            }

            return builder
                .build();
        }
    }

    private Map<String, Object> gatherOutputs(Value value) {
        Map<String, Object> outputs = new HashMap<>();
        this.outputs
            .forEach(s -> outputs.put(s, as(value.getMember(s))));

        return outputs;
    }

    private Object as(Value member) {
        if (member == null) {
            return null;
        }
        if(member.isString()) {
            return member.asString();
        }
        if(member.isNumber() && member.fitsInInt()) {
            return member.asInt();
        }
        if(member.isNumber() && member.fitsInLong()) {
            return member.asLong();
        }
        if(member.isNumber() && member.fitsInFloat()) {
            return member.asFloat();
        }
        if(member.isNumber() && member.fitsInDouble()) {
            return member.asDouble();
        }
        if(member.isProxyObject()) {
            return member.asProxyObject();
        }
        if(member.isHostObject()) {
            return member.asHostObject();
        }
        if(member.hasMembers()) {
            // this should be a map
            Map<String, Object> value = new HashMap<>();
            member.getMemberKeys().forEach(key -> value.put(key, as(member.getMember(key))));
            return value;
        }

        // do our best to use a known type, this will crash with a ClassCastException if the type is not transformable
        return member.as(Object.class);
    }

    @Builder
    @Getter
    @ToString
    public static class Output implements io.kestra.core.models.tasks.Output {
        private Object result;

        @Schema(
            title = "The captured outputs as declared on task property."
        )
        private final Map<String, Object> outputs;
    }

}