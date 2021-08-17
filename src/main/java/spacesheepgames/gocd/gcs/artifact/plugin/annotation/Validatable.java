/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spacesheepgames.gocd.gcs.artifact.plugin.annotation;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import spacesheepgames.gocd.gcs.artifact.plugin.utils.Util;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface Validatable {

    ValidationResult validate();

    default String toJSON() {
        return Util.GSON.toJson(this);
    }

    default Map<String, String> toProperties() {
        return Util.GSON.fromJson(toJSON(), new TypeToken<Map<String, String>>() {
        }.getType());
    }

    default List<ValidationError> validateAllFieldsAsRequired() {
        return validateAllFieldsAsRequired(Collections.emptySet());
    }

    default List<ValidationError> validateAllFieldsAsRequired(Set<String> excluding) {
        return toProperties().entrySet().stream()
                .filter(entry -> !excluding.contains(entry.getKey()))
                .filter(entry -> StringUtils.isBlank(entry.getValue()))
                .map(entry -> new ValidationError(entry.getKey(), entry.getKey() + " must not be blank."))
                .collect(Collectors.toList());
    }

    default List<ValidationError> validateAllOrNoneRequired(Set<String> including) {

        boolean allBlank = true, noneBlank = true;

        for (String propertyName : including) {
            String value = toProperties().get(propertyName);

            allBlank &= StringUtils.isBlank(value);
            noneBlank &= StringUtils.isNotBlank(value);
        }

        if (allBlank || noneBlank) {
            return Collections.emptyList();
        } else {
            List<String> fieldsList = Lists.newArrayList(including);
            String fields = String.join(" and ", String.join(", ", fieldsList.subList(0, fieldsList.size() - 1)), fieldsList.get(fieldsList.size() - 1));

            String errorMessage = fields + " must be filled altogether, if required.";

            return including.stream()
                    .map(s -> new ValidationError(s, errorMessage))
                    .collect(Collectors.toList());
        }
    }
}
