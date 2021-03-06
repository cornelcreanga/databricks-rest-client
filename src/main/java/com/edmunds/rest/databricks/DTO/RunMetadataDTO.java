/*
 * Copyright 2018 Edmunds.com, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.edmunds.rest.databricks.DTO;

import com.edmunds.rest.databricks.DTO.jobs.NotebookOutputDTO;
import com.edmunds.rest.databricks.DTO.jobs.RunDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Data;


/**
 * DTO to hold RunDTO and NotebookOutputDTO values together to get complete run metadata.
 */
@Data
public class RunMetadataDTO implements Serializable {

  @JsonProperty("metadata")
  private RunDTO run;
  @JsonProperty("notebook_output")
  private NotebookOutputDTO notebookOutput;

}
