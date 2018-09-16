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

package com.edmunds.rest.databricks.service;

import com.edmunds.rest.databricks.DTO.JobDTO;
import com.edmunds.rest.databricks.DTO.JobSettingsDTO;
import com.edmunds.rest.databricks.DTO.JobsDTO;
import com.edmunds.rest.databricks.DTO.NotebookTaskDTO;
import com.edmunds.rest.databricks.DTO.RunDTO;
import com.edmunds.rest.databricks.DTO.RunLifeCycleStateDTO;
import com.edmunds.rest.databricks.DTO.RunsDTO;
import com.edmunds.rest.databricks.DatabricksRestException;
import com.edmunds.rest.databricks.DatabricksServiceFactory;
import com.edmunds.rest.databricks.TestUtil;
import com.edmunds.rest.databricks.fixtures.DatabricksFixtures;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.awaitility.Awaitility.await;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Created by shong on 7/31/16.
 */
public class JobServiceTest {
    private JobService service;
    private long jobId;
    private Long runId = null;
    String defaultClusterId;
    String jobName = "JobServiceTest_setUpOnce_test_job";
    String multiJobName = "JobServiceTest_multipleJobs";
    private long multiJobId[];

    @BeforeClass
    public void setUpOnce() throws IOException, DatabricksRestException {
        DatabricksServiceFactory factory = DatabricksFixtures.getDatabricksServiceFactory();
        service = factory.getJobService();

        //Delete any existing jobs
        deleteAllJobs(jobName);
        deleteAllJobs(multiJobName);

        NotebookTaskDTO notebook_task = new NotebookTaskDTO();
        notebook_task.setNotebookPath("/Users/dwhrestapi@edmunds.com/test_notebook");

        defaultClusterId = TestUtil.getDefaultClusterId(factory.getClusterService());
        JobSettingsDTO jobSettingsDTO = new JobSettingsDTO();
        jobSettingsDTO.setName(jobName);
        jobSettingsDTO.setExistingClusterId(defaultClusterId);
        jobSettingsDTO.setNotebookTask(notebook_task);

        jobId = service.createJob(jobSettingsDTO);

        runId = service.runJobNow(jobId).getRunId();

        await().atMost(1, MINUTES).until(TestUtil.runLifeCycleStateHasChangedTo(RunLifeCycleStateDTO.TERMINATED,
            runId, service));
    }

    @AfterClass(alwaysRun = true)
    public void tearDownOnce() throws IOException, DatabricksRestException {
        deleteAllJobs(jobName);
        deleteAllJobs(multiJobName);
    }

    private void deleteAllJobs(String jobName) throws IOException, DatabricksRestException {
        for (JobDTO jobDTO : service.getJobsByName(jobName)) {
            System.out.println("Deleting: " + jobDTO.getJobId());
            service.deleteJob(jobDTO.getJobId());
        }
    }

    @Test
    public void testSetUpOnce() throws IOException, DatabricksRestException {
        assertTrue(isJobIdValid(jobId));
    }

