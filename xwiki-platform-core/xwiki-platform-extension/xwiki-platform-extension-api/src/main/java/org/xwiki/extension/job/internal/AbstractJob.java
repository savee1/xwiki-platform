/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.extension.job.internal;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.job.Job;
import org.xwiki.extension.job.Request;
import org.xwiki.extension.job.event.JobFinishedEvent;
import org.xwiki.extension.job.event.JobStartedEvent;
import org.xwiki.extension.job.event.status.JobStatus;
import org.xwiki.extension.job.event.status.PopLevelProgressEvent;
import org.xwiki.extension.job.event.status.PushLevelProgressEvent;
import org.xwiki.extension.job.event.status.StepProgressEvent;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

/**
 * Base class for {@link Job} implementations.
 * 
 * @param <R> the request type associated to the job
 * @version $Id$
 */
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public abstract class AbstractJob<R extends Request> implements Job
{
    /**
     * Component manager.
     */
    @Inject
    protected ComponentManager componentManager;

    /**
     * Used to send extensions installation and upgrade related events.
     */
    @Inject
    protected ObservationManager observationManager;

    /**
     * Used to isolate job related log.
     */
    @Inject
    protected LoggerManager loggerManager;

    /**
     * Used to store the results of the jobs execution.
     */
    @Inject
    protected JobStatusStorage storage;

    /**
     * The logger to log.
     */
    @Inject
    protected Logger logger;

    /**
     * The job request.
     */
    protected R request;

    /**
     * @see #getStatus()
     */
    protected DefaultJobStatus<R> status;

    @Override
    public R getRequest()
    {
        return this.request;
    }

    @Override
    public JobStatus getStatus()
    {
        return this.status;
    }

    @Override
    public void start(Request request)
    {
        this.request = castRequest(request);
        this.status = createNewStatus(this.request);

        this.observationManager.notify(new JobStartedEvent(getId(), getType(), request), this);

        this.status.startListening();

        Exception exception = null;
        try {
            start();
        } catch (Exception e) {
            this.logger.error("Exception thrown during job execution", e);
            exception = e;
        } finally {
            this.status.stopListening();

            this.status.setState(JobStatus.State.FINISHED);

            this.observationManager.notify(new JobFinishedEvent(getId(), getType(), request), this, exception);

            try {
                if (request.getId() != null) {
                    this.storage.store(this.status);
                }
            } catch (Exception e) {
                this.logger.warn("Failed to store job status [{}]", this.status, e);
            }
        }
    }

    /**
     * Should be overridden if R is not Request.
     * 
     * @param request the request
     * @return the request in the proper extended type
     */
    protected R castRequest(Request request)
    {
        return (R) request;
    }

    /**
     * @param request contains information related to the job to execute
     * @return the status of the job
     */
    protected DefaultJobStatus<R> createNewStatus(R request)
    {
        return new DefaultJobStatus<R>((R) request, getId(), this.observationManager, this.loggerManager);
    }

    /**
     * @return unique id for the job
     */
    protected String getId()
    {
        return getRequest().getId() != null ? getRequest().getId() : getClass().getName() + "_"
            + Integer.toHexString(hashCode());
    }

    /**
     * Push new progression level.
     * 
     * @param steps number of steps in this new level
     */
    protected void notifyPushLevelProgress(int steps)
    {
        this.observationManager.notify(new PushLevelProgressEvent(steps), this);
    }

    /**
     * Next step.
     */
    protected void notifyStepPropress()
    {
        this.observationManager.notify(new StepProgressEvent(), this);
    }

    /**
     * Pop progression level.
     */
    protected void notifyPopLevelProgress()
    {
        this.observationManager.notify(new PopLevelProgressEvent(), this);
    }

    /**
     * Should be implemented by {@link Job} implementations.
     * 
     * @throws Exception errors during job execution
     */
    protected abstract void start() throws Exception;
}
