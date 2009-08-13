/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.jstestdriver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts a list of browsers when run.
 * @author jeremiele@google.com (Jeremie Lenfant-Engelmann)
 */
public class BrowserStartupAction implements Action, Observer {

  private static final Logger logger = LoggerFactory.getLogger(BrowserStartupAction.class);
  private final CountDownLatch latch;
  private final List<String> browserPath;
  private final String serverAddress;
  private volatile List<Process> processes = new ArrayList<Process>();
  private final ProcessFactory processFactory;

  public BrowserStartupAction(List<String> browserPath, String serverAddress,
      ProcessFactory processFactory, CountDownLatch latch) {
    this.browserPath = browserPath;
    this.serverAddress = serverAddress;
    this.processFactory = processFactory;
    this.latch = latch;
  }

  public void run() {
    try {
      String url = String.format("%s/capture", serverAddress);

      for (String browser : browserPath) {
        try {
          processes.add(processFactory.start(browser, url));
        } catch (IOException e) {
          logger.error("Could not start: {} because {}", browser, e.toString());
        }
      }
      if (!latch.await(30, TimeUnit.SECONDS)) {
        long count = latch.getCount();

        if (count < browserPath.size()) {
          logger.warn("Not all browsers were captured continuing anyway...");
        } else {
          logger.error("None of the browsers were captured after 30 seconds");
          new BrowserShutdownAction(this).run();
        }
      }
    } catch (InterruptedException e) {
      logger.error("Error in starting browsers: {}", e.toString());
    }
  }

  public List<Process> getProcesses() {
    return processes;
  }

  public void update(Observable o, Object arg) {
    latch.countDown();
  }
  
  public List<String> getBrowserPath() {
    return browserPath;
  }
  
  public String getServerAddress() {
    return serverAddress;
  }
}