    @Test
    public void createMultiJobs() throws IOException, DatabricksRestException {
        JobSettingsDTO jobSettingsDTO = new JobSettingsDTO();
        jobSettingsDTO.setName(multiJobName);
        jobSettingsDTO.setExistingClusterId(defaultClusterId);
        NotebookTaskDTO notebook_task = new NotebookTaskDTO();
        notebook_task.setNotebookPath("/Users/dwhrestapi@edmunds.com/test_notebook");
        jobSettingsDTO.setNotebookTask(notebook_task);
        long jobIdOne = service.createJob(jobSettingsDTO);
        long jobIdTwo = service.createJob(jobSettingsDTO);
        multiJobId = new long[]{jobIdOne, jobIdTwo};

        assertEquals(service.getJobsByName(multiJobName).size(), 2);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void createAndDeleteJobFromJobSettingsDTO() throws IOException, DatabricksRestException {
        NotebookTaskDTO notebook_task = new NotebookTaskDTO();
        notebook_task.setNotebookPath("/fake_notebook_path");

        JobDTO originalJob = service.getJob(jobId);
        JobSettingsDTO newJobSettings = originalJob.getSettings();
        newJobSettings.setName("new Name");

        long createdJobId = service.createJob(newJobSettings);

        assertTrue(isJobIdValid(createdJobId));
        List<JobDTO> jobs = service.getJobsByName("new Name");
        assertTrue(jobs.size() > 0);
        service.deleteJob(createdJobId);

        assertFalse(isJobIdValid(createdJobId));
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void getJob_whenCalled_retrievesInformationAboutASingleJob() throws IOException, DatabricksRestException {
        JobDTO result = service.getJob(jobId);

        long resultantJobId = result.getJobId();

        assertEquals(resultantJobId, jobId);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void listAllJobs_whenCalled_returnsListOfAllJobs() throws IOException, DatabricksRestException {
        JobsDTO result = service.listAllJobs();

        assertTrue(result.getJobs().length >= 1);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void getJobsByName_whenCalled_returnsJobIfExists() throws IOException, DatabricksRestException {
        List<JobDTO> result = service.getJobsByName(jobName);
        if (result.size() > 1) {
            System.out.println("WARNING: There are multiple jobs existing for this name. Probably due to incomplete " +
                "tests. Please delete");
        }
        for (JobDTO jobDTO : result) {
            if (jobDTO.getJobId() == jobId) {
                assertTrue(true);
                return;
            }
        }
        assertTrue(false);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void getJobsByName_whenCalled_returnsEmptyList() throws IOException, DatabricksRestException {
        assertEquals(service.getJobsByName("doesnt Exist").size(), 0);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void getJobByName_whenExists_returnsJob() throws IOException, DatabricksRestException {
        assertEquals(service.getJobByName(jobName, true).getJobId(), jobId);
        assertEquals(service.getJobByName(jobName, false).getJobId(), jobId);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void getJobByName_whenDoesntExists_returnsNull() throws IOException,
                                                                                   DatabricksRestException {
        assertEquals(service.getJobByName("doesntExist", true), null);
        assertEquals(service.getJobByName("doesntExist", false), null);
    }

    @Test(dependsOnMethods = {"createMultiJobs"}, expectedExceptions = IllegalStateException.class)
    public void getJobByName_whenMultipleExists_throwsException() throws IOException,
                                                                   DatabricksRestException {
        service.getJobByName(multiJobName, true);
    }

    @Test(dependsOnMethods = {"createMultiJobs"})
    public void getJobByName_whenMultipleExistsAndNotFail_returnsFirst() throws IOException,
                                                                         DatabricksRestException {
        assertEquals(service.getJobByName(multiJobName, false).getJobId(), multiJobId[0]);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void runJobNow_whenCalled_startsNewRun() throws IOException, DatabricksRestException {
        Map<String, String> notebook_params = new HashMap<>();
        notebook_params.put("test_arg", "hello world!!");
        long calledRunId = service.runJobNow(jobId, notebook_params).getRunId();


        await().atMost(1, MINUTES).until(TestUtil.runLifeCycleStateHasChangedTo(RunLifeCycleStateDTO.TERMINATED,
            calledRunId, service));

        assertTrue(isRunIdValid(calledRunId));
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void listRuns_whenCalled_returnsMostRecentRuns() throws DatabricksRestException, IOException {
        RunsDTO result = service.listRuns(jobId, null, null, null);

        assertNotNull(result);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void getRun_whenCalled_returnsRunMetadata() throws IOException, DatabricksRestException {
        RunDTO result = service.getRun(runId);

        assertNotNull(result);

        Long resultantRunId = result.getRunId();

        assertEquals(resultantRunId, runId);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void reset_whenCalled_overridesSettings() throws IOException, DatabricksRestException {
        JobDTO originalJob = service.getJob(jobId);
        JobSettingsDTO newJobSettings = originalJob.getSettings();
        newJobSettings.setMaxRetries(10);

        service.reset(jobId, newJobSettings);

        JobDTO newJob = service.getJob(jobId);

        assertEquals(newJob.getSettings().getMaxRetries().intValue(), 10);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void upsertJob_whenJobExists_resetsJob() throws IOException, DatabricksRestException {
        JobDTO originalJob = service.getJob(jobId);
        JobSettingsDTO newJobSettings = originalJob.getSettings();
        newJobSettings.setMaxRetries(20);

        service.upsertJob(newJobSettings, false);

        JobDTO newJob = service.getJob(jobId);

        assertEquals(newJob.getSettings().getMaxRetries().intValue(), 20);
    }

    @Test(dependsOnMethods = {"testSetUpOnce"})
    public void upsertJob_whenJobDoesntExists_createsJob() throws IOException, DatabricksRestException {
        JobDTO originalJob = service.getJob(jobId);
        JobSettingsDTO newJobSettings = originalJob.getSettings();
        newJobSettings.setName("new-job");
        newJobSettings.setMaxRetries(5);

        service.upsertJob(newJobSettings, false);

        JobDTO newJob = service.getJobByName("new-job");
        service.deleteJob(newJob.getJobId());

        assertEquals(newJob.getSettings().getMaxRetries().intValue(), 5);
    }

    private boolean isJobIdValid(long jobId) throws IOException, DatabricksRestException {

        JobDTO[] jobs = service.listAllJobs().getJobs();
        for (JobDTO job : jobs) {
            if (job.getJobId() == jobId) {
                return true;
            }
        }

        return false;
    }

    private boolean isRunIdValid(long runId) throws IOException, DatabricksRestException {
        RunDTO[] runs = service.listRuns(jobId, null, null, null).getRuns();

        for (RunDTO run : runs) {
            if (run.getRunId() == runId) {
                return true;
            }
        }

        return false;
    }
}