package diogomrol.gocd.s3.artifact.plugin.model;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class EnvironmentVariableResolver {

    private static final Pattern ENVIRONMENT_VARIABLE_PATTERN = Pattern.compile("\\$\\{(.*)\\}");
    private String property;
    private String propertyName;

    public EnvironmentVariableResolver(String property, String propertyName) {
        this.property = property;
        this.propertyName = propertyName;
        Map<String,String> map = new HashMap<>();
        map.put("GO_ARTIFACT_LOCATOR", "${GO_PIPELINE_NAME}/${GO_PIPELINE_COUNTER}/${GO_STAGE_NAME}/${GO_STAGE_COUNTER}/${GO_JOB_NAME}");
        this.property = StrSubstitutor.replace(property, map);
    }

    public String resolve(Map<String, String> environmentVariables) throws UnresolvedPropertyException {
        String evaluatedProperty = StrSubstitutor.replace(property, environmentVariables);
        if(ENVIRONMENT_VARIABLE_PATTERN.matcher(evaluatedProperty).find()) {
            throw new UnresolvedPropertyException(evaluatedProperty, propertyName);
        }
        return evaluatedProperty;
    }
}
