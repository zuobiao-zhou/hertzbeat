/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.usthe.alert;

import com.usthe.common.entity.alerter.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 采集数据队列
 *
 *
 */
@Component
@Slf4j
public class AlerterDataQueue {

    private final LinkedBlockingQueue<Alert> alertDataQueue;

    public AlerterDataQueue() {
        alertDataQueue = new LinkedBlockingQueue<>();
    }

    public void addAlertData(Alert alert) {
        alertDataQueue.offer(alert);
    }

    public Alert pollAlertData() throws InterruptedException {
        return alertDataQueue.poll(2, TimeUnit.SECONDS);
    }

}
