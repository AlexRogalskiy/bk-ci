/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.listener

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.dispatch.service.PipelineDispatchService
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import feign.RetryableException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ThirdPartyAgentListener @Autowired constructor(
    private val pipelineDispatchService: PipelineDispatchService,
    private val client: Client
) {
    fun listenAgentStartUpEvent(pipelineAgentStartupEvent: PipelineAgentStartupEvent) {
        try {
            if (checkRunning(pipelineAgentStartupEvent)) {
                pipelineDispatchService.startUp(pipelineAgentStartupEvent)
            }
        } catch (e: RetryableException) {
            logger.warn("[${pipelineAgentStartupEvent.buildId}]|feign fail, do retry again", e)
            pipelineDispatchService.reDispatch(pipelineAgentStartupEvent)
        } catch (ignored: Throwable) {
            logger.error("Fail to start the pipe build($pipelineAgentStartupEvent)", ignored)
        }
    }

    fun listenAgentShutdownEvent(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent) {
        try {
            pipelineDispatchService.shutdown(pipelineAgentShutdownEvent)
        } catch (ignored: Throwable) {
            logger.error("Fail to start the pipe build($pipelineAgentShutdownEvent)", ignored)
        }
    }

    private fun checkRunning(event: PipelineAgentStartupEvent): Boolean {
        // 判断流水线是否还在运行，如果已经停止则不在运行
        // 只有detail的信息是在shutdown事件发出之前就写入的，所以这里去builddetail的信息。
        // 为了兼容gitci的权限，这里把渠道号都改成GIT,以便去掉用户权限验证
        val record = client.get(ServiceBuildResource::class).getBuildDetailStatusWithoutPermission(
            event.userId,
            event.projectId,
            event.pipelineId,
            event.buildId,
            ChannelCode.BS
        )
        if (record.isNotOk() || record.data == null) {
            logger.warn("The build event($event) fail to check if pipeline is running because of ${record.message}")
            return false
        }
        val status = BuildStatus.parse(record.data)
        if (!status.isRunning()) {
            logger.error("The build event($event) is not running")
            return false
        }

        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentListener::class.java)
    }
}
