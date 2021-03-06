/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.cc.test.dao;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.ObjectUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

/**
 * @author gy@fir.im
 */
public class CmdDaoTest extends TestBase {

    @Test
    public void should_save_all_fields() throws Throwable {
        // given:
        Cmd cmd = new Cmd("zone", "agent", CmdType.SHUTDOWN, "123");
        cmd.setId(UUID.randomUUID().toString());
        cmd.setStatus(CmdStatus.KILLED);
        cmd.setOutputEnvFilter(Lists.newArrayList("FLOW_VAR"));
        cmd.setLogPath("/test/log/path");
        cmd.setTimeout(10);
        cmd.setWebhook("http://webhook.com");
        cmd.getInputs().put("VAR_1", "1");
        cmd.getInputs().put("VAR_2", "2");
        cmd.setWorkingDir("/");
        cmd.setSessionId("session-id");
        cmd.setExtra("test");

        // when:
        cmdDao.save(cmd);

        // then:
        Cmd loaded = cmdDao.get(cmd.getId());
        Assert.assertEquals(cmd.getZoneName(), loaded.getZoneName());
        Assert.assertEquals(cmd.getAgentName(), loaded.getAgentName());
        Assert.assertEquals(cmd.getType(), loaded.getType());
        Assert.assertEquals(cmd.getCmd(), loaded.getCmd());
        Assert.assertEquals(cmd.getStatus(), loaded.getStatus());
        Assert.assertEquals(cmd.getOutputEnvFilter(), loaded.getOutputEnvFilter());
        Assert.assertEquals(cmd.getWebhook(), loaded.getWebhook());
        Assert.assertNotNull(loaded.getCreatedDate());
        Assert.assertNotNull(loaded.getUpdatedDate());
        Assert.assertEquals(cmd.getExtra(), loaded.getExtra());

        // when:
        Cmd copy = ObjectUtil.deepCopy(loaded);
        Thread.sleep(1000);

        loaded.setCmd("update something, otherwise mysql will not affected");
        cmdDao.update(loaded);

        // then:
        loaded = cmdDao.get(cmd.getId());
        Assert.assertTrue(copy.getUpdatedDate().isBefore(loaded.getUpdatedDate()));
    }

    @Test
    public void should_get_cmd_by_agent_path() throws Throwable {
        // given:
        final String zoneName = "zone-1";

        Cmd cmd0 = new Cmd(zoneName, "agent-1", CmdType.CREATE_SESSION, "hello");
        cmd0.setStatus(CmdStatus.KILLED);
        cmd0.setId(UUID.randomUUID().toString());
        cmdDao.save(cmd0);

        Cmd cmd1 = new Cmd(zoneName, "agent-2", CmdType.SHUTDOWN, "hello");
        cmd1.setStatus(CmdStatus.RUNNING);
        cmd1.setId(UUID.randomUUID().toString());
        cmdDao.save(cmd1);

        List<Cmd> list = cmdDao.list(Sets.newHashSet(cmd0.getId(), cmd1.getId()));
        Assert.assertTrue(list.size() == 2);

        // when: get all cmd for zone
        List<Cmd> result = cmdDao.list(null, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        // when: get all cmd for zone
        result = cmdDao.list(new AgentPath(zoneName, null), null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        // when: get all cmd for zone and agent
        result = cmdDao.list(new AgentPath(zoneName, "agent-2"), null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(cmd1, result.get(0));

        // when: get cmd for agent by type and status
        result = cmdDao.list(new AgentPath(zoneName, "agent-2"),
            Sets.newHashSet(CmdType.SHUTDOWN), Sets.newHashSet(CmdStatus.RUNNING));
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(cmd1, result.get(0));
    }

    @Test
    public void should_get_cmd_list_by_session() throws Throwable {
        // given:
        final String sessionId = UUID.randomUUID().toString();
        Cmd cmd = new Cmd("zone-1", "agent-1", CmdType.CREATE_SESSION, "hello");
        cmd.setStatus(CmdStatus.KILLED);
        cmd.setSessionId(sessionId);
        cmd.setId(UUID.randomUUID().toString());
        cmdDao.save(cmd);

        cmd = new Cmd("zone-1", "agent-1", CmdType.RUN_SHELL, "hello");
        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setSessionId(sessionId);
        cmd.setId(UUID.randomUUID().toString());
        cmdDao.save(cmd);

        // when:
        List<Cmd> list = cmdDao.list(sessionId);
        Assert.assertNotNull(list);

        // then:
        Assert.assertEquals(2, list.size());
    }
}
