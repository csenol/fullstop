/**
 * Copyright (C) 2015 Zalando SE (http://tech.zalando.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.unknown.pkg;

import org.assertj.core.api.Assertions;

import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.zalando.stups.fullstop.plugin.count.CountEventsPlugin;

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ExampleApplication.class)
@IntegrationTest
public class PluginTest {

    @Autowired
    private CountEventsPlugin plugin;

    @Test
    public void testCreation() {
        CloudTrailEvent event = Mockito.mock(CloudTrailEvent.class);

        // plugin supports all events
        Assertions.assertThat(plugin.supports(event)).isTrue();
    }

}
