/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.cmd;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.DynamicBpmnConstants;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.activiti.engine.task.Task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GetTaskVariableInstancesCmd implements Command<Map<String, VariableInstance>>, Serializable {

  private static final long serialVersionUID = 1L;
  protected String taskId;
  protected Collection<String> variableNames;
  protected boolean isLocal;
  protected String locale;
  protected boolean withLocalizationFallback;

  public GetTaskVariableInstancesCmd(String taskId, Collection<String> variableNames, boolean isLocal) {
    this.taskId = taskId;
    this.variableNames = variableNames;
    this.isLocal = isLocal;
  }

  public GetTaskVariableInstancesCmd(String taskId, Collection<String> variableNames, boolean isLocal, String locale, boolean withLocalizationFallback) {
    this.taskId = taskId;
    this.variableNames = variableNames;
    this.isLocal = isLocal;
    this.locale = locale;
    this.withLocalizationFallback = withLocalizationFallback;
  }

  public Map<String, VariableInstance> execute(CommandContext commandContext) {
    if (taskId == null) {
      throw new ActivitiIllegalArgumentException("taskId is null");
    }

    TaskEntity task = commandContext.getTaskEntityManager().findById(taskId);

    if (task == null) {
      throw new ActivitiObjectNotFoundException("task " + taskId + " doesn't exist", Task.class);
    }

    Map<String, VariableInstance> variables = null;
    if (variableNames == null) {

      if (isLocal) {
        variables = task.getVariableInstancesLocal();
      } else {
        variables = task.getVariableInstances();
      }

    } else {

      if (isLocal) {
        variables = task.getVariableInstancesLocal(variableNames, false);
      } else {
        variables = task.getVariableInstances(variableNames, false);
      }

    }

    if (variables != null && locale != null) {
      for (Entry<String, VariableInstance> entry : variables.entrySet()) {
        String variableName = entry.getKey();
        VariableInstance variableEntity = entry.getValue();

        String localizedName = null;
        String localizedDescription = null;

        ObjectNode languageNode = Context.getLocalizationElementProperties(locale, variableName, task.getProcessDefinitionId(), withLocalizationFallback);
        if (languageNode != null) {
          JsonNode nameNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_NAME);
          if (nameNode != null) {
            localizedName = nameNode.asText();
          }
          JsonNode descriptionNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_DESCRIPTION);
          if (descriptionNode != null) {
            localizedDescription = descriptionNode.asText();
          }
        }

        variableEntity.setLocalizedName(localizedName);
        variableEntity.setLocalizedDescription(localizedDescription);
      }
    }

    return variables;
  }
}
