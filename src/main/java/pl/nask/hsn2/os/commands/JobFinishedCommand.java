/*
 * Copyright (c) NASK, NCSC
 *
 * This file is part of HoneySpider Network 2.0.
 *
 * This is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.nask.hsn2.os.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.os.ObjectStore;
import pl.nask.hsn2.protobuff.Jobs.JobFinished;

public class JobFinishedCommand extends AbstractCommand {
	private static final Logger LOGGER = LoggerFactory.getLogger(JobFinishedCommand.class);
	private final JobFinished request;
	private final String jobFinishedIgnore;

	public JobFinishedCommand(JobFinished request, String jobFinishedIgnore){
		this.request = request;
		this.jobFinishedIgnore = jobFinishedIgnore;
	}

	@Override
	public final void processCommand() {
		switch (jobFinishedIgnore) {
			case "all":
				LOGGER.info("JobFinished (status: {}) was ignored.", request.getStatus());
				break;
			case "fail":
				if ("failed".equalsIgnoreCase(request.getStatus().name())){
					LOGGER.info("JobFinished (status: {}) was ignored.", request.getStatus());
					break;
				}
			case "none":
				ObjectStore.getInstance().removeJobData(request.getJob());
				break;
			default:
				LOGGER.info("Unknown jobFinishedIgnore attribute: {}. JobFinished processed.", jobFinishedIgnore);
				ObjectStore.getInstance().removeJobData(request.getJob());
		}

	}
}
